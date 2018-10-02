;; migrations/20150317210736610-add-user-email-and-org-name-to-organizations-users.clj

(defn up []
  ["ALTER TABLE organizations_users ADD COLUMN organization_name VARCHAR (250)"
   "ALTER TABLE organizations_users ADD COLUMN user_email VARCHAR (255)"])

(defn down []
  ["ALTER TABLE organizations_users DROP COLUMN user_email"
   "ALTER TABLE organizations_users DROP COLUMN organization_name"])
