(ns kulu-backend.session-tokens.model
  (:require [kulu-backend.db :as db]
            [honeysql.core :as sql]
            [honeysql.helpers :as h-sql]))

(defonce table-name :tokens)

(defn store
  [m]
  (->> m
       (db/insert! (keyword table-name))
       first))

(defn lookup-by-id
  [id]
  (try (-> (h-sql/select :*)
           (h-sql/from table-name)
           (h-sql/where [:= :id (java.util.UUID/fromString id)])
           sql/format
           db/query
           first)
       (catch Exception e {})))

(defn get-it
  [id]
  (let [token (lookup-by-id id)]
    (when (not (empty? token))
      token)))

;; This appears to be basically a copy of the organizations_users join table. So why not just use its id as token?
;;
;; Because this is meant to be ephemeral.
;; Tokens are deleted on logout; also makes for an easy shift to redis later on.
(defn create
  ([m]
   (:id (store {:user_id (:user_id m)
                :user_email (:user_email m)
                :organization_name (:organization_name m)}))))

(defn delete
  [id]
  (db/delete! table-name ["id = ?", id]))
