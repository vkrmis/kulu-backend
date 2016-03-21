(ns kulu-backend.invoices.mailer
  (:require [kulu-backend.config :as cfg]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [kulu-backend.invoices.messaging :as inv-msg]
            [cheshire.core :as cj :only [decode encode]]
            [clj-http.client :as client]
            [clojure.string :as str]))

(defn- uuid [] (str (java.util.UUID/randomUUID)))

(defn download-attachment
  [url]
  (log/infof "Downloading file %s" url)
  (io/input-stream
   (:body
    (client/get url
                {:basic-auth [(cfg/mailgun-user) (cfg/mailgun-pass)]
                 :socket-timeout 10000
                 :conn-timeout 10000
                 :as :byte-array}))))

(defn mailgun-url []
  (str "https://api:" (cfg/mailgun-pass) "@api.mailgun.net/v2/" (cfg/web-client-name) "/messages"))

(defn mailgun-send-from []
  (str "no-reply@" (cfg/web-client-name)))

(defn send-mail
  ([subject to body]  (let [mail-params {:from (mailgun-send-from)
                                         :to to
                                         :subject subject
                                         :html body}]
                        (client/post (mailgun-url) {:form-params mail-params})))
  ([subject to body attachment]  (let [mail-params [{:name "from" :content (mailgun-send-from)}
                                                    {:name "to" :content to}
                                                    {:name "subject" :content subject}
                                                    {:name "html" :content body}
                                                    {:name "attachment" :content (clojure.java.io/file attachment)}]]
                                   (client/post (mailgun-url) {:multipart mail-params}))))

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
                     (download-attachment url))]
    (inv-msg/write attachment m)))
