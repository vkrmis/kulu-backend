(ns kulu-backend.reports.api
  (:require [schema.core :as s :refer [defschema optional-key maybe pred enum Str Inst Num Uuid]]
            [kulu-backend.reports.dashboard :as dashboard-reports]))

(defschema ReportParams
  {:organization_name Str
   :from Inst
   :to Inst})

(defn all
  [org-id from to]
  (assoc {}
         :currency-breakdown (dashboard-reports/all-expenses-currency-breakdown org-id from to)
         :submitted (dashboard-reports/submitted-expenses org-id from to)
         :reviewed (dashboard-reports/reviewed-expenses org-id from to)
         :conflicted (dashboard-reports/conflicted-expenses org-id from to)
         :awaiting-review (dashboard-reports/awaiting-review-expenses-currency-breakdown org-id from to)
         :awaiting-approval (dashboard-reports/awaiting-approval-reimbursements-currency-breakdown org-id from to)
         :category-breakdown (dashboard-reports/all-expenses-category-breakdown org-id from to)))
