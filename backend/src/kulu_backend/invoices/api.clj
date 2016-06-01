(ns kulu-backend.invoices.api
  (:require [clj-time.coerce :as time-c]
            [clj-time.format :as time-f]
            [clojure.tools.logging :as log]
            [kulu-backend.currencies :refer [Currency valid?] :as curr]
            [kulu-backend.invoices.model :as inv]
            [kulu-backend.organizations.model :as org]
            [kulu-backend.invoices.mailer :as inv-mailer]
            [kulu-backend.invoices.search :as inv-search]
            [kulu-backend.config :as cfg]
            [kulu-backend.models.helpers.pagination :as pagn]
            [ring.swagger.schema :as ring-s :only [describe]]
            [clojure.string :as str]
            [schema.core :as s :refer [defschema optional-key maybe pred enum Str Inst Num Uuid]]
            [kulu-backend.invoices.export :as excelify]))

(s/defschema InvoiceID
  (ring-s/field Uuid
                {:description "UUID formatted string"}))

(defschema Invoice
  "schema for Invoices at the 'web side' of things"
  {(optional-key :organization_name)           (maybe Str)
   (optional-key :id)                          (maybe Uuid)
   (optional-key :organization_id)             (maybe Uuid)
   (optional-key :name)                        (maybe Str)
   (optional-key :date)                        (maybe Inst)
   (optional-key :amount)                      (maybe Num)
   (optional-key :currency)                    (maybe Str)
   (optional-key :status)                      (maybe Str)
   (optional-key :remarks)                     (maybe Str)
   (optional-key :expense_type)                (maybe Str)
   (optional-key :attachment_url)              (maybe Str)
   (optional-key :attachment_content_type)     (maybe Str)
   (optional-key :email)                       (maybe Str)
   (optional-key :role)                        (maybe Str)
   (optional-key :user_name)                   (maybe Str)
   (optional-key :category_id)                 (maybe Uuid)
   (optional-key :category_name)               (maybe Str)
   (optional-key :created_at)                  (maybe Inst)
   (optional-key :conflict)                    (maybe Boolean)})

(s/defschema PaginatedInvoiceList
  "paginated results as well as meta data for the results"
  {:meta                          pagn/PaginationResults
   :items                         [Invoice]})

(s/defschema GetInvoicesQueryParams
  (merge {:organization_name s/Str} pagn/PaginationParams))

(s/defschema ChangeInvoiceBodyParams
  {(optional-key :name)           Str
   (optional-key :date)           Inst
   (optional-key :amount)         Num
   (optional-key :currency)       Currency
   (optional-key :remarks)        Str
   (optional-key :expense_type)   Str
   (optional-key :email)          Str
   (optional-key :user_name)      Str
   (optional-key :status)         Str
   (optional-key :category_id)    Uuid
   (optional-key :conflict)       Str})

(s/defschema CreateInvoiceBodyParams
  {(optional-key :storage_key)    Str
   (optional-key :id)             Uuid
   (optional-key :name)           Str
   (optional-key :date)           Inst
   (optional-key :amount)         Num
   (optional-key :currency)       Currency
   (optional-key :remarks)        Str
   (optional-key :expense_type)   Str
   (optional-key :email)          Str
   (optional-key :user_name)      Str
   (optional-key :user_token)     Uuid })

(s/defschema SearchInvoicesQueryParams
  (merge pagn/PaginationParams
         {:organization_name                   s/Str
          (optional-key :q)                    s/Str
          (optional-key :name)                 s/Str
          (optional-key :token)                (maybe Uuid)
          (optional-key :from_date)            Inst
          (optional-key :to_date)              Inst
          (optional-key :from_submission_date) Inst
          (optional-key :to_submission_date)   Inst
          (optional-key :currency)             Currency
          (optional-key :min_amount)           Str
          (optional-key :max_amount)           Str
          (optional-key :amount)               Str
          (optional-key :expense_type)         inv/ExpenseType
          (optional-key :conflict)             Boolean
          (optional-key :user_name)            s/Str
          (optional-key :category_name)        s/Str
          (optional-key :status)               inv/ExpenseState}))

(defn- rm-private-fields
  "removes fields such as timestamps from the given invoice (map)"
  [m]
  (dissoc m :updated-at :storage-key :deleted :email-2 :id-2
          :created-at-2 :updated-at-2 :password :organization-id-2 :id-3
          :created-at-3 :updated-at-3 :name-2 :category-id-2 :priority :invoice-id
          :created-at-4 :updated-at-4 :id-4))

(defn- rm-non-update-fields
  [m]
  (dissoc m :user-name :email))

(defn index
  [m]
  {:meta (pagn/info (merge (select-keys m [:page :per-page :order :direction])
                           {:total-count (inv/size m)}))
   :items (map rm-private-fields (inv/all m))})

(defn lookup
  [id]
  (-> (inv/lookup-with-category id)
      rm-private-fields))

(defn conflict?
  [conflict]
  (= conflict "true"))

(defn- new-invoice?
  [id]
  (empty? (inv/lookup id)))

(defn update
  [[id attrs]]
  (when-let [original-i (inv/lookup id)]
    (-> attrs
        (update-in [:conflict] conflict?)
        (update-in [:date] time-c/to-sql-date)
        (->> (inv/update-with-category id)))
    (let [new-i (lookup id)]
      (future (inv-search/create-or-update new-i))
      (log/info "Updated Invoice %s from %s to %s"
                id original-i new-i)
      new-i)))

(defn- create-or-update-invoice
  [m]
  (if (new-invoice? (:id m))
    (inv/create
     (update-in m[:date] time-c/to-sql-date))
    (update [(:id m) (rm-non-update-fields m)])))

(defn create
  [m]
  (assert (map? m) "you have to supply a map as argument")
  (let [invoice (create-or-update-invoice m)]
    (future (inv-search/create-or-update invoice))
    (select-keys invoice [:id])))

(defn soft-delete
  "sets a record with id to soft deleted"
  [id]
  (log/info "deleting id: " id)
  (inv/update id {:deleted true})
  id)

(defn receive-mail
  "gets a post request from mailgun for an incoming mail,
parses this and enqueues creation of new invoices"
  [params]
  (log/debug "receieved an incoming mail, enqueueing attachments..." params)
  (let [organization-name (get-in params [:organization :name])
        mail-params (inv-mailer/parse params)
        invoice-params (assoc mail-params :organization-name organization-name)]
    (inv-mailer/write invoice-params)))

(defn next-and-prev-invoices
  [[id params]]
  (inv/next-and-prev-invoices id params))

(defn index
  [m]
  {:meta (pagn/info (merge (select-keys m [:page :per-page :order :direction])
                           {:total-count (inv/size m)}))
   :items (map rm-private-fields (inv/all m))})

(defn search
  [{:keys
    [organization-name q name
     from-date to-date from-submission-date to-submission-date
     currency min-amount max-amount amount
     status conflict expense-type
     user-name category-name
     order direction page per-page] :as params}]

  (let [organization-id (str (:id (org/lookup-by-name organization-name)))
        valid-case   (fn [i] (if (str/blank? i) i (str/lower-case i)))
        valid-number (fn [i] (if (str/blank? i) i (read-string i)))
        order (or order "name")
        direction (or direction "asc")
        sort-params {(str/replace (clojure.core/name order) #"_" "-") direction}

        components [{[:term :organization-id] organization-id}
                    {[:term :name] (valid-case name)}
                    {[:term :status] (valid-case status)}
                    {[:term :expense-type] (valid-case expense-type)}
                    {[:term :currency] (valid-case currency)}
                    {[:term :user-name] (valid-case user-name)}
                    {[:term :category-name] (valid-case category-name)}
                    {[:term :conflict] conflict}
                    {[:term :amount] (valid-number amount)}
                    {[:range :date :from] from-date [:range :date :to] to-date}
                    {[:range :created-at :from] from-submission-date [:range :created-at :to] to-submission-date}
                    {[:range :amount :gt] (valid-number min-amount)  [:range :amount :lt] (valid-number max-amount)}]

        build-components (remove empty? (map #(reduce-kv (fn [m k v]
                                                           (if (nil? v)
                                                             m (assoc-in m k v))) {} %) components))

        filters {:bool {:must build-components}}]
    (log/infof "Searching invoices with params: %s, filters %s" params filters)
    (let [results (inv-search/search q filters [sort-params] {:size per-page :from (Math/abs (* (dec page) per-page))})
          total-count (:total results)
          hits (->> (:hits results)
                    (map (fn [res] (assoc (:_source res) :id (:_id res)))))]
      {:meta (pagn/info {:total-count total-count
                         :per-page per-page
                         :page page
                         :order order :direction direction}) :items hits})))

(defn export
  [{:keys [organization-name] :as params}]
  (let [per-page (cfg/max-export-rows)
        results (:items (search (assoc params :per-page per-page)))
        _ (log/infof "Search results have returned.")
        headers [:name :date :currency :amount
                 :expense-type :status :conflict
                 :user-name :email :remarks :category-name :created-at :id]

        relevant-headers ["Merchant Name" "Expense Date" "Currency" "Amount"
                          "Type" "Status" "Conflict"
                          "Spender" "Email" "Remarks" "Category" "Submission Date" "Link"]

        parse-date-time (fn [datetime] (time-f/unparse (time-f/formatters :mysql)
                                                       (time-f/parse (time-f/formatters :date-time-no-ms) datetime)))
        parse-date (fn [datetime] (time-f/unparse (time-f/formatters :date)
                                                  (time-f/parse (time-f/formatters :date-time-no-ms) datetime)))
        relevant-rows (map #(map (-> %
                                     (update-in [:date] parse-date)
                                     (update-in [:created-at] parse-date-time)
                                     (update-in [:id] inv/invoice-web-link organization-name)) headers) results)
        data {:headers relevant-headers :rows relevant-rows}]
    (-> data
        (excelify/create-sheet params)
        (excelify/send-mail params))))
