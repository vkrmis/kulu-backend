(ns kulu-backend.scripts.populate-data
  (:require [clojure.java.io :as io]
            [kulu-backend.scripts.bulk-upload-invoices :as bulk-upload]))

(defn get-files-from-dir
  [dir-path]
  (.listFiles (io/file dir-path)))

(defn get-data
  [org-name user-email dir-path]
  (when (.isDirectory (io/file dir-path))
    (let [files (get-files-from-dir dir-path)
          no-files (count files)]
      (bulk-upload/db-and-image org-name user-email
                           (take no-files files)))))

(defn -main [& args]
  (System/setProperty "nomad.env" (first args))
  (get-data (second args) (nth args 2) (nth args 3)))
