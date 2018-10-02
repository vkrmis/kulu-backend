(ns kulu-backend.currencies
  (:require [schema.core :as s]))

(def known-currencies #{"INR" "USD" "EUR" "SGD" "IDR" "GBP" "CAD"})

(defn valid?
  [c]
  (contains? known-currencies c))

(s/defschema Currency
  (apply s/enum (into [] known-currencies)))
