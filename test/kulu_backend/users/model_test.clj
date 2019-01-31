(ns kulu-backend.users.model-test
  (:require [conjure.core :refer :all]
            [kulu-backend.db :as db]
            [kulu-backend.users.model :as users]
            [kulu-backend.organizations.model :as org]
            [kulu-backend.test-helper :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :each isolate-db)
(use-fixtures :once (join-fixtures [set-test-env
                                    migrate-test-db
                                    silence-logging]))

(deftest create-a-member-user
  (testing "creates a user and a join table record"
    (let [org (org/create {:user-email "esau@sand.org" :user-name "esau" :name "nilenso"}) ;; creating an admin user
          oru  (org/member-user-create {:email "jacob@sand.org"
                                        :organization_name "nilenso"
                                        :password "foobar"})
          user (count (users/lookup-by-id (:user_id oru)))
          org-count (count (org/all))]
      (is (= 1 org-count)))))
