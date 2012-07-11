(ns ciste.test-helper
  (:use [ciste.runner :only [load-site-config start-application!
                             stop-application!]]))

(defmacro test-environment-fixture
  "Wrapper to ensure tests are run in the test environment"
  [& body]
  `(do
     (println "****************************************************************************")
     (println (str "Testing " *ns*))
     (println "****************************************************************************")
     (println " ")

     (load-site-config)
     (start-application! :test)
     ~@body
     (stop-application!)))