(ns kulu-backend.invoices.states
  (:require [schema.core :as s]))

(defonce known-states ["Submitted" "Extracted" "Reviewed" "Reimbursed/Deducted" "Recorded"])

(defn valid?
  [c]
  (contains? known-states c))


