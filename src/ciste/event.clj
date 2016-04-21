(ns ciste.event
  (:require [clojurewerkz.eep.emitter :as e]
            [slingshot.slingshot :refer [throw+]]))

(defonce ^:dynamic *keys* (ref {}))

(defonce emitter
  (e/create {:dispatcher-type :ring-buffer}))

(defn notify
  [channel-key msg]
  ;; TODO: make sure that the channel is reqistered
  (if-let [d (get @*keys* channel-key)]
    (e/notify emitter channel-key (assoc msg :event channel-key))
    (throw+ {:msg (str "No description for key: " channel-key)})))

(defn defkey
  [channel-key desc & {:as body}]
  (dosync
   (alter *keys* assoc channel-key (assoc body :desc desc))))
