(ns kulu-backend.models.helpers.pagination
  (:require [honeysql.helpers :refer :all]
            [ring.swagger.schema :as ring-s :only [describe]]
            [schema.core :as s]))

(defonce default-page-num 1)
(defonce default-per-page 25)
(defonce default-order "created_at")
(defonce default-direction "desc")
(defonce min-per-page 1)
(defonce max-per-page 10000)

(defn- per-page-ok?
  [item]
  (contains? (set (range min-per-page max-per-page)) item))

(s/defschema PaginationParams {(s/optional-key :page)
                               (ring-s/describe
                                (s/both Long (s/pred pos? 'pos?))
                                (format "Page being requested (more than 0).
                                         Default: %d." default-page-num))
                               (s/optional-key :order)
                               (ring-s/describe
                                (s/maybe String)
                                (format "Please pass a string"))
                               (s/optional-key :direction)
                               (ring-s/describe
                                (s/maybe String)
                                (format "Please pass a string"))
                               (s/optional-key :token)
                               (ring-s/describe
                                 (s/maybe String)
                                 (format "Please pass a string"))
                               (s/optional-key :per_page)
                               (ring-s/describe
                                (s/both Long (s/pred per-page-ok? 'per-page-ok?))
                                (format "Number of items to load in a page
                                         (between %d and %d). Default: %d."
                                        min-per-page
                                        (dec max-per-page)
                                        default-per-page))})

(s/defschema PaginationResults {:page
                                (s/both Long
                                        (s/pred pos? 'pos?))
                                :per_page
                                (s/both Long
                                        (s/pred per-page-ok? 'per-page-ok?))
                                :direction
                                (s/maybe String)
                                :order
                                (s/maybe String)
                                :total_pages
                                (s/both Long
                                        (s/pred #(not (neg? %1)) 'not-negative?))
                                :total_count
                                (s/both Long
                                        (s/pred #(not (neg? %1)) 'not-negative?))})

(defn- calculate-offset
  [limit page]
  (* (dec page) limit))

(defn- optional-order
  [q]
  (if (contains? q :order-by)
    {} ; nothing to add
    (order-by :id)))

(defn- calculate-total-pages
  [total-count per-page]
  (let [per-page (or per-page default-per-page)]
    (long (java.lang.Math/ceil (/ total-count per-page)))))

(defn info
  [{:keys [total-count per-page page order direction] :as m}]
  (let [page (or page default-page-num)
        per-page (or per-page default-per-page)
        order (or default-order)
        direction (or default-direction)
        total-pages (calculate-total-pages total-count per-page)]
    (merge m
           {:page page
            :per-page per-page
            :total-pages total-pages
            :order order
            :direction direction})))

(defn paginate
  [query page per-page]
  {:pre [(contains? query :order-by)]}
  (let [page (or page default-page-num)
        per-page (or per-page default-per-page)]
    (into {} [query
              (optional-order query)
              (limit per-page)
              (offset (calculate-offset per-page page))])))
