(ns ciste.model
  (:require [ciste.config :refer [config]]
            [clj-http.client :as client]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [taoensso.timbre :as timbre]
            [net.cgrand.enlive-html :as enlive])
  (:import java.io.InputStream
           java.io.StringReader
           nu.xom.Builder
           nu.xom.Document
           nu.xom.Element
           nu.xom.Node
           nu.xom.XPathContext
           org.xml.sax.InputSource))

;; TODO: find a better place
(defmacro implement
  "Throws an exception saying that this function has not been implemented"
  ([]
   `(throw (UnsupportedOperationException. "Not implemented yet")))
  ([& body]
   `(do
      (timbre/warn "Not implemented yet")
      ~@body)))

(defn string->zip
  "parse xml string as a zipper sequence"
  [^String xml]
  (-> xml StringReader. InputSource.
      xml/parse zip/xml-zip))

(defn ^Document stream->document
  "Read an input stream into a XOM document"
  [^InputStream input-stream]
  (.build (Builder.) input-stream))

(defn ^Document string->document
  "Read a string into a XOM document"
  [^String xml]
  (.build (Builder.) xml ""))

(defn ^String fetch-resource
  "Fetch the url, return the string"
  [^String url]
  (timbre/debugf "fetching resource: %s" url)
  (if-let [response (client/get url)]
    (let [{:keys [body status]} response]
      (when (not (#{404 500} status))
        body))))

(defn ^Document fetch-document
  "Fetch the url and return it as a XOM document"
  [^String url]
  (some-> url fetch-resource string->document))

;; TODO: reverse order
(defn query
  "Return the sequence of nodes that match the xpath expression"
  [^Element doc ^String path & [context]]
  (let [xc (XPathContext/makeNamespaceContext doc)]
    (doseq [[prefix uri] context]
      (.addNamespace xc prefix uri))
    (let [nodes (.query doc path xc)]
      (map
       #(.get nodes %)
       (range (.size nodes))))))

(defn get-links
  [url]
  (-> url
      fetch-resource
      StringReader.
      enlive/html-resource
      (enlive/select [:link])))
