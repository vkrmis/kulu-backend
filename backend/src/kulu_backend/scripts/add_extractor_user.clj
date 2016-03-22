(ns kulu-backend.scripts.add-extractor-user
  (:require [kulu-backend.organizations.model :as org]))

(defn create [email password]
  (org/create {:user-email email
               :password password
               :user-name (str "extractor-" (str (java.util.UUID/randomUUID)))
               :organization-name "transcribers"}))

;;
;; how to run:
;;
;; $ lein run -m kulu-backend.scripts.add-extractor-user dev sam@harris.org chomsky
;;
(defn -main [& args]
  (System/setProperty "nomad.env" (first args))
  (create (second args) (first (rest (rest args))))
  (System/exit 0))
