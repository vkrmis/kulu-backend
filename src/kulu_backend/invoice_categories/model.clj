(ns kulu-backend.invoice-categories.model
  (:require [honeysql.core :as sql]
            [honeysql.helpers :as h-sql]
            [kulu-backend.config :as cfg]
            [kulu-backend.db :as db :only [query insert!]]))

(defonce table-name :invoice_categories)

(defn add
  [{:keys [invoice_id category_id] :as m}]
  {:pre [(contains? m :category_id) (contains? m :invoice_id)]}
  (db/insert! (keyword table-name) {:invoice_id invoice_id :category_id category_id}))

(defn lookup
  [id]
  (let [uuid ^java.util.UUID id]
    (-> (h-sql/select :*)
        (h-sql/from table-name)
        (h-sql/where [:= :id uuid])
        sql/format
        db/query
        first)))

(defn lookup-by-invoice-id
  [invoice-id]
  (let [uuid ^java.util.UUID invoice-id]
    (-> (h-sql/select :*)
        (h-sql/from table-name)
        (h-sql/where [:= :invoice_id uuid])
        sql/format
        db/query
        first)))

(defn upsert
  [invoice_id category_id]
  (if (not (empty? (lookup-by-invoice-id invoice_id)))
    (db/update! (keyword table-name) {:category_id category_id} ["invoice_id = ?" invoice_id])
    (db/insert! (keyword table-name) {:invoice_id invoice_id :category_id category_id})))

(defn all
  [{:keys [invoice_id] :as m}]
  (let [uuid ^java.util.UUID invoice_id]
    (-> (h-sql/select :*)
        (h-sql/from :categories)
        (h-sql/left-join [table-name :ic] [:= :categories.id :ic.category_id])
        (h-sql/where [:= :ic.invoice_id uuid])
        sql/format
        db/query)))

(defn delete
  [id]
  (assert (lookup id) (format "the record %s you wanted to delete does not exist" id))
  (db/delete! (keyword table-name) ["id = ?" id])
  id)
