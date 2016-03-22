;; migrations/20150310152753658-add-organization-id-to-users.clj

(defn up []
  ["ALTER TABLE users ADD COLUMN organization_id UUID"
   "ALTER TABLE users ADD FOREIGN KEY (organization_id) REFERENCES organizations (id)"])

(defn down []
  ["ALTER TABLE users DROP COLUMN organization_id"])
