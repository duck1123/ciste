(ns ciste.test-helper
  (:use [ciste.config :only [load-site-config]]
        [ciste.runner :only [start-application! stop-application!]]))

(defmacro test-environment-fixture
  "Wrapper to ensure tests are run in the test environment"
  [& body]
  `(do
     (start-application! :test)
     ~@body
     (stop-application!)))
