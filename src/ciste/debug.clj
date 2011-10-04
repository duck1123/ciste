(ns ciste.debug
  (:use (clojure [pprint :only (pprint)]))
  (:require (clojure.tools [logging :as log])))

(defmacro spy
  [sym]
  `(let [value# ~sym]
     (log/info (str ~(str sym) ": "
                    (with-out-str
                      (pprint value#))))
     value#))

(defmacro with-time
  "Evaluates expr and calls f with timing info. Returns the value of expr."
  [f expr]
  `(let [start# (System/nanoTime)
         ret# ~expr
         elapsed# (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
     (~f {:return ret# :elapsed elapsed# :code '~expr})
     ret#))
