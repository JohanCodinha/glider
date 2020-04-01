(ns glider.commands-handler
  (:require [glider.domains.observation :as observation]))

(defmulti handler :name)

#_(defmethod handler :glider.commands/publish-observation
  [command]
  (let [[errors aggregate-events] (observation/aggregate-root events command)] 
    (publish-to-store aggregate-events)
    (publish-to-bus aggregate-events)))

; handle command
; setup aggregate root
; generate new state
; return response accept or refuse
; persist state
; publish event trigering view model rebuild
