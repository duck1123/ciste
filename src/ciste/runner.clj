(ns ciste.runner
  "This is the runner for ciste applications.

  Specify this namespace as the main class of your application."
  (:require [ciste.config :refer [default-site-config load-site-config]]
            [ciste.service :as service]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre])
  (:gen-class))

(defonce application-promise (ref nil))

(defn stop-application!
  []
  (timbre/info "Stopping application")
  (service/stop-services!)
  (deliver @application-promise true))

(defn configure-logging
  [environment]
  (when-let [logger (some-> @default-site-config :logger symbol)]
    (require logger)
    ((ns-resolve logger 'set-logger))))

(defn start-application!
  ([]
   (start-application! (env :ciste-env "default")))
  ([environment]
   (load-site-config)
   (configure-logging environment)
   (timbre/with-context {:env environment}
     (timbre/infof "Starting application with environment: %s" environment))
   (service/init-services environment)
   ;; (service/start-services!)
   (dosync (ref-set application-promise (promise)))
   (timbre/info "application initialized")
   @application-promise))

(defn -main
  "Main entry point for ciste applications.

   Specify this function as you application's entry point."
  [& options]
  @(start-application!))
