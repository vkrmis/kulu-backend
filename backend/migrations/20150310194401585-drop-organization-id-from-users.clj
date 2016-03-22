;; migrations/20150310194401585-drop-organization-id-from-users.clj

(defn up []
  ["ALTER TABLE users DROP COLUMN organization_id"])

(defn down []
  ["ALTER TABLE users ADD COLUMN organization_id UUID"
   "ALTER TABLE users ADD FOREIGN KEY (organization_id) REFERENCES organizations (id)"])
