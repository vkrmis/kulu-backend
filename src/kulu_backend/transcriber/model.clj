(ns kulu-backend.transcriber.model
  (:require [aws.sdk.s3 :as s3]
            [honeysql.core :as sql]
            [honeysql.helpers :as h-sql]
            [kulu-backend.config :as cfg]
            [kulu-backend.db :as db :only [query insert!]]
            [kulu-backend.models.helpers.pagination :as pagination]
            [clojure.string :as s]
            [clj-http.client :as client]
            [clojure.core.strint :refer :all]))

(defonce table-name :invoices)
(defonce default-order "created_at")
(defonce default-direction "desc")

(assert (keyword? table-name) "the table-name must be a keyword")

(defn assoc-presigned-url
  "Adds the S3 presigned URL (key: attachment-url) to the given invoice map.
  The generated URL defaults to 24-hours expiry.
  Don't add a presigned url to an invoice without storage-key!"
  [{:keys [storage-key] :as expense}]
  (if (or (nil? expense) (nil? storage-key))
    expense
    ;; if we can add a presigned url, do so:
    (let [bucket (cfg/invoice-bucket)
          presigned-url (s3/generate-presigned-url (cfg/aws-creds)
                                                   bucket storage-key)
          presigned-head-url (s3/generate-presigned-url (cfg/aws-creds)
                                                        bucket storage-key {:http-method :head})
          content-type (-> (client/head presigned-head-url)
                           :headers
                           (get "Content-Type"))]
      (assoc expense :attachment-url presigned-url :attachment-content-type content-type))))

(defn lookup
  "Finds an invoice by its UUID.
  It won't find soft-deleted invoices"
  [id]
  (let [uuid ^java.util.UUID id]
    (-> (h-sql/select :*)
        (h-sql/from table-name)
        (h-sql/where [:= :id uuid]
                     [:= :deleted false])
        sql/format
        db/query
        first
        assoc-presigned-url
        (dissoc :deleted))))

(defn order-by
  [query order direction]
  (if (= order "amount" )
    (h-sql/order-by query [:currency (keyword direction)] [:amount (keyword direction)] [(keyword "invoices.id")])
    (h-sql/order-by query [(keyword (str "invoices." order)) (keyword direction)] [(keyword "invoices.id")])))

;; TODO:
;; left-join returns a flat-map which overrides similar column names
(defn all
  "Returns a limited set of all invoices"
  ([] (all {:page 1 :per-page 10 :direction :desc :order :created_at})) ;; just some defaults
  ([{:keys [page per-page order direction] :or {page 1 per-page 10 direction "desc" order "created_at"}}]
   (-> (h-sql/select :*)
       (h-sql/from table-name)
       (h-sql/left-join [:users :u] [:= :invoices.email :u.email])
       (h-sql/where [:= :deleted false])
       (order-by order direction)
       (pagination/paginate page per-page)
       sql/format
       db/query
       (->> (map #(dissoc % :deleted))))))

(defn update
  "Once the transcriber updates the record, it changes to an Extracted state"
  [id attrs]
  (db/update! (keyword table-name)
              (assoc attrs :status "Extracted")
              ["id = ?" id])
  id)


(defn- generate-order-by-query
  [order direction]
  (if (= order "amount")
    (str "currency " direction ", " "amount " direction ", " "id")
    (str order " " direction ", " "id")))

(defn- find-first-non-empty
  [params]
  (first (remove clojure.string/blank? params)))

(defn- generate-next-and-prev-invoice-query
  [params]
  (let [order (find-first-non-empty [(:order params) default-order])
        direction (find-first-non-empty [(:direction params) default-direction])]
    (<< "select * from (select i.id AS id,
                              LEAD (i.id) OVER (ORDER BY ~(generate-order-by-query order direction)) AS next_item_id,
                              LAG (i.id) OVER (ORDER BY ~(generate-order-by-query order direction)) AS prev_item_id
                              from ~(name table-name) AS i
                              where deleted = false)
      as subquery where id = ?")))

(defn next-and-prev-invoices
  [id params]
  (let [uuid ^java.util.UUID id]
    (first (db/query [(generate-next-and-prev-invoice-query params) uuid]))))

(defn size
  [m]
  (-> (h-sql/select :%count.id)
      (h-sql/from table-name)
      (h-sql/where [:= :deleted false])
      sql/format
      db/query
      first
      :count))
