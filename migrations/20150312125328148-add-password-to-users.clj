;; migrations/20150312125328148-add-password-to-users.clj

(defn up []
  ["ALTER TABLE users ADD COLUMN password VARCHAR (255)"])

(defn down []
  ["ALTER TABLE users DROP COLUMN password"])
