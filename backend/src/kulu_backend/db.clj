(ns kulu-backend.db
  (:require [clj-time.coerce :as time-convert]
            [clj-time.core :as time]
            [clojure.java.jdbc :as j]
            [kulu-backend.config :as cfg]
            [kulu-backend.utils.api :as api-utils]
            [clojure.tools.logging :as log])
  (:import [com.jolbox.bonecp BoneCPDataSource]))

(defn parse-config
  "Parses username/pwd etc. from a DB URL like
postgresql://username:password@localhost:5432/kulu_backend_dev"
  [url]
  (let [[_ user password host port db]
        (re-find #"^.*://(.*):(.*)@(.*):(.*)/(.*)$" url)]
    {:classname "org.postgresql.Driver"
     :subprotocol "postgresql"
     :subname (str "//" host ":" port "/" db)
     :user user
     :password password}))

(defn spec
  []
  (parse-config (cfg/database-url)))

(def min-pool 3)

(def max-pool 6)

(defn pool
  [spec]
  (let [partitions 3
        cpds (doto (BoneCPDataSource.)
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUsername (:user spec))
               (.setPassword (:password spec))
               (.setMinConnectionsPerPartition (inc (int (/ min-pool partitions))))
               (.setMaxConnectionsPerPartition (inc (int (/ max-pool partitions))))
               (.setPartitionCount partitions)
               (.setStatisticsEnabled true)
               ;; test connections every 25 mins (default is 240):
               (.setIdleConnectionTestPeriodInMinutes 25)
               ;; allow connections to be idle for 3 hours (default is 60 minutes):
               (.setIdleMaxAgeInMinutes (* 3 60))
               (.setConnectionTestStatement "/* ping *\\/ SELECT 1"))]
    {:datasource cpds}))

(def pooled-db
  (delay
   (do
     (log/info "connecting to postgresql with " (spec))
     (pool (spec)))))

(defn connection
  []
  @pooled-db)

(defn exec-transaction
  [jdbc-fn args]
  (j/with-db-transaction [conn (connection)]
    (api-utils/dasherize
     (api-utils/idiomatize-keys-for-sql #(apply (partial jdbc-fn conn) %1)
                                        args))))

(defn query
  [& args]
  (exec-transaction j/query args))

;; TODO:
;; jdbc methods already run in a transaction by default
;; see how we can separate exec-transaction v j/<method> out
(defn insert!
  [table-name & [args & _]]
  (let [now (time-convert/to-sql-time (time/now))]
    (exec-transaction j/insert!
                      [table-name
                       (merge {:created_at now :updated_at now} args)])))

(defn exec-nested-transactions
  [fns]
  (j/with-db-transaction [conn (connection)]
    ((apply comp (reverse fns)))))

(defn insert-without-timestamps!
  [table-name & [args]]
  (exec-transaction j/insert! [table-name args]))

(defn update!
  [table-name set-map where-clause]
  (let [now (time-convert/to-sql-time (time/now))]
    (exec-transaction j/update!
                      [table-name
                       (merge {:updated_at now} set-map)
                       where-clause])))

(defn delete!
  "deletes all records matching the where-clause"
  [table-name where-clause]
  (assert (sequential? where-clause)  "the where clause must be a sequence")
  (assert (not (empty? where-clause)) "no clause; attempting to delete the whole table permitted")
  (try (exec-transaction j/delete! [table-name where-clause])
  (catch Exception e (.printStackTrace (.getNextException e)))))
