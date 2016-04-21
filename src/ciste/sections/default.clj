(ns ciste.sections.default
  (:require [ciste.sections :refer [declare-section defsection]]
            [inflections.core :refer [plural underscore]]))

(declare-section link-to)
(declare-section full-uri)
(declare-section uri)
(declare-section title)

(declare-section delete-button)
(declare-section edit-button)
(declare-section update-button)

(declare-section actions-section)


(declare-section add-form)
(declare-section edit-form)

(declare-section show-section)
(declare-section index-line)
(declare-section index-block :seq)
(declare-section index-section :seq)

(declare-section show-section-minimal)
(declare-section index-line-minimal)
(declare-section index-block-minimal :seq)
(declare-section index-section-minimal :seq)


(defsection title :default
  [record & options]
  (pr-str record))

(defsection uri :default
  [record & options]
  (if-let [segment (if-let [model-name (class record)]
                     (plural (underscore (.getSimpleName model-name))))]
    (format "/%s/%s" segment (or (:_id record)
                                 (:id record)))))

(defsection show-section :default
  [record & options]
  (title record))

(defsection index-line :default
  [record & options]
  (show-section record))

(defsection index-block :default
  [records & [options & _]]
  (map #(index-line % options)
       records))

(defsection index-section :default
  [records & [options & _]]
  (index-block records options))

(defsection show-section-minimal :default
  [record & _]
  (title record))
