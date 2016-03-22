;; migrations/20150317160206451-add-organization-id-to-invoices.clj

(defn up []
  ["ALTER TABLE invoices ADD COLUMN organization_id UUID"
   "ALTER TABLE invoices ADD FOREIGN KEY (organization_id) REFERENCES organizations (id)"])

(defn down []
  ["ALTER TABLE invoices DROP COLUMN organization_id"])
