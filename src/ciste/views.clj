(ns ciste.views
  "A View is a pair of multi-methods: apply-view, and default-format. The
  apply-view method dispatches on a vector containing the Action and the
  Format. If no match is found this value, then default-format tries
  using only Format.

  A View accepts two parameters: the request, and the response from
  invoking the action. A View should render the supplied data into a
  structure appropriate to the Format. It is not required, but this is
  most commonly a map.

  Example:

    (defview #'show :html
      [request user]
      {:status 200
       :body [:div.user
               [:p (:name user)]]})"
  (:require [ciste.core :refer [*format*]]
            [ciste.event :refer [defkey notify]]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.timbre :as timbre]))

(defn view-dispatch
  [{:keys [action]} & _]
  [action *format*])

(defmulti
  ^{:doc "Return a transformed response map for the action and format"}
  apply-view view-dispatch)

(defmulti
  ^{:doc "Fallback view for when no view can be found for the action"}
  apply-view-by-format (fn [& _] *format*))

(defkey ::view-run
  "views that are run")

(defmacro defview
  "Define a view for the action with the specified format"
  [action format binding-form & body]
  `(defmethod ciste.views/apply-view [~action ~format]
     [& args#]
     (let [~binding-form args#]
       (notify ::view-run
               {:request (first args#)
                :response (rest args#)})
       ~@body)))

(defmethod apply-view :default
  [{:keys [action serialization format] :as request} & args]
  ;; (timbre/with-serialization {:action action :serialization serialization :format format}
  ;;   (timbre/debugf "Running default view. action: %s. serialization: %s. format: %s"
  ;;                  action serialization format))
  (apply apply-view-by-format request args))

(defmethod apply-view-by-format :default
  [{:keys [action format] :as request} response]
  ;; (timbre/with-format {:action action :format format}
  ;;   (timbre/debugf "Running default view by format %s %s" action format))
  (throw+
   {:message "No view defined"
    :action (:action request)
    :format *format*}))
