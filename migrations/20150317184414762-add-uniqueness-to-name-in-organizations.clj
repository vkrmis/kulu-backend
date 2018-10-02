;; migrations/20150317184414762-add-uniqueness-to-name-in-organizations.clj

(defn up []
  ["ALTER TABLE organizations ADD CONSTRAINT uniq_name UNIQUE (name)"
   "ALTER TABLE organizations ADD CONSTRAINT uniq_team_domain UNIQUE (team_domain)"])

(defn down []
  ["ALTER TABLE organizations DROP CONSTRAINT uniq_names"
   "ALTER TABLE organizations DROP CONSTRAINT uniq_team_domain"])
