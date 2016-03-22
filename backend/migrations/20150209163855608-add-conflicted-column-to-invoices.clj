;; migrations/20150209163855608-add-conflicted-column-to-invoices.clj

(defn up []
  ["ALTER TABLE invoices ADD COLUMN conflict boolean"
   "UPDATE invoices SET conflict = false WHERE true"
   "ALTER TABLE invoices ALTER COLUMN conflict SET DEFAULT false"
   "ALTER TABLE invoices ALTER COLUMN conflict SET NOT NULL"])

(defn down []
  ["ALTER TABLE invoices DROP COLUMN conflict"])

