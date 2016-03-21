;; migrations/20150526110702605-invoice-categories.clj

(defn up []
  ["CREATE TABLE invoice_categories (id UUID default uuid_generate_v4() primary key,
                            category_id UUID,
                            invoice_id UUID,
                            priority integer,
                            created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL)"
   "ALTER TABLE invoice_categories ADD FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE"
   "ALTER TABLE invoice_categories ADD FOREIGN KEY (invoice_id)  REFERENCES invoices (id)"
   "ALTER TABLE invoice_categories ADD CONSTRAINT uniq_cat_id_v_invoice_id UNIQUE (category_id, invoice_id)"])

(defn down []
  ["ALTER TABLE invoice_categories DROP CONSTRAINT uniq_cat_id_v_invoice_id"
   "DROP TABLE invoice_categories"])
