(ns kulu-backend.search.common-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.elastisch.rest.index :as esi]
            [kulu-backend.config :as cfg]
            [kulu-backend.search.common :refer :all]
            [kulu-backend.test-helper :refer :all]))

(use-fixtures :once set-test-env)
(use-fixtures :each reset-es-indexes)

(deftest test-create-indexes
  (testing "creates an index"
    (in-es (esi/delete))
    (is ((comp not nil?) (create-indexes)))
    (is (index-exists?)))

  (testing "does not recreate an index if it exists"
    (create-indexes)
    (is (nil? (create-indexes)))
    (is (index-exists?))))
