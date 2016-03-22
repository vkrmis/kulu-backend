;; migrations/20150115181457100-create-users.clj

(defn up []
  ["CREATE TABLE users(id UUID default uuid_generate_v4() primary key,
                 user_name VARCHAR(250),
                 email VARCHAR(255),
                 created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                 updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                 CONSTRAINT uniq_email UNIQUE (email))"])

(defn down []
  ["DROP TABLE users"])

