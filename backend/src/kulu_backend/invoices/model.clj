(ns kulu-backend.invoices.model
  (:require [aws.sdk.s3 :as s3]
            [clj-http.client :as client]
            [clojure.core.strint :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [honeysql.core :as sql]
            [honeysql.helpers :as h-sql]
            [kulu-backend.config :as cfg]
            [kulu-backend.db :as db :only [query insert!]]
            [kulu-backend.invoice-categories.model :as inv-cat]
            [kulu-backend.models.helpers.pagination :as pagination]
            [kulu-backend.organizations.model :as org]
            [kulu-backend.session-tokens.model :as token]
            [kulu-backend.users.model :as user]
            [schema.core :as schema]))

(defonce table-name :invoices)
(defonce default-order "created_at")
(defonce default-direction "desc")

(assert (keyword? table-name) "the table-name must be a keyword")

(defonce types  {:company "Company"
                 :reimbursement "Reimbursement"})
(defonce states {:submitted "Submitted"
                 :extracted "Extracted"
                 :reviewed "Reviewed"
                 :reimbursed "Reimbursed/Deducted"
                 :recorded "Recorded"})
(schema/defschema ExpenseState
  (apply schema/enum (into [] (vals states))))
(schema/defschema ExpenseType
  (apply schema/enum (into [] (vals types))))

(defn assoc-presigned-url
  "Adds the S3 presigned URL (key: attachment-url) and the attachment-content-type to the given invoice map.
  The generated URL defaults to 24-hours expiry.
  Don't add a presigned url to an invoice without storage-key!"
  [{:keys [storage-key] :as expense}]
  (if (or (nil? expense) (nil? storage-key))
    expense
    ;; if we can add a presigned url, do so:
    (let [bucket (cfg/invoice-bucket)
          presigned-url (s3/generate-presigned-url (cfg/aws-creds)
                                                   bucket storage-key {:http-method :get})
          presigned-head-url (s3/generate-presigned-url (cfg/aws-creds)
                                                        bucket storage-key {:http-method :head})
          content-type (try
                         (-> (client/head presigned-head-url)
                            :headers
                            (get "Content-Type"))
                         (catch Exception e "text/html"))]
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

(defn lookup-with-category
  [id]
  (let [uuid ^java.util.UUID id]
    (-> (h-sql/select :* [:c.id "category-id"])
        (h-sql/from table-name)
        (h-sql/left-join [:users :u] [:= :invoices.email :u.email]
                         [:invoice_categories :ic] [:= :invoices.id :ic.invoice_id]
                         [:categories :c] [:= :c.id :ic.category_id])
        (h-sql/where [:= :invoices.id uuid]
                     [:= :invoices.deleted false])
        sql/format
        db/query
        first
        assoc-presigned-url
        (dissoc :deleted))))

(defn order-by
  [query order direction]
  (cond
    (= order "amount") (h-sql/order-by query [:currency (keyword direction)] [:amount (keyword direction)] [(keyword "invoices.id")])
    (= order "user_name") (h-sql/order-by query [:user-name (keyword direction)] [(keyword "invoices.id")])
    :else (h-sql/order-by query [(keyword (str "invoices." order)) (keyword direction)] [(keyword "invoices.id")])))

;; TODO:
;; left-join returns a flat-map which overrides similar column names
(defn all
  "Returns a limited set of all invoices"
  ([] (all {:page 1 :per-page 10 :direction :desc :order :created_at})) ;; just some defaults
  ([{:keys [organization-name page per-page order direction] :or {page 1 per-page 10 direction "desc" order "created_at"}}]
   (when organization-name
     (let [org-id (:id (org/lookup-by-name organization-name))]
       (-> (h-sql/select :* [:c.name "category-name"])
           (h-sql/from table-name)
           (h-sql/left-join [:users :u] [:= :invoices.email :u.email]
                            [:invoice_categories :ic] [:= :invoices.id :ic.invoice_id]
                            [:categories :c] [:= :c.id :ic.category_id])
           (h-sql/where [:and [:= :invoices.organization_id org-id] [:= :invoices.deleted false]])
           (order-by order direction)
           (pagination/paginate page per-page)
           sql/format
           db/query
           (->> (map #(dissoc % :deleted))))))))

(defn all-for-search
  []
  (-> (h-sql/select :* [:c.name "category-name"] [:u.user-name "user-name"])
      (h-sql/from table-name)
      (h-sql/left-join [:users :u] [:= :invoices.email :u.email]
                       [:invoice_categories :ic] [:= :invoices.id :ic.invoice_id]
                       [:categories :c] [:= :c.id :ic.category_id])
      (h-sql/where [:= :invoices.deleted false])
      sql/format
      db/query))

(defn store
  [m]
  (->> m
       (db/insert! (keyword table-name))
       first))

(defn create
  "Creates an Invoice record
  When given a map without an :id <uuid>, a new :id is generated."
  ([]
   (create {}))
  ([m]
   (let [org-name (:organization-name m)
         user-info (select-keys m [:email :user-name])
         user (if (empty? (user-info :email))
                (-> (str (:user-token m))
                    (token/lookup-by-id)
                    (:user-id)
                    (user/lookup-by-id))
                (user/create user-info))
         invoice-params (-> m
                            (dissoc :user-name :user-token :organization-name)
                            (assoc  :email (:email user))
                            (assoc  :organization-id (:id (org/lookup-by-name org-name))))
         invoice (store (merge
                         {:id (or (m :id) (java.util.UUID/randomUUID))}
                         {:status (:submitted states)}
                         invoice-params))]
     invoice)))

(defn update
  [id attrs]
  (db/update! (keyword table-name)
              (dissoc attrs :category-id)
              ["id = ?" id])
  id)

(defn update-with-category
  [id attrs]
  (db/exec-nested-transactions (flatten [#(update id attrs)
                                         (if (:category-id attrs)
                                           [#(inv-cat/upsert % (:category-id attrs))]
                                           [#(inv-cat/lookup-by-invoice-id %)
                                            [#(when (:id %) (inv-cat/delete (:id %)))]])]))
  id)

(defn delete!
  [id]
  (assert (lookup id) (format "the record %s you wanted to delete does not exist" id))
  (db/delete! table-name ["id = ?" id])
  id)

(defn soft-delete
  "Marking a record as removed by setting its value 'deleted'
  value to true. Returns true if removed. False if the uuid did not
  exist. Throws an AssertException if the item was not deleted correctly."
  [id]
  (let [exists-before (lookup id)
        _             (db/update! table-name {:deleted true} ["id = ?" id])
        exists-after  (lookup id)]
    (assert (nil? exists-after) (format "the item %s was not removed correctly!" id))
    (not (nil? exists-before))))

(defn order-by
  [query order direction]
  (cond
    (= order "amount") (h-sql/order-by query [:currency (keyword direction)] [:amount (keyword direction)] [(keyword "invoices.id")])
    (= order "user_name") (h-sql/order-by query [:user-name (keyword direction)] [(keyword "invoices.id")])
    :else (h-sql/order-by query [(keyword (str "invoices." order)) (keyword direction)] [(keyword "invoices.id")])))


(defn- generate-order-by-query
  [order direction]
  (cond
    (= order "amount") (str "currency " direction ", " "amount " direction ", " "i.id")
    (= order "user_name") (str "user_name " direction ", " "i.id")
    :else (str "i." order " " direction ", " "i.id")))

(defn- find-first-non-empty
  [params]
  (first (remove clojure.string/blank? params)))

(defn- generate-next-and-prev-invoice-query
  [params]
  (let [org-id (:id (org/lookup-by-name (:organization-name params)))
        order (find-first-non-empty [(:order params) default-order])
        direction(find-first-non-empty [(:direction params) default-direction])]
    (<< "select * from (select i.id as id,
                              LEAD (i.id) OVER (ORDER BY ~(generate-order-by-query order direction)) AS next_item_id,
                              LAG (i.id) OVER (ORDER BY ~(generate-order-by-query order direction)) AS prev_item_id
                              from ~(name table-name) AS i
                              left join users u on i.email = u.email
                              where deleted = false and organization_id = '~(str org-id)')
      as subquery where id = ?")))

(defn next-and-prev-invoices
  [id params]
  (let [uuid ^java.util.UUID id]
    (first (db/query [(generate-next-and-prev-invoice-query params) uuid]))))

(defn invoice-web-link
  [id organization-name]
  (str "https://" organization-name "." (cfg/web-client-name) "/expenses/" id))

(defn size
  [{:keys [organization-name] :as m}]
  (let [org-id (:id (org/lookup-by-name organization-name))]
    (-> (h-sql/select :%count.id)
        (h-sql/from table-name)
        (h-sql/where [:and [:= :organization_id org-id] [:= :deleted false]])
        sql/format
        db/query
        first
        :count)))
