;; migrations/20140728121512103-create-invoices.clj

(defn up []
  ["CREATE TABLE invoices(id UUID default uuid_generate_v4() primary key,
                 name VARCHAR(250),
                 date DATE,
                 amount DECIMAL,
                 currency VARCHAR(10),
                 storage_key VARCHAR(250),
                 status VARCHAR(20),
                 created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                 updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL)"])

(defn down []
  ["DROP TABLE invoices"])
