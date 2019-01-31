(ns kulu-backend.invoices.search
  (:require [kulu-backend.config :as cfg]
            [kulu-backend.invoices.model :as inv]
            [kulu-backend.search.common :as search]
            [kulu-backend.search.schema :refer [invoice-mapping-type]]))

(defn- filter-attrs
  [i]
  (select-keys i [:organization-id :name :status :expense-type :date
                  :amount :currency :status :conflict :created-at :user-name :email :remarks :category-name]))

(defn create-or-update
  "transforms an invoice into a ES document format and upserts it."
  [{id :id :as invoice}]
  (->> invoice
       filter-attrs
       (search/put invoice-mapping-type id)))

(defn migrate-indexes
  "indexes all processed invoices"
  []
  (doseq [i (inv/all-for-search)]
    (create-or-update i)))

(defn lookup [id]
  (search/lookup invoice-mapping-type id))

(defn search
  ([q filters sort size-params]
   (:hits (search/search q filters sort size-params))))
