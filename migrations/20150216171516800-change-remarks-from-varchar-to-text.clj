;; migrations/20150216171516800-change-remarks-from-varchar-to-text.clj

(defn up []
  ["ALTER TABLE invoices ALTER COLUMN remarks TYPE text"])

(defn down []
  ["ALTER TABLE invoices ALTER COLUMN remarks TYPE varchar(255)"])
