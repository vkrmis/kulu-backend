(ns kulu-backend.tasks
  (:require [clojure.tools.logging :as log]
            [kulu-backend.invoices.search :as inv-search]
            [kulu-backend.search.common :as kulu-search]))

(defn- migrate-indexes
  []
  (log/info "Migrating indexes")
  (inv-search/migrate-indexes)
  (System/exit 0))

(defn- setup
  [env]
  (log/infof "Setting up system in environment: %s" env)
  (System/setProperty "nomad.env" env)
  (kulu-search/create-indexes)
  (migrate-indexes)
  (System/exit 0))

(defn -main
  "Sets up resources for the kulu backend service.
  Commands
  setup env       migrates DB, creates ElasticSearch indexes.
                  env is one of production/dev/test (default: dev).
  migrate-indexes creates ES document indexes for all invoices "
  ([] (println (:doc (meta #'-main))))
  ([command & args]
     (let [[e & _] args
           env (or e "dev")]
       (condp = command
         "setup" (setup env)
         "migrate-indexes" (migrate-indexes)))))
