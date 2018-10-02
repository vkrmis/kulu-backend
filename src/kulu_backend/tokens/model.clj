(ns kulu-backend.tokens.model
  (:require [kulu-backend.redis :as r]
            [taoensso.carmine :as car :refer (wcar)]))

(defn- token-key
  [key info]
  (str (clojure.string/join ":" info) ":" (name key)))

(defn create-token
  [key info]
  (let [token (str (java.util.UUID/randomUUID))]
    (r/wcar* (car/set (token-key key info) token))
    token))

(defn verify-token
  [key info token]
  (= (str token) (r/wcar* (car/get (token-key key info)))))

(defn expire-token
  [key info]
  (r/wcar* (car/expire (token-key key info) 0)))
