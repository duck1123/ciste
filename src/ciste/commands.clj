(ns ciste.commands
  (:require [ciste.filters :refer [deffilter]]
            [ciste.predicates :as pred]
            [ciste.routes :refer [resolve-routes]]
            [ciste.views :refer [defview]]
            [clojure.string :as string]
            [taoensso.timbre :as timbre]))

(defonce
  ^{:dynamic true
    :doc "The sequence of commands that have been registered."}
  *commands*
  (ref {}))

(defonce
  ^{:dynamic true
    :doc "The sequence of predicates used for command dispatch.
          By default, commands are dispatched by name."}
  *command-predicates*
  (ref [#'pred/name-matches?]))

(defn add-command!
  "Adds the fn identified by var v as the command handler for the given name."
  [command-name v]
  (timbre/with-context {:name command-name}
    (timbre/debugf "Registering command - %s" command-name))
  (dosync
   (alter *commands* assoc command-name v)))

(defn parse-command
  "Takes a sequence of key/value pairs and runs a command"
  [{command-name :name :keys [args] :as command}]
  (timbre/with-context {:name command-name :args (string/join " " args)}
    (timbre/infof "parsing command - %s" command-name))
  (let [f (->> @*commands*
               (map (fn [[k v]] [{:name k} {:action v
                                            :format :clj}]))
               (resolve-routes @*command-predicates*))]
    (f (merge {:serialization :command} command))))

;; Should this return a set?
(defn command-names
  "The names of all the registered commands."
  []
  (sort (keys @*commands*)))

(add-command! "ciste.commands.list" #'command-names)

(deffilter #'command-names :command
  [action request]
  (action))

(defview #'command-names :text
  [request names]
  (string/join "\n" names))

(defview #'command-names :json
  [request names]
  {:body names})
