(ns ciste.initializer
  (:require [ciste.config :refer [environment*]]
            [clojure.string :as string]
            [taoensso.timbre :as timbre]))


(defonce ^:dynamic *initializers* (ref []))
(defonce ^:dynamic *initializer-order* (ref []))

(defn add-initializer
  [init-ns init-fn]
  (timbre/debugf "adding initializer - %s" init-ns)
  (dosync
   (alter *initializers* conj [init-ns init-fn]))
  (try
    (when (environment*) (init-fn))
    (catch RuntimeException ex
      (timbre/error ex "Error running initializer")
      (System/exit -1))))

(defmacro definitializer
  "Defines an initializer. When an environment is bound, the initializers will
  be run in the order that they are loaded.

  Initializers are blocks of code that need to set up the environment of
  the namespace, but cannot run until the configuration system is
  available with a valid environment.

  Whenever the environment is changed, the initializers will run in the
  order they were declared.

  Note: At this time, Initializers will be re-run if the namespace is
  reloaded. For this reason, it is recommended that initializers be able
  to handle being run multiple times gracfully.

  Example:

    (ns ciste.example
      (:use [ciste.config :only (definitializer)]))

    (definitializer
      (println \"This will be run when the environment is set\")
      (println (config :hostname)))

    (println \"out of the initializer\"


    > (use 'ciste.example)
    out of the initializer
    > (set-environment! :development)
    This will be run when the environment is set
    server1.example.com"
  [& body]
  `(let [init-fn# (fn [] ~@body)]
     (add-initializer *ns* init-fn#)))

(defn run-initializers!
  "Run all initializers"
  []
  (timbre/debug "running initializers")
  (doseq [[init-ns init-fn] @*initializers*]
    (timbre/debugf "running initializer - %s" init-ns)
    (init-fn)))
