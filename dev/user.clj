(ns user
  (:require [glider.system :as system]
            [clojure.repl :refer :all]
            [clojure.tools.namespace.repl :refer [refresh]]
            [integrant.repl :as ig-repl]
            [glider.wrapper.login :as login]))

(ig-repl/set-prep! (fn [] system/system-config))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(comment
  (go)
  (halt)
  (reset)
  (reset-all))

(comment
  (System/getenv "admin_username")
  (require `[glider.wrapper.login :refer :all]))
