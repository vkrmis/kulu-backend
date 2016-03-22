;; migrations/20150302141850789-create-tokens-table.clj

(defn up []
  ["CREATE TABLE tokens(id UUID default uuid_generate_v4() primary key,
                 user_id UUID,
                 created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                 updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL)"
   "ALTER TABLE tokens ADD FOREIGN KEY (user_id) REFERENCES users (id)"])

(defn down []
  ["DROP TABLE tokens"])
