(ns kulu-backend.reports.dashboard
    (:require [aws.sdk.s3 :as s3]
            [honeysql.core :as sql]
            [honeysql.helpers :as h-sql]
            [kulu-backend.config :as cfg]
            [kulu-backend.users.model :as user]
            [kulu-backend.organizations.model :as org]
            [kulu-backend.invoice-categories.model :as inv-cat]
            [kulu-backend.session-tokens.model :as token]
            [kulu-backend.db :as db :only [query insert!]]
            [kulu-backend.models.helpers.pagination :as pagination]
            [clj-http.client :as client]
            [clojure.string :as s]
            [clojure.core.strint :refer :all]))

(defonce expenses-table :invoices)
(defonce categories-table :categories)
(defonce invoice-categories-table :invoice_categories)

;; total conflict free expenses (currecnt brakdown)
;;
;; n waiting for approval cb x
;; b waiting to be reimbursed cb
;;
;; x submitted
;; y fully reviwed
;;
;; category breakdown

(defn run
  [query from to org-id]
  (db/query [query from to org-id]))

(def all-expenses-currency-breakdown-query
  (<<
   "select currency, sum(amount), count(id)
           from ~(name expenses-table)
           where (created_at, created_at) overlaps (?::DATE, ?::DATE) and
                 organization_id =  ?   and
                 conflict        = 'f'  and
                 deleted         = 'f'  and
                 currency is not null
           group by currency"))

(defn all-expenses-currency-breakdown
  [org-id from to]
  (run all-expenses-currency-breakdown-query from to org-id))

(def submitted-expenses-query
  (<<
   "select currency, sum(amount), count(id)
           from ~(name expenses-table)
           where (created_at, created_at) overlaps (?::DATE, ?::DATE) and
                 organization_id =  ?           and
                 conflict        = 'f'          and
                 deleted         = 'f'          and
                 status          = 'Submitted'  and
                 currency is not null
           group by currency"))

(defn submitted-expenses
  [org-id from to]
  (run submitted-expenses-query from to org-id))

(def conflicted-expenses-query
  (<<
   "select currency, sum(amount), count(id)
           from ~(name expenses-table)
           where (created_at, created_at) overlaps (?::DATE, ?::DATE) and
                 organization_id =  ?                  and
                 conflict        = 't'                 and
                 deleted         = 'f'                 and
                 status in ('Submitted', 'Extracted')  and
                 currency is not null
           group by currency"))

(defn conflicted-expenses
  [org-id from to]
  (run conflicted-expenses-query from to org-id))

(def reviewed-expenses-query
  (<<
   "select currency, sum(amount), count(id)
           from ~(name expenses-table)
           where (created_at, created_at) overlaps (?::DATE, ?::DATE) and
                 organization_id =  ?          and
                 conflict        = 'f'         and
                 deleted         = 'f'         and
                 status          = 'Reviewed'  and
                 currency is not null
           group by currency"))

(defn reviewed-expenses
  [org-id from to]
  (run reviewed-expenses-query from to org-id))

(def awaiting-review-expenses-currency-breakdown-query
  (<<
   "select currency, sum(amount), count(id)
           from ~(name expenses-table)
           where (created_at, created_at) overlaps (?::DATE, ?::DATE) and
                 organization_id =  ?           and
                 conflict        = 'f'          and
                 deleted         = 'f'          and
                 status          = 'Extracted'  and
                 expense_type    = 'Company'    and
                 currency is not null
           group by currency"))

(defn awaiting-review-expenses-currency-breakdown
  [org-id from to]
  (run awaiting-review-expenses-currency-breakdown-query from to org-id))

(def awaiting-approval-reimbursements-currency-breakdown-query
  (<<
   "select currency, sum(amount), count(id)
           from ~(name expenses-table)
           where (created_at, created_at) overlaps (?::DATE, ?::DATE) and
                 organization_id =  ?               and
                 conflict        = 'f'              and
                 deleted         = 'f'              and
                 status          = 'Extracted'      and
                 expense_type    = 'Reimbursement'  and
                 currency is not null
           group by currency"))

(defn awaiting-approval-reimbursements-currency-breakdown
  [org-id from to]
  (run awaiting-approval-reimbursements-currency-breakdown-query from to org-id))

(def all-expenses-category-breakdown-query
  (<<
   "select cat.name, i.currency, sum(i.amount)
           from ~(name expenses-table) as i
           left join ~(name invoice-categories-table) as ic on i.id = ic.invoice_id
           left join ~(name categories-table) as cat on ic.category_id = cat.id
           where (i.created_at, i.created_at) overlaps (?::DATE, ?::DATE) and
                 i.organization_id =  ?   and
                 i.conflict        = 'f'  and
                 i.deleted         = 'f'  and
                 i.status in ('Extracted', 'Recorded', 'Reimbursed/Deducted', 'Reviewed') and
                 i.currency is not null
           group by cat.name, i.currency"))

(defn all-expenses-category-breakdown
  [org-id from to]
  (run all-expenses-category-breakdown-query from to org-id))
