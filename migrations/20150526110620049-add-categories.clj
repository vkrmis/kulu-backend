;; migrations/20150526110620049-add-categories.clj

(defn up []
  ["CREATE TABLE categories(id UUID default uuid_generate_v4() primary key,
                 name VARCHAR(250),
                 organization_id UUID,
                 created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                 updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL)"
   "CREATE INDEX category_name ON categories (name)"
   "ALTER TABLE categories ADD FOREIGN KEY (organization_id) REFERENCES organizations (id)"
   "ALTER TABLE categories ADD CONSTRAINT uniq_name_v_org_id UNIQUE (name, organization_id)"])

(defn down []
  ["ALTER TABLE categories DROP CONSTRAINT uniq_name_v_org_id"
   "DROP INDEX category_name"
   "DROP TABLE categories"])
