(ns kulu-backend.invoices.export
  (:require [dk.ative.docjure.spreadsheet :as excel]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [clj-time.format :as time-f]
            [kulu-backend.session-tokens.model :as token]
            [kulu-backend.invoices.mailer :as mail]
            [kulu-backend.config :as cfg]))

(defonce keyword-map {:name "Merchant Name"
                      :from-date "Expense Date From"
                      :to-date "To Expense Date"
                      :currency "Currency"
                      :amount "Amount"
                      :min-amount "Minimum Amount"
                      :max-amount "Maximum Amount"
                      :expense-type "Type"
                      :status "Status"
                      :conflict "Conflicts"
                      :user-name "Spender"
                      :category-name "Category"
                      :from-submission-date "From Submission Date"
                      :to-submission-date "To Submission Date"})

(defn construct-search-query
  [params]
  (reduce (fn [a-vector tuple]
            (let [k (keyword-map (first tuple))
                  v (second tuple)]
              (if k
                (conj a-vector (vector k v))
                a-vector)))
          [["Search Parameter" "Value"]]
          (into [] params)))

(defn create-query-table
  [search-query]
  (let [table-content (reduce (fn [string values]
                  (-> string
                      str
                      (str "<tr><th>" (first values) "</th>")
                      (str "<th>" (second values) "</th></tr>")))
                ""
                search-query)]
    (-> "<table border=\"1\">"
        (str table-content "</table>"))))

(defn create-body-html
  [body-text search-table]
  (-> "<html><head></head><body>"
      (str body-text)
      (str search-table)
      (str "</body></html>")))

(defn send-mail
  [file params]
  (let [today (time-f/unparse-local (time-f/formatter "Y-MM-dd") (time/today))
        subject (str (cfg/mail-subject) today)
        email-body (->> params construct-search-query create-query-table (create-body-html (cfg/mail-body)))
        user-email (-> params
                       :token
                       str
                       token/lookup-by-id
                       :user-email)]
    (mail/send-mail subject user-email email-body file)))

(defn create-sheet
  "Creates a spreadsheet of the data to ne sent to the user via email"
  [{:keys [headers rows] :as data} params]
  (let [today (time-f/unparse-local (time-f/formatter "Y-MM-dd") (time/today))
        search-query (construct-search-query params)
        search-query-count (count search-query)
        wb (excel/create-workbook "Expenses" search-query)
        sheet (excel/select-sheet "Expenses" wb)
        header-row (first (excel/row-seq sheet))
        filename (.getAbsolutePath (java.io.File/createTempFile  (str "Kulu_Data_Export_" today) ".xlsx"))]
    (excel/set-row-style! header-row (excel/create-cell-style! wb {:background :yellow,
                                                                   :font {:bold true}}))
    (excel/add-rows! sheet [[""] headers])
    (excel/set-row-style! (last (excel/row-seq sheet)) (excel/create-cell-style! wb {:background :yellow,
                                                                   :font {:bold true}}))
    (excel/add-rows! sheet rows)
    (excel/save-workbook! filename wb)
    filename))
