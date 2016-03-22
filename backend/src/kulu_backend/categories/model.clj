(ns kulu-backend.categories.model
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h-sql]
            [kulu-backend.config :as cfg]
            [kulu-backend.db :as db :only [query insert!]]))

(defonce table-name :categories)

(defn add
  [{:keys [organization-id name] :as m}]
  {:pre [(contains? m :organization-id) (contains? m :name)]}
  (db/insert! (keyword table-name) {:organization_id organization-id :name name}))

(defn lookup
  [id]
  (let [uuid ^java.util.UUID id]
    (-> (h-sql/select :*)
        (h-sql/from table-name)
        (h-sql/where [:= :id uuid])
        sql/format
        db/query
        first)))

(defn all
  [organization_id]
  (let [uuid ^java.util.UUID organization_id]
    (-> (h-sql/select :*)
        (h-sql/from table-name)
        (h-sql/where [:= :organization_id uuid])
        sql/format
        db/query)))

(defn update
  [{:keys [id organization-id name] :as m}]
  {:pre [(contains? m :id) (contains? m :organization-id) (contains? m :name)]}
  (db/update! (keyword table-name)
              {:organization_id organization-id :name name}
              ["id = ?" id]))

(defn delete
  "This cascadingly removes the invoice_categories rows that belong to this category as well.
  They should not exist without their parent category.
  It's a hard constraint in the system at the moment, but can be changed later on.'"
  [id]
  (assert (lookup id) (format "the record %s you wanted to delete does not exist" id))
  (db/delete! (keyword table-name) ["id = ?" id])
  id)
