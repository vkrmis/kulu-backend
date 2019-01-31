;; migrations/20150115181527528-add-user-email-to-invoices.clj

(defn up []
  ["ALTER TABLE invoices ADD COLUMN email VARCHAR(255)"
   "ALTER TABLE invoices ADD FOREIGN KEY (email) REFERENCES users (email)" ])

(defn down []
  ["ALTER TABLE invoices DROP COLUMN email"])
