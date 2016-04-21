(ns ciste.filters
  "Filters are methods of the multimethod apply-filter. A Filter
  dispatches on the Action and the Serialization. A Filter takes 2
  arguments: The Action, and the request map.

  It is the job of the Filter to parse the request map and produce the
  options to be passed to Action. The Filter must call that action with
  the appropriate arguments.

  While it is possible to modify the response from the Action, it is
  recommended that filters not modify responses. (That would belong in
  the view.)

  Example:

    (defaction login
      [username password]
      ;; Perform authentication
      )

    (deffilter #'login :http
      [action request]
      (let [{{:keys [username password]} :params}]
        (action username password)))"
  (:require [ciste.core :refer [*serialization*]]
            [ciste.event :refer [defkey notify]]
            [taoensso.timbre :as timbre]))

(defn filter-action-dispatch
  "Dispatch function for filter-action.

  Returns a vector containing the Action (the first param) and the
  :serialization key from the second param (The request object)."
  [action request]
  [action *serialization*])

(defmulti filter-action filter-action-dispatch)

(defmethod filter-action :default
  [action {:keys [serialization] :as request}]
  ;; (timbre/with-context {:action action :serialization serialization}
  ;;   (timbre/debugf "Running default filter %s %s" action serialization))
  (action request))

(defkey ::filter-run
  "All filters that have been run")

;; TODO: fall back to filtering on just the serialization
(defmacro deffilter
  "Define a filter for the Action for the given serialization type.

   When a route is being resolved, the filter will be called instead of the
   action.

   It is the job of the filter to deserialize the request object and call the
   provided action function with the required arguments."
  [action serialization binding-form & body]
  `(defmethod ciste.filters/filter-action [~action ~serialization]
     [& args#]
     (notify ::filter-run {:args (rest args#)})
     (let [~binding-form args#]
       ~@body)))
