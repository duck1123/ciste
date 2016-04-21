(ns ciste.sections-test
  (:require [ciste.core :refer [with-context]]
            [ciste.sections :refer [record-class-serialization]]
            [ciste.test-helper :refer [test-environment-fixture]]
            [midje.sweet :refer [fact =>]]))

(defrecord User [])

(test-environment-fixture

 (fact "record-class-serialization"
   (let [record (User.)
         format :html
         serialization :http]
     (with-context [serialization format]
       (record-class-serialization record)) => [User format serialization])))
