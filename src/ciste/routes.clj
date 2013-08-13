(ns ciste.routes
  "## Routing

Ciste's routing mechanism can be used in any situation where you have
a request that needs to be processed, possibly changing state,
returning a result that is then transformed into a desired output
format, and then either returned or processed in some other fashion.

'resolve-routes' takes 2 parameters: a sequence of predicates, and a
sequence of matcher pairs. A \"handler\" function is then returned that
takes a request map and then returns a response.

When a request is being processed, Ciste will iterate over the
sequence of matchers and apply the predicates. The first
matcher to return a non-nil result will then invoke its action.

A matcher pair is a sequence containing 2 maps. The first map contains
data that will be used by the predicates to determine if the request
is valid for the matcher. The section map contains information that
will be used if the matcher is selected.

The predicate sequence is a list of predicate functions. Each function
takes the matcher data as the first argument and the request as the
second. Each predicate will perform some test, possibly using data
contained in the matcher map as its arguments. If the predicate
passes, it returns a map containing the new request map for the next
step in the chain. Usually the request is simply returned unmodified.

## Invoking an Action

When a Ciste route is matched, invoke-action will perform a series of
steps, ultimately returning the final result.

First, the Filter is called. The Filter will extract all of the
necessary parameters from the serialization-specific request and call
the serialization-agnostic Action. The Action will produce a result,
which is then returned by the Filter.

Next, the request map and the returned data are passed to the View
function. Views are specific to the Format in use. The View will
transform the response data to a format acceptable to the downstream
Serializer.

With the response data transformed into a format-specific view, a
template is then called, if enabled. This will attach any additional
markup or perform any processing that is done to every request using
the same format that specifies that a template be used.

The next stage is to call the Formatter. This is the last stage that
is specific to the format. This is where any intermediate data
structures are converted to types that can be used by
serializers. Steps such as converting Hiccup vectors to strings should
be done here.

Finally, the Serializer performs a last stage transform specific to
the Serialization type. Place things that need to apply to every
request here. If Ciste is being used in a Ring application, there is
no need to perform any IO, and the map can simply be returned. It is
possible to write Serializers that will respond to a request by
transmitting the response in any number of ways. (XMPP, Email,
Filesystem, etc.)"
  (:use [ciste.config :only [config]]
        [ciste.core :only [with-context apply-template serialize-as *serialization*]]
        [ciste.filters :only [filter-action]]
        [clojure.core.incubator :only [-?> -?>>]])
  (:require [ciste.formats :as formats]
            [ciste.views :as views]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [lamina.trace :as trace]
            [slingshot.slingshot :refer [throw+]]))

(defn escape-route
  [path]
  (string/replace path #":" "\\:"))

(defn lazier
  "This ensures that the lazy-seq will not be chunked

Contributed via dnolan on IRC."
  [coll]
  (when-let [s (seq coll)]
    (lazy-seq (cons (first s) (lazier (next s))))))

(defn make-matchers
  [handlers]
  (map
   (fn [[matcher action]]
     (let [[method route] matcher]
       [{:method method
         :format :http
         :serialization :http
         :path route}
        {:action action
         :serialization :http
         :format :html}]))
   handlers))

(defn try-predicate
  "Tests if the request and matcher info matches the predicate.

If the predicate is a sequence, test the first first element. If that test
succeeds, continue with the remainder of the sequence.

If the predicate is a function, apply that function against the request and
matcher.

If the request is still non-nil after walking the entire predicate sequence,
then the route is considered to have passed."
  [request matcher predicate]
  (if predicate
    (if (coll? predicate)
      (if (empty? predicate)
        request
        (if-let [request (try-predicate request matcher (first predicate))]
          (recur request matcher (rest predicate))))
      (when (ifn? predicate)
        (trace/trace :ciste:predicate:tested [predicate request matcher])
        (predicate request matcher)))
    (throw+ "Predicate nil" )))

(defn try-predicates
  "Tests if the request and the matcher info matches the provided predicates.

Returns either a (possibly modified) request map if successful, or nil."
  [request matcher predicates]
  (trace/trace :ciste:matcher:tested [matcher predicates request])
  (->> predicates
       lazier
       (map (partial try-predicate request matcher))
       (filter identity)
       first))

(defn invoke-action
  "Renders the given action against the request"
  [request]
  (log/debug "invoking action")
  (let [{:keys [format serialization action]} request]
    (trace/trace :ciste:action:invoked [action request])
    (with-context [serialization format]
      (-?>> request
            (filter-action action)
            (views/apply-view request)
            (apply-template request)
            (formats/format-as format request)
            (serialize-as *serialization*)))))

(defn resolve-route
  "If the route matches the predicates, invoke the action"
  [predicates [matcher {:keys [action format serialization] :as res}] request]
  (if-let [request (try-predicates request matcher (lazier predicates))]
    (do
      (log/debug "match found")
      (let [format (or (:format request)
                       (-?> request :params :format keyword)
                       format)
            serialization (or (:serialization request)
                              serialization)
            request (merge request res
                           {:action action
                            :format format
                            :serialization serialization})]
        (invoke-action request)))))

(defn resolve-routes
  "Returns a handler fn that will match each route against
the predicate sequence and return the result of the invoking the
first match."
  [predicates routes]
  (log/debug "resolving routes")
  (fn [request]
    (log/debug "processing request")
    (->> routes
         lazier
         (map #(resolve-route predicates % request))
         (filter identity)
         first)))
