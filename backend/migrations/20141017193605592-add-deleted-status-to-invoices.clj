;; migrations/20141017193605592-add-deleted-status-to-invoices.clj

(defn up []
  ["ALTER TABLE invoices ADD COLUMN deleted boolean"
   "UPDATE invoices SET deleted = false WHERE true" ;;this sets all rows invoices.deleted = false for ALL rows (WHERE true gives that).
   "ALTER TABLE invoices ALTER COLUMN deleted SET DEFAULT false"
   "ALTER TABLE invoices ALTER COLUMN deleted SET NOT NULL"])

(defn down []
  ["ALTER TABLE invoices DROP COLUMN deleted"])
