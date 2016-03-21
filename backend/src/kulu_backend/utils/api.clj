(ns kulu-backend.utils.api
  (:require [clojure.string :as str]
            [ring.swagger.json-schema :refer [json-type]]
            [ring.util.http-response :as ring-http-response]
            [schema.core :as s]
            [clj-time.format :as f]
            [clj-time.coerce :as c]))

;;; ring-swagger doesn't validate BigDecimal as Doubles or Ints, which is valid.
;;; Declare them as Numbers (provided by prismatic schema). However,
;;; ring-swagger doesn't know about "Num".
;;; See https://github.com/metosin/ring-swagger/issues/4
(defmethod json-type s/Num [_] {:type "number" :format "double"})

(defn- transform
  "recursively applies f to keyword keys in target. Note that only keyword keys
  are changed."
  [target f]
  (condp #(%1 %2) target
    map?
    (into {} (for [[k v] target]
               [(if (keyword? k) (f k) k)
                (transform v f)]))

    sequential?
    (into (empty target) (map #(transform %1 f) (seq target)))

    target))

(defn dasherize
  "uses transform to change keyword keys to `kabab-case` flavor"
  [target]
  (let [dasherize-fn #(keyword (str/replace (name %1) #"_" "-"))]
    (transform target dasherize-fn)))

(defn underscorize
  "uses transform to change keyword keys to `under_score` flavor"
  [target]
  (let [underscore-fn #(keyword (str/replace (name %1) #"-" "_"))]
    (transform target underscore-fn)))

(defn- idiomatize-with
  "transforms target by applying in-transformer-fn, applies f on that and
  applies out-transformer-fn before returning the result. See `idiomatize-keys`
  for example usage."
  [f target in-transformer-fn out-transformer-fn]
  (let [transformed-target (in-transformer-fn target)
        result (f transformed-target)]
    (out-transformer-fn result)))

(defn idiomatize-keys
  "transforms target into `kabab-case` flavor, applies f on that and then
  returns `under_score`izes the result before returning that. Makes working with
  `under_score` flavor params (such as from HTTP requests, or from SQL DB)
  idiomatic inside Clojure code."
  [f target]
  (idiomatize-with f target dasherize underscorize))

(defn idiomatize-keys-for-sql
  "The inverse of `idiomatize-keys`. `under-scoreizes` on the way in and
  `dasherizes` on the way out."
  [f target]
  (idiomatize-with f target underscorize dasherize))

(defn respond-with-found-or-missing
  "Convenience fn to generate a 200 or a 404 HTTP response.
   If item exists, responds with an HTTP ok (200) with the item in the body.
   Otherwise, responds with a formatted error and HTTP not-found (404)."
  [item item-name identifier-name identifier]
  (if item
    (ring-http-response/ok item)
    (ring-http-response/not-found
     {:errors {identifier-name
               (format "no %s found with %s: %s"
                       item-name identifier-name identifier)}})))

(def built-in-formatter (f/formatters :basic-date-time))
(def custom-formatter (f/formatter "yyyy-MM-dd"))

(defn format-date
  [date-str]
  (->> date-str (f/parse custom-formatter) c/to-timestamp))
