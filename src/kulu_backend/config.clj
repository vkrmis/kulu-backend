(ns kulu-backend.config
  (:require [clojure.java.io :as io]
            [nomad :refer [defconfig]]))

(defconfig app-config (io/file "config/kulu-backend-config.edn"))

(defn aws-creds []
  (get-in (app-config) [:aws :creds]))

(defn sqs-base-url []
  (get-in (app-config) [:aws :sqs :base-url]))

(defn invoice-bucket []
  (get-in (app-config) [:aws :s3 :bucket-invoices]))

(defn database-url []
  (let [cfg (app-config)]
    (or (:db-url cfg)
        (-> cfg
            :placeholder-db-url
            (format (System/getProperty "user.name"))))))

(defn redis-spec []
  (get-in (app-config) [:redis-spec]))

(defn mailgun-user []
  (get-in (app-config) [:mailgun :user]))
(defn mailgun-pass []
  (get-in (app-config) [:mailgun :password]))

(defn mail-subject []
  (get-in (app-config) [:export :subject]))
(defn mail-body []
  (get-in (app-config) [:export :body]))
(defn max-export-rows []
  (get-in (app-config) [:export :max-page]))

(defn web-client-name []
  ((app-config) :web-client-name))

(defn es-connection-str []
  (get-in (app-config) [:elasticsearch :url]))
(defn es-index []
  (get-in (app-config) [:elasticsearch :index]))
(defn no-of-shards []
  (get-in (app-config) [:elasticsearch :shards]))
