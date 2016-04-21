(ns ciste.service
  (:use [ciste.config :only [config config* load-config! set-environment!
                             default-site-config]]
        [ciste.initializer :only [run-initializers!]]
        [ciste.loader :only [process-requires require-modules]])
  (:require [environ.core :refer [env]]
            [taoensso.timbre :as timbre]))

(defn start-services!
  "Start each service."
  ([] (start-services! @default-site-config))
  ([site-config]
   (doseq [service-name (concat (:services site-config)
                                (config* :services))]
     (let [service-sym (symbol service-name)]
       (timbre/with-context {:name service-name}
         (timbre/infof "Starting Service - %s" service-name))
       (require service-sym)
       ((intern (the-ns service-sym) (symbol "start")))))))

(defn init-services
  "Ensure that all namespaces for services have been required and that the
   config provider has benn initialized"
  [environment]
  (timbre/info "initializing services")
  ;; TODO: initialize config backend
  (load-config! (env :ciste-properties (str "config/" (name environment) ".properties")))
  (set-environment! environment)
  (require-modules)
  ;; (run-initializers!)
  (process-requires))

(defn stop-services!
  "Shut down all services"
  ([] (stop-services! @default-site-config))
  ([site-config]
   ;; (timbre/debug "stopping services")
   (doseq [service-name (concat (:services site-config)
                                (config* :services))]
     (timbre/with-context {:name service-name}
       (timbre/infof "Stopping %s" service-name))
     ((intern (the-ns (symbol service-name)) (symbol "stop"))))))
