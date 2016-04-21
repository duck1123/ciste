(ns ciste.sections
  "Sections are a series of multimethods for generically transforming
  records into the most appropriate format.

  A Section dispatches on a Vector containing the type of the first
  argument or the type of the first element of the first argument if the
  Section has been defined as a :seq type, the Format, and
  the Serialization. If no match is found, the final value is removed
  and tried again. This repeats until there is only the type.

  Example:

    (declare-section show-section)
    (declare-section index-section :seq)

    (defsection show-section [User :html :http]
      [user & options]
      [:div
        [:p \"Name: \" (:name user)]
        [:p \"Email: \" (:email user)]])

    (defsection index-section [User :html :http]
      [users & options]
      [:ul
        (map
          (fn [user]
            [:li (show-section user)])
          users)])"
  (:require [ciste.core :refer [*format* *serialization*]]
            [ciste.event :refer [defkey notify]]
            [taoensso.timbre :as timbre]))

(defn record-class
  "Returns the class of the first parameter"
  [record & _]
  [(class record)])

(defn record-class-seq
  "Returns the class of the first element of the first parameter"
  [records & _]
  [(class (first records))])

(defn record-class-serialization
  "Returns the class of the first parameter"
  [record & _]
  [(class record) *format* *serialization*])

(defn record-class-seq-serialization
  "Returns the class of the first element of the first parameter"
  [records & _]
  [(class (first records)) *format* *serialization*])

(defn record-class-format
  "Returns the class of the first parameter"
  [record & _]
  [(class record) *format*])

(defn record-class-seq-format
  "Returns the class of the first element of the first parameter"
  [records & _]
  [(class (first records)) *format*])

(defmacro declare-section
  "Setup a section with the given name"
  [name & opts]
  (let [dispatch-name (if (= (first opts) :seq)
                        "record-class-seq" "record-class" )

        ;; Find a way to make this automatic
        ;; One option would be to capture the ns outside the defmacro,
        ;; creating a closure. I'm not sure if that's bad practice, however.
        dispatch-ns (the-ns 'ciste.sections)

        dispatch-fn            (ns-resolve dispatch-ns
                                           (symbol dispatch-name))
        serialization-dispatch (ns-resolve dispatch-ns
                                           (symbol (str dispatch-name "-serialization")))
        format-dispatch        (ns-resolve dispatch-ns
                                           (symbol (str dispatch-name "-format")))

        serialization-name (symbol (str name "-serialization"))
        format-name        (symbol (str name "-format"))
        type-name          (symbol (str name "-type"))]
    `(do
       (defmulti ~serialization-name ~serialization-dispatch)
       (defmulti ~format-name        ~format-dispatch)
       (defmulti ~type-name          ~dispatch-fn)

       (defn ~name
         [record# & options#]
         (if *format*
           (if *serialization*
             (apply ~serialization-name record# options#)
             (throw (IllegalArgumentException.
                     "serialization not provided and *serialization* not set")))
           (throw (IllegalArgumentException.
                   "format not provided and *format* not set"))))

       (defmethod ~serialization-name :default
         [record# & others#]
         (apply ~format-name record# others#))

       (defmethod ~format-name :default
         [record# & others#]
         (apply ~type-name record# others#)))))

(defn log-section
  [sym dispatch-val]
  (timbre/debugf "%s - %s" dispatch-val sym))

(defkey ::section-run
  "sections run")

(defmacro defsection
  [section-name dispatch-val binding-form & body]
  (if-let [declared-ns (-> section-name resolve meta :ns)]
    (let [bare-var           (ns-resolve declared-ns (symbol section-name))
          type-name          (symbol (str section-name "-type"))
          format-name        (symbol (str section-name "-format"))
          serialization-name (symbol (str section-name "-serialization"))
          method-name (if (= dispatch-val :default)
                        type-name
                        (condp = (count dispatch-val)
                          3 serialization-name
                          2 format-name
                          type-name))
          full-symbol (symbol (str declared-ns "/" method-name))]
      `(defmethod ~full-symbol ~dispatch-val
         [& args#]
         (let [~binding-form args#]
           (notify ::section-run
                   {:section  ~bare-var
                    :dispatch ~dispatch-val
                    :args     args#})
           ~@body)))
    (throw (IllegalArgumentException. (str "Can not resolve section: " name)))))
