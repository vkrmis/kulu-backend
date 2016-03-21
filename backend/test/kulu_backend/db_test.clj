(ns kulu-backend.db-test
  (:require [clj-time.core :as time]
            [clj-time.coerce :as time-c]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [honeysql.core :as sql]
            [honeysql.helpers :as sql-h]
            [kulu-backend.db :as db]
            [kulu-backend.db :refer :all]
            [kulu-backend.test-helper :refer :all]))

(use-fixtures :once (join-fixtures [set-test-env migrate-test-db]))
(use-fixtures :each isolate-db)

(def table-name :db_test)

(defn setup-db-test-table
  []
  (jdbc/db-do-commands
   (db/connection)
   (jdbc/create-table-ddl
    table-name
    [:id :serial "PRIMARY KEY"]
    [:name "VARCHAR(50)"]
    [:currency "CHAR(3)"]
    [:created_at "TIMESTAMP WITHOUT TIME ZONE NOT NULL"]
    [:updated_at "TIMESTAMP WITHOUT TIME ZONE NOT NULL"])))

(defn teardown-db-test-table
  []
  (jdbc/db-do-commands (db/connection)
                       (jdbc/drop-table-ddl
                        table-name)))

(deftest insert-query
  (testing "adds created_at/updated_at by default"
    (setup-db-test-table)

    (let [current-time (time/date-time 2014 8 30 1 2 3 456)]
      (with-redefs [time/now (constantly current-time)]
        (let [expected-name "oogabooga"
              [{:keys [id]} & _] (db/insert! table-name {:name expected-name})
              [{:keys [name created-at updated-at]} & _] (-> (sql-h/select :name :created_at :updated_at)
                                                             (sql-h/from table-name)
                                                             (sql-h/where [:= :id id])
                                                             (sql/format)
                                                             (db/query))]

          (is (= expected-name name))
          (is (= current-time (time-c/from-sql-time created-at)))
          (is (= current-time (time-c/from-sql-time updated-at))))))

    (teardown-db-test-table))

  (testing "does not override created_at/updated_at if they are already specified"
    (setup-db-test-table)

    (let [created-time (time-c/to-sql-time (time/date-time 2014 8 25 0 0 0 0))
          updated-time (time-c/to-sql-time (time/date-time 2014 8 30 0 0 0 0))
          [{:keys [id]} & _] (db/insert! table-name {:name "oogabooga" :created_at created-time :updated_at updated-time})
          [{:keys [created-at updated-at]} & _] (-> (sql-h/select :created-at :updated-at)
                                                    (sql-h/from table-name)
                                                    (sql-h/where [:= :id id])
                                                    (sql/format)
                                                    (db/query))]

      (is (= created-at created-time))
      (is (= updated-at updated-time)))

    (teardown-db-test-table)))

(deftest update-query
  (testing "updates the updated_at"
    (setup-db-test-table)

    (let [created-time (time-c/to-sql-time (time/date-time 2014 8 25 0 0 0 0))
          updated-time (time-c/to-sql-time (time/date-time 2014 8 30 0 0 0 0))
          [{:keys [id]} & _] (db/insert! table-name {:name "oogabooga" :created_at created-time :updated_at updated-time})
          _ (db/update! table-name {:name "foobar"} ["id = ?" id])
          [{:keys [created-at updated-at]} & _] (-> (sql-h/select :created-at :updated-at)
                                                    (sql-h/from table-name)
                                                    (sql-h/where [:= :id id])
                                                    (sql/format)
                                                    (db/query))]

      (is (= created-at created-time))
      (is (not  (= updated-at updated-time))))


    (teardown-db-test-table))

  (testing "updates the column values"
    (setup-db-test-table)

    (let [original-name "jacob"
          expected-name "esau"
          original-currency "USD"
          expected-currency "INR"
          [{:keys [id]} & _] (db/insert! table-name {:name original-name :currency original-currency})
          _ (db/update! table-name {:name expected-name :currency expected-currency} ["id = ?" id])
          [{:keys [name currency]} & _] (-> (sql-h/select :name :currency)
                                            (sql-h/from table-name)
                                            (sql-h/where [:= :id id])
                                            (sql/format)
                                            (db/query))]

      (is (= name expected-name))
      (is (= currency expected-currency)))

    (teardown-db-test-table)))


(deftest delete-query
  (testing "creates an item, looks it up, deletes it, confirms it is gone"
    (setup-db-test-table)
    (let [created-time (time-c/to-sql-time (time/date-time 2014 8 25 0 0 0 0))
          updated-time (time-c/to-sql-time (time/date-time 2014 8 30 0 0 0 0))
          [{:keys [id]} & _] (db/insert! table-name {:name "oogabooga" :created_at created-time :updated_at updated-time})

          found-item (-> (sql-h/select :*)
                            (sql-h/from table-name)
                            (sql-h/where [:= :id id])
                            (sql/format)
                            (db/query))

          _ (db/delete! table-name ["id = ?" id])

          not-found-item (-> (sql-h/select :*)
                             (sql-h/from table-name)
                             (sql-h/where [:= :id id])
                             (sql/format)
                             (db/query))]
      (is (not (empty? found-item)))
      (is (empty? not-found-item)))

    (teardown-db-test-table)))

