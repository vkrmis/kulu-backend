;; migrations/20141111183928938-add-remarks-and-expense-type-to-invoices.clj

(defn up []
  ["ALTER TABLE invoices ADD COLUMN remarks VARCHAR(250)"
   "ALTER TABLE invoices ADD COLUMN expense_type VARCHAR(20)"])

(defn down []
  ["ALTER TABLE invoices DROP COLUMN expense_type"
   "ALTER TABLE invoices DROP COLUMN remarks"])
