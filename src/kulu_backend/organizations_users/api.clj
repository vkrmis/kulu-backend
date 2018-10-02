(ns kulu-backend.organizations-users.api
  (:require [kulu-backend.organizations-users.model :as org-users]))

(defn users
  [org-name]
  (org-users/users org-name))
