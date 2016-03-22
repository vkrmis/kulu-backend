(ns kulu-backend.transcriber.api
  (:require [clj-time.coerce :as time-c]
            [clojure.tools.logging :as log]
            [kulu-backend.currencies :refer [Currency valid?] :as curr]
            [kulu-backend.transcriber.model :as ext]
            [kulu-backend.config :as cfg]
            [kulu-backend.models.helpers.pagination :as pagn]
            [ring.swagger.schema :as ring-s :only [describe]]
            [schema.core :as s :refer [defschema optional-key maybe pred enum Str Inst Num Uuid]]))

(defschema LoggedInUser
  {:token Uuid})

(defschema LoginBodyParams
  {:user_email Str
   :password   Str})

(s/defschema InvoiceID
  (ring-s/field Uuid
                {:description "UUID formatted string"}))

(defschema Invoice
  "schema for Invoices at the 'web side' of things"
  {(optional-key :organization_id)             (maybe Uuid)
   (optional-key :id)                          (maybe Uuid)
   (optional-key :name)                        (maybe Str)
   (optional-key :date)                        (maybe Inst)
   (optional-key :amount)                      (maybe Num)
   (optional-key :currency)                    (maybe Str)
   (optional-key :status)                      (maybe Str)
   (optional-key :remarks)                     (maybe Str)
   (optional-key :expense_type)                (maybe Str)
   (optional-key :attachment_url)              (maybe Str)
   (optional-key :attachment_content_type)     (maybe Str)
   (optional-key :email)                       (maybe Str)
   (optional-key :role)                        (maybe Str)
   (optional-key :user_name)                   (maybe Str)
   (optional-key :conflict)                    (maybe Boolean)})

(s/defschema PaginatedInvoiceList
  "paginated results as well as meta data for the results"
  {:meta                          pagn/PaginationResults
   :items                         [Invoice]})

(s/defschema GetInvoicesQueryParams
  (merge {:user_email s/Str} pagn/PaginationParams))

(s/defschema ChangeInvoiceBodyParams {
   (optional-key :name)           Str
   (optional-key :date)           Inst
   (optional-key :amount)         Num
   (optional-key :currency)       (pred curr/valid? 'valid?)
   (optional-key :remarks)        Str
   (optional-key :expense_type)   Str
   (optional-key :email)          Str
   (optional-key :user_name)      Str
   (optional-key :status)         Str
   (optional-key :conflict)       Str})

;; index page
(defn- rm-private-fields
  "removes fields such as timestamps from the given invoice (map)"
  [m]
  (dissoc m :created-at :updated-at :storage-key :deleted :email-2 :id-2 :created-at-2 :updated-at-2 :password))

(defn index
  [m]
  {:meta (pagn/info (merge (select-keys m [:page :per-page :order :direction])
                           {:total-count (ext/size m)}))
   :items (map rm-private-fields (ext/all m))})

;; show page
(defn lookup
  [id]
  (-> (ext/lookup id)
      rm-private-fields))

(defn conflict?
  [conflict]
  (= conflict "true"))

;; update page
(defn update
  [[id attrs]]
  (when-let [original-i (ext/lookup id)]
    (-> attrs
        (update-in [:conflict] conflict?)
        (update-in [:date] time-c/to-sql-date)
        (->> (ext/update id)))
    (let [new-i (lookup id)]
      (log/info "Updated Invoice %s from %s to %s"
                id original-i new-i)
      new-i)))

;; next/prev page
(defn next-and-prev-invoices
  [[id params]]
  (ext/next-and-prev-invoices id params))
