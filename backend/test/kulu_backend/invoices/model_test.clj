(ns kulu-backend.invoices.model-test
  (:require [clojure.test :refer :all]
            [conjure.core :refer :all]
            [kulu-backend.db :as db]
            [kulu-backend.invoices.messaging :as inv-msg]
            [kulu-backend.invoices.model :as inv]
            [kulu-backend.organizations.model :as org]
            [kulu-backend.invoices.search :as inv-search]
            [kulu-backend.test-helper :refer :all]
            [kulu-backend.config :as cfg]))

(use-fixtures :each isolate-db)
(use-fixtures :once set-test-env)

(deftest create-invoice
  (testing "creates an invoice record"
    (let [count-before (inv/size {})
          inv (inv/create {:name "foobar"})]
      (is (= (+ 1 count-before)
             (inv/size inv)))))

  (testing "generates an id and persist the invoice"
    (let [expected-name "Flipkart"
          {:keys [id name]} (inv/create {:name expected-name})]
      (is ((comp not empty?) (str id)))
      (is (= expected-name name))))

  (testing "newly created invoice should be in uploaded state"
    (let [n "Flipkart"
          expected-state (:submitted inv/states)
          {:keys [id status]} (inv/create {:name n})]
      (is (= status expected-state)))))

(deftest lookup-invoice
  (testing "finds the invoice for the given id"
    (let [key "foo/bar.pdf"
          {id :id} (inv/create {:storage-key key})]
      (is (= key (:storage-key (inv/lookup id))))))

  (testing "returns nil if there is invoice for the given id"
    (let [non-existent-id #uuid "fb857379-1b66-4499-b705-08ec68601c6c"]
      (is (nil? (inv/lookup non-existent-id)))))

  (testing "adds the attachment URL after the attachment is uploaded"
    (let [key "foo/bar.pdf"
          {id :id} (inv/create {:storage-key key})
          _ (inv/update id {:name "Invoice #1"})
          attachment-url (:attachment-url (inv/lookup id))]
      (is ((comp not nil?) attachment-url)))))

(deftest get-invoices
  (testing "fetches all the invoices"
    (let [org (org/store {:name "filenso"})
          names ["Flipkart" "Harvest" "Cognitect"]]
      (doall (map #(inv/create {:name %1 :organization-name "filenso"})
                  names))
      (let [result-names (map :name (inv/all {:organization-name "filenso"}))]
        (is (= (sort names)
               (sort result-names)))))))

(deftest get-invoices-pagination
  (testing "paginates the results"
    (let [org (org/store {:name "filenso"})]
      (doseq [d (range 1 9)]
        (->> d
             (format "%02d")
             (str "2014-08-")
             (java.sql.Date/valueOf)
             (hash-map :organization-name "filenso" :name (str d) :date)
             (inv/create)))

      (let [per-page 3
            page-1-names (map :name (inv/all {:page 1 :per-page per-page :organization-name "filenso"}))
            page-2-names (map :name (inv/all {:page 2 :per-page per-page :organization-name "filenso"}))
            page-3-names (map :name (inv/all {:page 3 :per-page per-page :organization-name "filenso"}))
            page-4-names (map :name (inv/all {:page 4 :per-page per-page :organization-name "filenso"}))]
        (is (= (map str [8 7 6]) page-1-names))
        (is (= (map str [5 4 3]) page-2-names))
        (is (= (map str [2 1]) page-3-names))
        (is (= [] page-4-names))))))

(deftest update-invoice
  (let [{id :id} (inv/create {:name "Name"})]
    (testing "changes the requested attrs and status of the invoice"
      (let [e-status (:submitted inv/states)
            _ (inv/update id {:name "New Name"})
            {:keys [name status]} (inv/lookup id)]
        (is (= name "New Name"))
        (is (= status e-status))))))

(deftest delete-invoice
  (testing "create invoice, look for it, delete it, make sure we dont find it again"
    (let [{id :id} (inv/create {:name "Deleter Deleteson"})
          before-removal (inv/lookup id)
          _ (inv/delete! id)
          after-removal (inv/lookup id)]
      (is (not (nil? id)))
      (is (not (= before-removal after-removal)))
      (is (map? before-removal))
      (is (nil? after-removal)))))

(deftest next-and-prev-invoice
  (testing "find the next and prev invoices for a given invoice within an organizations"
    (let [{org1-id :id} (org/store {:name "filenso"})
          {org2-id :id} (org/store {:name "flipcar"})
          {inv1-id :id} (inv/create {:organization-name "flipcar"})
          {inv2-id :id} (inv/create {:organization-name "filenso"})
          {inv3-id :id} (inv/create {:organization-name "flipcar"})
          {inv4-id :id} (inv/create {:organization-name "filenso"})
          {inv5-id :id} (inv/create {:organization-name "flipcar"})
          {inv6-id :id} (inv/create {:organization-name "filenso"})]
      (let [result1 (dissoc (inv/next-and-prev-invoices inv1-id {:order "created_at"
                                                                 :direction "desc"
                                                                 :organization-name "flipcar"}) :id)
            result2 (dissoc (inv/next-and-prev-invoices inv2-id {:order "created_at"
                                                                 :direction "desc"
                                                                 :organization-name "filenso"}) :id)
            result3 (dissoc (inv/next-and-prev-invoices inv3-id {:order "created_at"
                                                                 :direction "desc"
                                                                 :organization-name "flipcar"}) :id)
            result4 (dissoc (inv/next-and-prev-invoices inv4-id {:order "created_at"
                                                                 :direction "desc"
                                                                 :organization-name "filenso"}) :id)]
        (is (= {:next-item-id nil :prev-item-id inv3-id} result1))
        (is (= {:next-item-id nil :prev-item-id inv4-id} result2))
        (is (= {:next-item-id inv1-id :prev-item-id inv5-id} result3))
        (is (= {:next-item-id inv2-id :prev-item-id inv6-id} result4))))))
