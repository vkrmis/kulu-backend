(ns kulu-backend.models.helpers.pagination-test
  (:require [kulu-backend.models.helpers.pagination :refer :all]
            [clojure.test :refer :all]))

(let [q {:select :foo :from :bar :order-by `(:date)}]
  (deftest pagination
    ;;; TODO: Use https://github.com/clojure/test.check to generate these tests
    (testing "adds correct limit and offset entries into the given map"
      (let [{:keys [limit offset]} (paginate q 1 25)]
        (is (= 25 limit))
        (is (= 0 offset)))
      (let [{:keys [limit offset]} (paginate q 2 25)]
        (is (= 25 limit))
        (is (= 25 offset)))
      (let [{:keys [limit offset]} (paginate q 3 25)]
        (is (= 25 limit))
        (is (= 50 offset))))

    (testing "raises AssertionError if order is unspecified"
      (let [q-without-order-clause (dissoc q :order-by)]
        (is (thrown? AssertionError
                     (paginate q-without-order-clause 1 10)))))

    (testing "uses default page/per-page values if they are nil"
      (let [{:keys [limit offset]} (paginate q nil nil)]
        (is (= 25 limit))
        (is (= 0 offset))))))

(deftest pagination-info
  (testing "appends the total number of pages to input map"
    (let [in-m {:per-page 10 :total-count 103 :page 4 :direction "desc", :order "created_at"}
          expected-out-m (merge in-m {:total-pages 11})
          out-m (info in-m)]
      (is (= expected-out-m out-m)))))
