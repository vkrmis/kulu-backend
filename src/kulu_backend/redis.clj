(ns kulu-backend.redis
  (:require [taoensso.carmine :as car :refer (wcar)]
            [kulu-backend.config :as cfg]))

(def conn {:pool {} :spec (cfg/redis-spec)})
(defmacro wcar* [& body] `(car/wcar conn ~@body))
