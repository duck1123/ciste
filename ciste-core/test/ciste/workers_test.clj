(ns ciste.workers-test
  (:use [ciste.config :only [config set-config!]]
        [ciste.test-helper :only [test-environment-fixture]]
        [ciste.workers :only [current-id defworker start-worker! stop-all-workers! stopping?]]
        [midje.sweet :only [fact falsey throws truthy =>]])
  (:require [clojure.tools.logging :as log]))

(test-environment-fixture

 (fact "#'current-id"
   (fact "when called outside of a worker"
     (fact "should throw an exception"
       (current-id) => (throws RuntimeException)))

   (fact "when called from within a worker"
     (fact "should return that worker's id"
       (let [resp (ref nil)]

         (set-config! [:worker-timeout] 5000)
         
         (defworker ::current-id-test []
           (dosync
            (ref-set resp (current-id)))
           (stop-all-workers!))

         
         (let [worker  (start-worker! ::current-id-test)]
           @(:worker worker)
           @resp => (:id worker))))))

 (fact "#'stop-all-workers!"
   (fact "When there all no workers"
     (fact "should simply return"
       (stop-all-workers!) =not=> (throws Exception)))

   (fact "When there are multiple workers"
     (defworker ::stop-all-workers-test
       []
       (log/infof "Running worker #%s" (current-id)))

     (let [workers  (doall
                     (map
                      (fn [n]
                        (start-worker! ::stop-all-workers-test))
                      (range 10)))]
       (doseq [worker workers]
         (stopping? (:id worker)) => falsey)

       (stop-all-workers!)

       (doseq [worker workers]
         (stopping? (:id worker)) => truthy)

       (doseq [worker workers]
         @(:worker worker))))))
