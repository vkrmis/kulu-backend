(ns kulu-backend.search.common
  (:require [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.admin :refer [cluster-health]]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.index :as esi]
            [kulu-backend.config :as cfg]
            [clojure.string :as s]
            [kulu-backend.search.schema :refer :all]))

(defmacro in-es [[f & args]]
  `(let [conn# (esr/connect (cfg/es-connection-str))
         index# (cfg/es-index)]
     (~f conn# index# ~@args)))

(defn index-exists? []
  (in-es (esi/exists?)))

(defn create-indexes []
  (let [mappings {invoice-mapping-type invoice-mapping}]
    (if-not (index-exists?)
      (in-es (esi/create :mappings mappings :settings {"number_of_shards" (cfg/no-of-shards)})))))

(defn put [mapping-type id doc]
  (in-es (esd/put mapping-type (str id) doc)))

(defn lookup [mapping-type id]
  (in-es (esd/get mapping-type (str id))))

(defn search
  ([qs]
   (in-es (esd/search invoice-mapping-type :query (q/query-string :query qs))))
  ([qs filters sort size-params]
   (let [base-filters {:filter filters}
         query-and-filters (if (s/blank? qs) base-filters (assoc base-filters :query (q/query-string :query qs)))
         params (merge {:sort sort :query (q/filtered query-and-filters)} size-params)]
     (in-es (esd/search invoice-mapping-type params)))))

(defn single-node-availiable?
  "checks if the es node seems to be availiable at all by seeing if the http-req
  throws a ConnectException or not"
  []
  (let [working? (promise)]
    (try
      (in-es (cluster-health nil))
      (deliver working? true)
      (catch java.net.ConnectException connect-exception
        (deliver working? false))
      (finally nil)) ;; no side effects needed
    @working?))

#_ (assert (single-node-availiable?))
