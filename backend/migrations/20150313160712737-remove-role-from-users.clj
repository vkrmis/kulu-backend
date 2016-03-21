;; migrations/20150313160712737-remove-role-from-users.clj

(defn up []
  ["ALTER TABLE users DROP COLUMN role"])

(defn down []
  ["ALTER TABLE users ADD COLUMN role VARCHAR (50)"])
