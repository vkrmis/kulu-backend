;; migrations/20150317202213218-add-user-email-and-org-name-to-tokens.clj

(defn up []
  ["ALTER TABLE tokens ADD COLUMN organization_name VARCHAR (250)"
   "ALTER TABLE tokens ADD COLUMN user_email VARCHAR (255)"])

(defn down []
  ["ALTER TABLE tokens DROP COLUMN user_email"
   "ALTER TABLE tokens DROP COLUMN organization_name"])
