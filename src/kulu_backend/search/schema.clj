(ns kulu-backend.search.schema)

(def invoice-mapping-type "invoices")

(def invoice-mapping {:properties {:organization-id {:type "string"
                                                     :index "not_analyzed"}
                                   :name {:type "string"}
                                   :amount {:type "double"}
                                   :currency {:type "string"
                                              :index "not_analyzed"}
                                   :status {:type "string"
                                            :index "not_analyzed"}
                                   :date {:type "date" :format "date_optional_time"}
                                   :created-at {:type "date" :format "date_optional_time"}
                                   :remarks {:type "string"}
                                   :conflict {:type "boolean"}
                                   :email {:type "string"}
                                   :spender {:type "string"}
                                   :category {:type "string"}
                                   :expense-type {:type "string"}}
                      :_timestamp {:enabled true}})
