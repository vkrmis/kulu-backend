;; migrations/20150311131755341-add-role-to-users.clj

(defn up []
  ["ALTER TABLE users ADD COLUMN role VARCHAR (50)"])

(defn down []
  ["ALTER TABLE users DROP COLUMN role"])
