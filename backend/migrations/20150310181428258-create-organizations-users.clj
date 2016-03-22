;; migrations/20150310181428258-create-organizations-users.clj

(defn up []
  ["CREATE TABLE organizations_users (id UUID default uuid_generate_v4() primary key,
                            user_id UUID,
                            organization_id UUID,
                            created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL)"])

(defn down []
  ["DROP TABLE organizations_users"])
