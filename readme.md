# Ciste ("she steh")

[![Build Status](https://secure.travis-ci.org/duck1123/ciste.png)](http://travis-ci.org/duck1123/ciste)

Ciste is a Lojban word meaning:

x1 (mass) is a system interrelated by structure x2 among components x3
(set) displaying x4 (ka).

-- http://vlasisku.lojban.org/ciste

## About Ciste

Ciste attempts to provide a structure to Clojure applications. The
main idea revolves around matching a request to a action variable and
a pair of format and serialization parameters.

The action is simply a function that accepts a request and returns
either: a record of some sort (or a sequence of them); or nil.

The response of the action method is then passed to the apply-view
family of multimethods. The view will match the combination of action
(expressed as a var) and the format (a keyword). The view will return
a logically true value (generally a hash). If there is no view defined
for both the action and the format, a view will be searched for using
only the format. This allows generic handlers to be defined for a
format (eg, converting the structure to JSON).

The response from the view is then passed to the formatter. This
allows extra transformations to be applied for the given format. An
example of this would be converting a series of vectors into HTML
using Hiccup.

Finally, the response is formatted according to the serialization
type. This framework has been used to respond to request
simultaneously over an HTTP and XMPP connection. This allows any final
transformations to be applied (eg, adding common headers).

In addition to the main thread of request and response, triggers can
also be defined. When a matching action is invoked, the request and
the response from the action are sent to the specified
trigger. Triggers are for side effects only.

### Example

``` clojure
(ns my-app.core
  (:use ciste.core))

(defaction index
  [options]
  (index-db-method :limit (:limit options)))

(deffilter #'index :html
  [action request]
  (action {:limit (-> request :params :limit)}))

(defview #'index :html
  [records]
  {:body (show-list records)})

(defsection show-list [RecordModel]
  [records]
  [:ul (map show-list-item records)])
```

## Routing

Ciste's routing mechanism can be used in any situation where you have
a request that needs to be processed, possibly changing state,
returning a result that is then transformed into a desired output
format, and then either returned or processed in some other fashion.

'resolve-routes' takes 2 parameters: a sequence of predicates, and a
sequence of matcher pairs. A "handler" function is then returned that
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
Filesystem, etc.) 

## Actions

Any fundamental state changes in your application should take place
through an action. Any time you create, update, or delete a resource,
you should use an action. Actions are analogous to the Controller in a
traditional MVC design.

When an action is executed, if the config path [:print :actions] is
enabled, then the action will be logged.

Actions are simply functions. An Action can take any number of
parameters and should return any logically true value if the action
succeeded.

## Filters

Filters are methods of the multimethod apply-filter. A Filter
dispatches on the Action and the Serialization. A Filter takes 2
arguments: The Action, and the request map.

It is the job of the Filter to parse the request map and produce the
options to be passed to Action. The Filter must call that action with
the appropriate arguments.

While it is possible to modify the response from the Action, it is
recommended that filters not modify responses. (That would belong in
the view.)

### Example

``` clojure
(defaction login
  [username password]
  ;; Perform authentication
  )

(deffilter #'login :http
  [action request]
  (let [{{:keys [username password]} :params}]
    (action username password)))
```

## Views

A View is a pair of multi-methods: apply-view, and default-format. The
apply-view method dispatches on a vector containing the Action and the
Format. If no match is found this value, then default-format tries
using only Format.

A View accepts two parameters: the request, and the response from
invoking the action. A View should render the supplied data into a
structure appropriate to the Format. It is not required, but this is
most commonly a map.

### Example

``` clojure
(defaction show
  [id]
  (fetch-user id))

(deffilter #'show :http
  [action {{id :id} :params}]
  (action id))

(defview #'show :html
  [request user]
  {:status 200
   :body [:div.user
           [:p (:name user)]]})
```

## Config

Ciste uses the config function in ciste.config to perform all the
configuration. Config takes a variable number of key values and will
either return a non-nil value if that option is defined, or will raise
an exception if it is not.

The config information is read from the file "config.clj" at the base
of the project's directory. The config file should contain a hash-map.

The top-level keys will be the names of environments. The values of
these keys will be an arbitrarily complex structure of hashes,
vectors, and other data.

### Example

config.clj

``` clojure
{:default {:option1 "foo"
           :option2 {:value "bar" :title "BAR"}
           :option3 ["foo" "bar" "baz"]}}

> (use 'ciste.config)
> (load-config)
> (set-environment! :default)
> (config :option1) => "foo"
> (config :option3) => ["foo" "bar" "baz"]
> (config :option2 :title) => "BAR"
```

## Initializers

Initializers are blocks of code that need to set up the environment of
the namespace, but cannot run until the configuration system is
available with a valid environment.

Whenever the environment is changed, the initializers will run in the
order they were declared.

Note: At this time, Initializers will be re-run if the namespace is
reloaded. For this reason, it is recommended that initializers be able
to handle being run multiple times gracfully.

### Example

``` clojure
(ns ciste.example
  (:use [ciste.config :only (definitializer)]))

(definitializer
  (println "This will be run when the environment is set")
  (println (config :hostname)))

(println "out of the initializer"


> (use 'ciste.example)
out of the initializer
> (set-environment! :development)
This will be run when the environment is set
server1.example.com
```

## Sections

Sections are a series of multimethods for generically transforming
records into the most appropriate format.

A Section dispatches on a Vector containing the type of the first
argument or the type of the first element of the first argument if the
Section has been defined as a :seq type, the Format, and
the Serialization. If no match is found, the final value is removed
and tried again. This repeats until there is only the type.

### Example

``` clojure
(declare-section show-section)
(declare-section index-section :seq)

(defsection show-section [User :html :http]
  [user & options]
  [:div
    [:p "Name: " (:name user)]
    [:p "Email: " (:email user)]])

(defsection index-section [User :html :http]
  [users & options]
  [:ul
    (map
      (fn [user]
        [:li (show-section user)])
      users)])
```

## Triggers

Triggers allow you to have functions called as part of a seperate
thread pool whenever a matching action is invoked.

A Trigger is a function that takes 3 arguments: The action, the
request map, and the response from invoking the action.

All of the dynamic bindings from the original request are coppied to
the trigger.

### Example

``` clojure
(defaction my-action
  [request]
  {:foo 23, :bar 42})

(defn my-trigger
  [action request record]
  "Do something in a different thread")

(ciste.trigger/add-trigger! #'my-action #'my-trigger)
```

## Workers

Workers are tasks that will repeatedly run. A worker can be started
and stopped by any thread. When a worker is stopped, it will continue
until the next time that it exits. You can check if it's stopping
within your code if you wish to exit earlier.

### Example

``` clojure
(defworker :queue-checker
  [queue-name]
  (check-and-process-queue queue-name))

(start-worker! :queue-checker)
(stop-worker! :queue-checker worker-id)
(stop-all-workers!)
```

## Debug

The ciste.debug namespace contains only a single macro: spy. Spy will
log the code it wraps as well as a pretty-printed version of it's
value. That value will then be returned. This allows you to easily
monitor any bit of code by simply wrapping it with spy.

## Installation

Add the following to your project.clj dependencies

    [ciste "0.2.0-SNAPSHOT"]

## License

Copyright (C) 2011 KRONK Ltd.

Distributed under the Eclipse Public License, the same as Clojure.
