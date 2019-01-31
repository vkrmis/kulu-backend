(ns kulu-backend.utils.api-test
  (:require [kulu-backend.utils.api :refer :all]
            [clojure.test :refer :all]))

(deftest dasherize-transformations
  (testing "turn underscores to dashses in a collection"
    (let [coll {:k_1 {:k_1_1 "v1" :k-1-2 "v_12"}
                :k_2 [:v21 :v-2-2 :v_2_3 {"v_4" "v-5"}]
                :k_3 (list "v3_1" "v3-2" {:v3_3 "abc"})
                "k_4" "v_4"}
          res (dasherize coll)]
      (is (= (set [:k-1 :k-2 :k-3 "k_4"]) (set (keys res))))
      (is (= {:k-1-1 "v1" :k-1-2 "v_12"} (:k-1 res)))
      (is (= (set (:k_2 coll)) (set (:k-2 res))))
      (is (= (set (list "v3_1" "v3-2" {:v3-3 "abc"})) (set (:k-3 res))))
      (is (= (coll "k_4") (res "k_4"))))))

(deftest underscorize-transformations
  (testing "turn dashes to underscores in a collection"
    (let [coll {:k-1 {:k-1-1 "v1" :k_1_2 "v-12"}
                :k-2 [:v21 :v_2_2 :v-2-3 {"v-4" "v_5"}]
                :k-3 (list "v3-1" "v3_2" {:v3-3 "abc"})
                "k-4" "v-4"}
          res (underscorize coll)]
      (is (= (set [:k_1 :k_2 :k_3 "k-4"]) (set (keys res))))
      (is (= {:k_1_1 "v1" :k_1_2 "v-12"} (:k_1 res)))
      (is (= (set (:k_2 coll)) (set (:k-2 res))))
      (is (= (set (list "v3-1" "v3_2" {:v3_3 "abc"})) (set (:k_3 res))))
      (is (= (coll "k-4") (res "k-4"))))))

(deftest idiomatize-keys-transformations
  (testing "calls given fn with dasherized inputs and returns underscorized results"
    (let [input {:meta_data {:per_page 3 :page 24}
                 :data {:storage_key "abc"}}
          expected-output {:meta-data {:per-page 3 :page 24}
                           :data {:storage-key "abc"}}
          transformed-input-holder (atom {})
          transformed-output (idiomatize-keys #(do (reset! transformed-input-holder %1) %1)
                                              input)]
      (is (= transformed-output input))
      (is (= @transformed-input-holder expected-output)))))

(deftest idiomatize-keys-for-sql-transformations
  (testing "calls given fn with underscorized inputs and returns dasherized results"
    (let [input [:invoice {:storage-key "foo/bar.pdf"}]
          expected-output [:invoice {:storage_key "foo/bar.pdf"}]
          transformed-input-holder (atom {})
          transformed-output (idiomatize-keys-for-sql #(do (reset! transformed-input-holder %1) %1)
                                                      input)]
      (is (= transformed-output input))
      (is (= @transformed-input-holder expected-output)))))

(deftest respond-with-found-or-missing-behaior
  (testing "responds with HTTP ok (200) and the item when it exists"
    (let [item {:name "Invoice #1"}
          r (respond-with-found-or-missing item "thing" "id" 123)]
      (is (= (:status r) 200))
      (is (= (:body r) item))))

  (testing "responds with HTTP not-found (404) and a structured error when the item does not exist"
    (let [r (respond-with-found-or-missing nil "thing" "id" 123)
          expected-body {:errors {"id" "no thing found with id: 123"}}]
      (is (= (:status r) 404))
      (is (= (:body r) expected-body)))))
