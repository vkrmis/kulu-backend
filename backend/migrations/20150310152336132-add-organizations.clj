;; migrations/20150310152336132-add-organizations.clj

(defn up []
  ["CREATE TABLE organizations (id UUID default uuid_generate_v4() primary key,
                             name varchar(250),
                             team_domain varchar(200),
                             created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                             updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL)"])

(defn down []
  ["DROP TABLE organizations"])
