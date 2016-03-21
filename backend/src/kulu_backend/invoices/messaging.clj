;;; TODO:
;;; Write tests
(ns kulu-backend.invoices.messaging
  (:require [aws.sdk.s3 :as s3 :only [copy-object]]
            [clojure.tools.logging :as log]
            [kulu-backend.invoices.model :as inv]
            [clj-time.coerce :as time-c]
            [kulu-backend.invoices.search :as inv-search]
            [kulu-backend.config :as cfg]))

(def mailer-folder "mailer/")

(defn update-invoice
  [id _]
  (log/infof "Marking invoice %s as uploaded" id)
  (inv/update id {:status (:submitted inv/states)}))

(defn store-invoice
  [m]
  (log/infof "Storing invoice %s" m)
  (let [inv (inv/create (merge {:status (:submitted inv/states)} m))]
    (future (inv-search/create-or-update inv))))

(defn- parse-data
  [data]
  (-> data
      (select-keys [:organization-id :remarks :date :email :expense_type])
      (update-in [:date] time-c/to-sql-date)))

(defn write
  [file {:keys [name content-type url] :as data}]
  (let [file-name (str mailer-folder name)
        expense-info (parse-data data)
        metadata {:content-type content-type}]
    (if file
      (do
        (log/infof "Writing %s|%s" file-name url)
        (s3/put-object (cfg/aws-creds) (cfg/invoice-bucket) file-name file metadata)
        (store-invoice (assoc expense-info :storage_key file-name)))
      (store-invoice (assoc expense-info :conflict true)))))
