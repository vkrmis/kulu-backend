(ns kulu-backend.users.model
  (:require [kulu-backend.db :as db :only [query insert!]]
            [honeysql.core :as sql]
            [cemerick.friend.credentials :as creds]
            [honeysql.helpers :as h-sql]))

(defonce table-name :users)

(defn store
  [m]
  (->> m
       (db/insert! (keyword table-name))
       first))

(defn lookup-by-email
  "Finds an invoice by its email."
  [email]
  (-> (h-sql/select :*)
      (h-sql/from table-name)
      (h-sql/where [:= :email email])
      sql/format
      db/query
      first))

(defn update
  [id attrs]
  (db/update! (keyword table-name)
              attrs
              ["id = ?" id])
  id)

(defn lookup-by-id
  "Finds an invoice by its id."
  [id]
  (-> (h-sql/select :*)
      (h-sql/from table-name)
      (h-sql/where [:= :id id])
      sql/format
      db/query
      first))

(defn create
  "Creates an user from the given map"
  ([]
   (create {}))
  ([m]
   (let [user (lookup-by-email (m :email))]
     (if (empty? user)
       (let [new-user (assoc m :password (creds/hash-bcrypt (:password m)))]
         (store new-user))
       user))))

(defn update-password
  [{:keys [id password] :as m}]
  {:pre [(not (empty? password))]}
  (update id {:password (creds/hash-bcrypt password)}))
