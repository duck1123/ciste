(ns ciste.workers
  "Workers are tasks functions that run on their own thread p for a time,
  sleep, and then run again. Generally, tasks that will repeatedly run. A
  worker can be started and stopped by any thread. When a worker is
  stopped, it will continue until the next time that it exits. You can
  check if it's stopping within your code if you wish to exit earlier.

    (defworker :queue-checker
      [queue-name]
      (check-and-process-queue queue-name))

    (start-worker! :queue-checker) => 1
    (stop-worker! 1) => nil
    (stop-all-workers!) => nil"
  (:require [ciste.config :refer [config describe-config]]
            [clj-factory.core :refer [defseq fseq]]
            [clojure.string :as string]
            [taoensso.timbre :as timbre])
  (:import java.net.InetAddress))

(defonce ^:dynamic *workers* (ref {}))
(defonce ^:dynamic *current-name* nil)
(defonce ^:dynamic *current-id* nil)

(describe-config [:worker-timeout]
  :number
  "The time between loops of workers")

(defseq :id [n] n)

(defmulti execute-worker (fn [x & _] x))

(defn current-id
  []
  (or *current-id*
      (throw
       ;; TODO: find a more appropriate exception
       ;; TODO: better error text
       (RuntimeException. "No id defined"))))

(defn current-worker
  "Returns the worker currently running on this thread"
  []

  )

(defn stopping?
  ([]
   (stopping? (current-id)))
  ([id]
   (-> @*workers*
       (get id)
       :stopping)))

(defn worker-keys
  "Returns a sequence of registered worker types"
  []
  (keys (methods execute-worker)))

(defn- make-worker-fn
  [name id inner-fn]
  (fn worker-fn [name & args]
    (future
      (try
        (binding [*current-id* id]
          ;; TODO: Pull this part out
          (loop []
            (try
              (apply inner-fn name args)
              (catch Exception ex
                (timbre/error ex "Uncaught exception")))
            (if-let [stopping (stopping? id)]
              (timbre/debugf "(stopping? %s %s): %s" name id stopping)
              (do (Thread/sleep (config :worker-timeout))
                  (recur)))))
        (catch Exception ex
          (timbre/error ex "Uncaught exception"))
        (finally
          (timbre/infof "Worker %s (%s) finished" name id)
          (dosync
           (alter *workers* dissoc id)))))))

(defn get-host-name
  "Returns the hostname of the host's local adapter."
  []
  (.getHostName (InetAddress/getLocalHost)))

(defn- start-worker*
  [name id worker-fn args]
  (timbre/infof "Starting worker %s(%s) => %s" name (string/join " " args) id)
  (let [inst (apply worker-fn name args)
        m {:worker inst
           :host (get-host-name)
           :name name
           :counter 0
           :stopping false
           :id id}]
    (dosync (alter *workers* assoc-in [id] m))
    m))

(defn increment-counter!
  ([] (increment-counter! 1))
  ([n] (increment-counter! (current-id) n))
  ([id n]
   (dosync
    (alter *workers*
           (fn [w]
             (let [counter (get-in w [id :counter])]
               (assoc-in w [id :counter] (+ counter n))))))))

(defmacro defworker
  "Define a worker named `name'"
  [name args & body]
  `(let [name# ~name]
     (defmethod execute-worker name#
       [worker-name# & args#]
       (let [id# (fseq :id)
             inner-fn# (fn inner-fn [worker-name# & ~args] ~@body)
             worker-fn# (#'make-worker-fn name# id# inner-fn#)]
         (#'start-worker* name# id# worker-fn# args#)))))

(defn start-worker!
  [worker-name & args]
  (apply execute-worker worker-name args))

(defn stop-worker!
  "Stop the worker with the given name"
  [id]
  (dosync
   (alter *workers* assoc-in [id :stopping] true)))

(defn stop-all-workers!
  "Tell all workers to stop"
  []
  (timbre/info "Stopping all workers")
  (doseq [[id _] @*workers*]
    (stop-worker! id)))
