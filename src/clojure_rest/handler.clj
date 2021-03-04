(ns clojure-rest.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response redirect]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [clojure.pprint :as pp]
            [environ.core :refer [env]]
            [clojure.data.json :as json]
            [clj-http.client :as client]
  )
)

;; Define a route using compojure
(defn dogs-handler-method
    [name]
    (format "<button>%s is a Good Boi %s</button>", name, (:github-client-id env)))

(defn get-token
  [response]
  ((json/read-str (response :body)) "access_token")
)

(defroutes app-routes
  (GET "/" [token] (resource-response "public/index.html"))

  (GET "/dogs/:name" 
        [name] 
        (dogs-handler-method name))

  (GET "/auth"
        [] 
        (redirect
          (format "https://github.com/login/oauth/authorize?client_id=%s", 
            (:github-client-id env))))

  (GET "/github-oauth-callback" 
        [code]
        (let [
          body (json/write-str {
            :client_id (:github-client-id env)
            :client_secret (:github-secret env)
            :code code
          })]
          (let [
            response (client/post "https://github.com/login/oauth/access_token" {
              :body body
              :content-type :json
              :accept :json
            })]
            (redirect 
              (format "/?token=%s", (get-token response))
            ))))

  (route/not-found "This page doesnt exist!")
)

(def app
  (-> app-routes
      ;; (wrap-defaults site-defaults)
      wrap-keyword-params
      wrap-params
      (wrap-resource "public"))) 
