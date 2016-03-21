(ns kulu-backend.organizations.model
  (:require [kulu-backend.db :as db]
            [honeysql.core :as sql]
            [kulu-backend.tokens.model :as tokens]
            [kulu-backend.organizations-users.model :as org-user]
            [kulu-backend.users.model :as user]
            [honeysql.helpers :as h-sql]))

(defonce table-name :organizations)
(defonce admin-role "admin")

(defn store
  [m]
  (->> m
       (db/insert! (keyword table-name))
       first))

(defn all
  []
  (-> (h-sql/select :*)
      (h-sql/from table-name)
      sql/format
      db/query))

(defn lookup-by-id
  "Finds an organization by its id."
  [id]
  (let [uuid ^java.util.UUID id]
    (-> (h-sql/select :*)
        (h-sql/from table-name)
        (h-sql/where [:= :id uuid])
        sql/format
        db/query
        first)))

(defn lookup-by-team-domain
  [td]
  (-> (h-sql/select :*)
      (h-sql/from table-name)
      (h-sql/where [:= :team-domain td])
      sql/format
      db/query
      first))

(defn lookup-by-email-and-org
  [user-email org-name]
  (lookup-by-id (:organization-id (org-user/lookup-by-email-and-org user-email org-name))))

(defn lookup-by-name
  [name]
  (-> (h-sql/select :*)
      (h-sql/from table-name)
      (h-sql/where [:= :name name])
      sql/format
      db/query
      first))

(defn member-user-create
  [{:keys [user_email user_name password organization_name] :as m}]
  (let [org (lookup-by-name organization_name)]
    (db/exec-nested-transactions [#(user/create {:email user_email :user_name user_name :password password})
                                  #(org-user/create {:organization_id (:id org)
                                                     :user_id (:id %)
                                                     :user_email (:email %)
                                                     :role "member"
                                                     :organization_name organization_name})])))

(defn admin-user-create
  [{:keys [user-email user-name password] :as m} org-args]
  (let [user (user/create {:email user-email :user-name user-name :password password})]
    (merge {:organization-id (:id org-args)} user)))

;; TODO: this should return an organization record instead and not the join table
(defn create
  "Create the organization first and then attach the user with it in a nested transaction"
  ([{:keys [user-email user-name organization-name] :as m}]
   (db/exec-nested-transactions [#(store {:name organization-name})
                                 #(admin-user-create m %)
                                 #(org-user/create {:organization_id (:organization-id %)
                                                    :user_id (:id %)
                                                    :user_email user-email
                                                    :role "admin"
                                                    :organization_name organization-name})])))

(defn accept-invite [{:keys [organization_name user_email] :as info} token]
  (when (tokens/verify-token :invite_token [user_email organization_name] token)
    (member-user-create info)
    (tokens/expire-token :invite_token info)))
