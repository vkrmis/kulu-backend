(ns kulu-backend.scripts.bulk-upload-invoices
  (:require  [aws.sdk.s3 :as s3 :only [copy-object]]
             [clojure.tools.logging :as log]
             [clojure.java.io :as io]
             [kulu-backend.invoices.model :as inv]
             [kulu-backend.organizations.model :as org]
             [kulu-backend.config :as cfg]
             [clj-time.coerce :as time-c]))

(defn all-files [path]
  (file-seq (io/file path)))

(defn only-files [files]
  (filter #(and (not (.isDirectory %)) (.isFile %)) files))

(defn write-to-s3 [file name]
  (s3/put-object (cfg/aws-creds) (cfg/invoice-bucket) name file))

(defn write-to-db [info storage-key]
  (prn info storage-key)
  (inv/store (assoc info :storage_key storage-key)))

(defn db-and-image [org-name user-email files]
  (reduce (fn [res file]
            (if-let [img (io/input-stream file)]
              (if-not (= (.getName file) ".DS_Store")
                (let [name (.getName file)
                      s3-token (write-to-s3 img name)
                      string-date (second (clojure.string/split name #"_"))
                      date (java.sql.Date. (.getTime (.parse (java.text.SimpleDateFormat. "yyyyMMdd") string-date))) ;; 20150325
                      org (org/lookup-by-name org-name)
                      db (write-to-db {:email user-email
                                       :expense-type "Company"
                                       :remarks ""
                                       :status "Submitted"
                                       :amount 0.0
                                       :currency "INR"
                                       :date date
                                       :organization_id (:id org)} name)]
                  (conj res name)))
              res))
          []
          files))

(defn db-only [org-name user-email file-names]
  (let [org (org/lookup-by-name org-name)]
    (reduce (fn [res file-name]
              (let [string-date (second (clojure.string/split file-name #"_"))
                    date (java.sql.Date. (.getTime (.parse (java.text.SimpleDateFormat. "yyyyMMdd") string-date)))]
                (write-to-db {:email user-email
                              :expense-type "Company"
                              :remarks ""
                              :status "Submitted"
                              :amount 0.0
                              :currency "INR"
                              :date date
                              :organization_id (:id org)} file-name))) [] (vec file-names))))

(defn -main [& args]
  (System/setProperty "nomad.env" (first args))
  (db-only (second args) (first (rest (rest args))) (rest (rest (rest args))))
  (System/exit 0))
