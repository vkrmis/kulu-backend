;; migrations/20150313160250630-add-role-to-orgs-users.clj

(defn up []
  ["ALTER TABLE organizations_users ADD COLUMN role VARCHAR (50)"])

(defn down []
  ["ALTER TABLE organizations_users DROP COLUMN role"])
