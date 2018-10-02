;; migrations/20150424181530066-remove-team-domain-from-orgs.clj

(defn up []
  ["ALTER TABLE organizations DROP COLUMN team_domain"])

(defn down []
  ["ALTER TABLE users ADD COLUMN team_domain VARCHAR (200)"])
