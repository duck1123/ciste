(ns ciste.core-test
  (:use [ciste.core :only [defaction with-format with-serialization
                           *format* *serialization*]]
        [ciste.test-helper :only [test-environment-fixture]]
        [midje.sweet :only [fact =>]]))

(test-environment-fixture

 (fact "with-serialization"
   *serialization* => nil
   (with-serialization :http
     *serialization* => :http)
   *serialization* => nil)

 (fact "with-format"
   *format* => nil
   (with-format :html
     *format* => :html)
   *format* => nil)

 (fact "#'defaction"
   (fact "should define the var"
     (defaction foo
       []
       true)

     (foo) => true))
 )
