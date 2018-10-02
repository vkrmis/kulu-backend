(ns kulu-backend.seeds.invoices
  (:require [kulu-backend.invoices.model :as inv]))

(defn seed-invoices
  []
  (doseq [d (range 1 11)]
    (->> d
         (format "%02d")
         (str "2014-08-")
         (java.sql.Date/valueOf)
         (hash-map :name (str "Invoice #" d)
                   :amount (first (shuffle [100.23 201.22 199 20000]))
                   :currency (first (shuffle ["INR" "USD" "EUR"]))
                   :storage-key "foo/bar.pdf"
                   :date)
         (inv/create))))

#_ (seed-invoices)
