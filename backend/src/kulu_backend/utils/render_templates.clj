(ns kulu-backend.utils.render-templates
  (:require [stencil.core :as mus]
            [clojure.java.io :as io]))

(defn render
  ([^String path data]
   (mus/render-file (str "templates/" path) data)))
