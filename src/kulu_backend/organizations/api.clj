(ns kulu-backend.organizations.api
  (:require [schema.core :as s :refer [defschema optional-key maybe pred enum Str Inst Num Uuid]]
            [ring.swagger.schema :as ring-s :only [describe]]
            [kulu-backend.organizations.model :as org]))

(s/defschema OrganizationID
  (ring-s/field Uuid
                {:description "UUID formatted string"}))

(defschema Organization
  "schema for Organizations at the 'web side' of things"
  {:id Uuid})

(defschema CreateOrganizationBodyParams
  {:name           (maybe Str)
   :user_email     (maybe Str)
   :user_name      (maybe Str)
   (optional-key :team_domain)    (maybe Str)})

(defschema UpdateCategoryParams
  {:organization_name Str
   :name Str})

(defn create
  [m]
  (select-keys (org/create m) [:organization_id]))
