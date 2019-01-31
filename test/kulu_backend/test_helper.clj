(ns kulu-backend.test-helper
  (:require [clj-sql-up.migrate :as m]
            [clojure.java.jdbc :as j]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clojurewerkz.elastisch.rest.index :as esi]
            [kulu-backend.config :as cfg]
            [kulu-backend.db :as db]
            [kulu-backend.search.common :as search]))

(defn set-test-env
  [f]
  (System/setProperty "nomad.env" "test")
  (f))

(defn migrate-test-db
  [f]
  (m/migrate (db/spec))
  (f))

;;; TODO:
;;; Use the connection pool for tests as well
(defn isolate-db
  [f]
  (j/with-db-transaction [conn (db/spec)]
    (j/db-set-rollback-only! conn)
    (with-redefs [db/connection (fn [] conn)]
      (f))))

(defn silence-logging
  [f]
  (with-redefs [log/log* (constantly nil)]
    (f)))

(defn reset-es-indexes
  [f]
  (let [delete-test-index #(search/in-es (esi/delete))]
    (delete-test-index)
    (search/create-indexes)
    (f)))
