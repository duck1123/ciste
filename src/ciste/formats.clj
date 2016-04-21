(ns ciste.formats
  (:require [ciste.core :refer [*format*]]))

(defmulti format-as (fn [format _handler _request] *format*))
