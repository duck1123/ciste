# Ciste ("she steh")

[![Build Status](http://build.jiksnu.org/job/ciste/branch/develop/badge/icon)](http://build.jiksnu.org/job/ciste/branch/develop/)

Ciste is a Lojban word meaning:

x1 (mass) is a system interrelated by structure x2 among components x3
(set) displaying x4 (ka).

-- http://vlasisku.lojban.org/ciste

## About Ciste

Ciste attempts to provide a structure to Clojure applications.

Ciste is a complete framework for building modular Clojure
applications and servers. Ciste contains an app runner, a service
initializer, an environment-aware configuration system, an extensible
request routing framework, a MVC-like page building mechanism, a
dynamic record transformation tool for displaying your resources in
multiple response formats, an asynchronous trigger system, support for
running and managing independent worker threads, and command system
for controlling aspects of the system from remote clients.

## Installation

Add the following to your project.clj dependencies

[![Clojars Project](http://clojars.org/ciste/latest-version.svg)](http://clojars.org/ciste)

## Running a Ciste Application

Ciste comes with a -main function specially designed to bootstrap your
application and start any required services. Simply add the following
to your project.clj file.

    :main ciste.runner

## Configuration

There are 2 files that must be present in order to properly configure
your Ciste application: ciste.clj and config.clj. These files should
be located at the root of your project.

### ciste.clj

ciste.clj (known as the site config) contains only configuration
needed during the initial startup of the application. This file should
contain only a properly formatted Clojure hash-map. The site config
should contain configuration options that are strictly static for
every environment.

There are currently 3 properties defined, but you can add your own.

* :environment - This should contain a keyword specifying the default
  environment of the application. This is the environment that will be
  run if not otherwise overridden.

* :modules - A list of modules to load

* :services - A list of services to run

### config.clj

config.clj is also a file containing a Clojure hash-map. Each key in
the map specifies a different environment, and the values are another
hash containing all the config for the application.

## Modules and Services

When Ciste is starting the application, is looks for :modules and
:services keys in both the config file and the site config. Each of
these keys should have a vector of strings. The loader service will
require each namespace.

Modules allow you to create namespaces that change the state of the
system in some way, but do not need to be required directly by the
rest of your namespaces. Ciste makes use of several different
multimethods and protocols. Implementations can be stored in a module
and easily included into an application.

Services are similar to Modules, except their start and stop functions
will be invoked at the proper point in the application's lifecycle.

## Actions

Actions for the core API of your application. Any major state change
should happen through an Action. Anything that you might want to log
(if action logging is enabled), or anything you might want callbacks
should be run for should be an Action. In the role of Actions with
respect to routing, a traditional function may be substituted.

Action should only have a single arity.

## Routing

Ciste's routing system allows any request map (usually a ring request,
but any map works) to be matched against a sequence of predicates and
match info maps and return a response map that can be invoked to
generate a full response.

The router will thread the request through each of the predicates
applying them against the match info providing parameters for the
predicates. The first route to return a non-nil response will be
invoked.

The predicates and predicate list can be adapted to any type of
request.

## Serialization

Each instance of a router should define a serialization type with
with-serialization (specified as a keyword). The serialization type
identifies the type of request coming in (used by filters) and to
determine a final transformation of returned responses.

## Filters

When a route is selected, it is first passed to the Action's
filter. The filter is a serialization-specific wrapper around the
action. The filter is where the request should be parsed, normalized,
and passed to the action returning its response.

## Formats

Ciste is designed to make it easy to serve resources in multiple
format. Each router should specify a default format for the interface
as a keyword (eg. :html) as well as allow it to be set by route
predicates. Formats are used by Views and Sections as well as the
formatter as the penultimate step in request processing.

Formatters allow you to deal in easy to manipulate data structures
throughout your request, deferring the final transformation till the
very end. A formatter might convert a hiccup structure into html or
convert a clojure map into a json string or whatever needs to be done
for responses of that type.

## Views

After the Filter has processed the action and returned the result, it
is passed to a view for the given Action and Format. The View returns
a map containing parameters for the generation of the response. The
:body key of the response should contain the data that should be
inserted into the main part of the template and converted by the
formatter, but may also contain any other key needed to control the
eventual output.

If the View contains the pair :template false, templating will be
skipped for this response. This is useful for simple responses, such
as redirects or serialization-specific error responses.

## Models

Ciste is designed to work best with Records or any type of object
where dispatch can function off the class name. There are few
functions that directly support this at the moment. An abstraction
around using records against various backends is in the works.

## Sections

Sections allow records to be formatted properly according to their
context. Calling a section function with a Record will apply the
appropriate multimethod for the currently bound Serialization and
Format. This allows you to easily do things like display a sequence of
records, display a single record, get the context-appropriate title
for that record or link to a record using a common language without
needing to define distinct names for each of the many formats you
support.

## Templates

Unless templating has been disabled by the view, a template method
will be applied to the response for the given Serialization type. A
Template will generally use many different keys from the view to build
the page, but will primarily read from the :body key

## Workers

Workers are functions that are identified by a keyword name. Workers
can be started and told to stop. (Workers can check if they've been
asked to stop.) The lists of available and running workers can be
queried and manipulated. Workers can update a per-worker counter
allowing metrics (number of packets processed) to be displayed.

Note: This section is in need of work and will most likely be
re-written to be simply a wrapper over a more robust thread manager.


## Commands

Commands are a special route map and serialization type. Commands
allow simple string names to be mapped to actions. Commands exist to
work with services that provide an interface to the command system.

## Modularity

The goal of Ciste is to make each part as optional as possible
allowing parts to be used without incurring the cost of the whole
system.

In addition, there are many supplemental libraries that provide new
services, custom formatters, and other tools that work with Ciste. The
intention is that you should be able to build up a base application by
combining together whatever pieces you need.

## Maturity

While Ciste has been around for quite some time, I have not been able
to devote as much time to it as I would like. Ciste has formed the
basis of a few highly complex applications, but hasn't been
extensively tested by others. If you find Ciste useful to your needs,
please let me know so I can take care to limit breaking changes.

## In the wild

The prime example of Ciste in action is my OStatus server located at
https://github.com/duck1123/jiksnu

Jiksnu has been the primary driver of new features in Ciste and is the
best example of a fully functioning Ciste application.


## License

Copyright (C) 2011 KRONK Ltd.

Distributed under the Eclipse Public License, the same as Clojure.
