(ns kulu-backend.invoices.mailer
  (:require [kulu-backend.config :as cfg]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [kulu-backend.invoices.messaging :as inv-msg]
            [cheshire.core :as cj :only [decode encode]]
            [clj-http.client :as client]
            [clojure.string :as str]
            [mailgun.mail :as mail]
            [mailgun.util :refer [to-file]]))

(defn- creds [] {:key (cfg/mailgun-pass)
                 :domain (cfg/web-client-name)})

(defn- uuid [] (str (java.util.UUID/randomUUID)))

(defn mailgun-send-from []
  (str "no-reply@" (cfg/web-client-name)))

(defn send-mail
  [subject to body & attachments]
  (let [message {:from (mailgun-send-from)
                 :to to
                 :subject subject
                 :html body
                 :attachment (to-file attachments)}]
    (mail/send-mail (creds) message)))

(defn- parse-subject
  [subject-header]
  (-> subject-header
      (str/replace #"(?i)Fwd:" "")
      (str/trim)))

(defn parse
  "Currently only parses the biggest attachment, subject and the date
and queues it to be later downloaded and stored to S3"
  [mail]
  (let [parse (fn [key] (cj/decode (key mail) true))
        message-headers (into {} (parse :message-headers))
        subject (parse-subject (message-headers "Subject"))
        from-header (message-headers "X-Envelope-From")
        from (first (re-seq #"[a-zA-z0-9_.]+@[a-zA-Z0-9_.]+\.[A-z]{2,}" from-header))
        expense_type (mail :expense_type)
        date (message-headers "Date")
        biggest-attachment (when (:attachments mail)
                             (apply max-key :size (parse :attachments)))]
    {:remarks subject
     :name (str (uuid) "-" (:name biggest-attachment))
     :url (:url biggest-attachment)
     :content-type (:content-type biggest-attachment)
     :email from
     :expense_type (or expense_type "")
     :date date}))

(defn write
  [{:keys [url] :as m}]
  (let [attachment (when url
                     (log/infof "Downloading file %s" url)
                     (mail/download-attachment (creds) url))]
    (inv-msg/write attachment m)))
