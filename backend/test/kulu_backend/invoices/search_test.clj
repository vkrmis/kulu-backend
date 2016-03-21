(ns kulu-backend.invoices.search-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.elastisch.rest.bulk :as es-bulk]
            [kulu-backend.invoices.model :as inv-model]
            [kulu-backend.invoices.search :refer :all]
            [kulu-backend.test-helper :refer :all]
            [kulu-backend.search.common :as search]
            [kulu-backend.config :as cfg]
            [kulu-backend.search.schema :refer :all]))

(use-fixtures :each (join-fixtures [isolate-db reset-es-indexes]))

(defn total-and-uniqs-for [field search-results]
  (let [total (:total search-results)
        f-get-field #(get-in % [:_source field])
        uniqs (set (map f-get-field (:hits search-results)))]
    [total uniqs]))

(defn bulk-create []
  (let [id (java.util.UUID/randomUUID)
        docs (es-bulk/bulk-index (for [n ["Flipkart" "ACT" "3oh3"]
                                       c ["INR" "USD"]
                                       a [123.23 2500]
                                       d ["2014-08-29" "2014-09-18"]
                                       s ["uploading" "processed"]]
                                   {:_index (cfg/es-index)
                                    :_type invoice-mapping-type
                                    :organization-id id
                                    :name n
                                    :currency c
                                    :amount a
                                    :date d
                                    :status s}))]
    (search/in-es (es-bulk/bulk-with-index-and-type
                              "invoices"
                              docs
                              :refresh true))
    id))

(deftest test-create-or-update
  (let [{id :id} (inv-model/create {:name "Invoice#1"
                                    :date (java.sql.Date/valueOf "2014-08-23")
                                    :amount 123.23
                                    :currency "INR"})
        i (inv-model/lookup id)]
    (testing "creates a document if it doesn't exist"
      (let [_ (create-or-update i)
            res (lookup id)
            found (:found res)
            name (get-in res [:_source :name])]
        (is found)
        (is (= name "Invoice#1"))))

    (testing "updates the document if it already exist"
      (create-or-update i)
      (let [new-i (assoc i :name "New Name")
            _ (create-or-update new-i)
            name (get-in (lookup id) [:_source :name])]
        (is (= name "New Name"))))

    (testing "sets the ES doc's id as the invoice's id"
      (create-or-update i)
      (let [{found-id :_id} (lookup id)]
        (is (= found-id (str id)))))))

(deftest test-search
  (let [id (bulk-create)
        sort-params [{"name" "asc"}]
        size-params {:size 50 :from 0}]
    (testing "supports simple queries"
      (let [q ""
            filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}]}}
            [total uniqs] (total-and-uniqs-for :name (search "" filters sort-params size-params))]
        (is (= total 16))
        (is (= #{"Flipkart"} uniqs))))

    (testing "supports alternative word queries (OR)"
      (let [[total uniqs] (total-and-uniqs-for :name (search "flipkart or 3oh3" {} sort-params size-params))]
        (is (= total 32))
        (is (= #{"Flipkart" "3oh3"} uniqs))))

    (testing "supports filtering with a date range (both ends provided)"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:range {:date {:from "2014-08-01" :to "2014-08-31"}}}]}}
            [total uniqs] (total-and-uniqs-for :date (search "" filters sort-params size-params))]
        (is (= total 8))
        (is (= #{"2014-08-29"} uniqs))))

    (testing "supports filtering with a date range (only start provided)"
      (let [filters{:bool {:must [{:term {:organization-id id}}
                                  {:term {:name "flipkart"}}
                                  {:range {:date {:from "2014-09-01"}}}]}}
            [total uniqs] (total-and-uniqs-for :date (search "" filters sort-params size-params))]
        (is (= total 8))
        (is (= #{"2014-09-18"} uniqs))))

    (testing "supports filtering with a date range (only end provided)"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:range {:date {:to "2014-08-31"}}}]}}
            [total uniqs] (total-and-uniqs-for :date (search "" filters sort-params size-params))]
        (is (= total 8))
        (is (= #{"2014-08-29"} uniqs))))

    (testing "supports filtering amount (both ends provided)"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:range {:amount {:from 100 :to 150}}}]}}
            [total uniqs] (total-and-uniqs-for :amount (search "" filters sort-params size-params))]
        (is (= total 8))
        (is (= #{123.23} uniqs))))

    (testing "supports filtering amount (only start provided)"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:range {:amount {:from 150}}}]}}
            [total uniqs] (total-and-uniqs-for :amount (search "" filters sort-params size-params))]
        (is (= total 8))
        (is (= #{2500} uniqs))))

    (testing "supports filtering amount (only end provided)"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:range {:amount {:to 150}}}]}}
            [total uniqs] (total-and-uniqs-for :amount (search "" filters sort-params size-params))]
        (is (= total 8))
        (is (= #{123.23} uniqs))))

    (testing "supports filtering by currency"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:term {:currency "INR"}}]}}
            [total uniqs] (total-and-uniqs-for :currency (search "" filters sort-params size-params))]
        (is (= 8 total))
        (is (= #{"INR"} uniqs))))

    (testing "supports filtering by state"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:term {:status "processed"}}]}}
            [total uniqs] (total-and-uniqs-for :status (search "" filters sort-params size-params))]
        (is (= 8 total))
        (is (= #{"processed"} uniqs))))

    (testing "supports combining all the above"
      (let [filters {:bool {:must [{:term {:organization-id id}}
                                   {:term {:name "flipkart"}}
                                   {:term {:currency "INR"}}
                                   {:range {:date {:from "2014-08-01" :to "2014-08-31"}}}
                                   {:term {:status "processed"}}
                                   {:range {:amount {:from 100 :to 150}}}]}}
            total (get-in (search "" filters sort-params size-params) [:total])]
        (is (= total 1))))))
