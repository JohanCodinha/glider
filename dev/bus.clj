(ns bus
  (:require [manifold.stream :as s]
            [manifold.bus :as b]))
(comment
  (def bus (b/event-bus))

  (def p (b/publish! bus :fetched {:data (rand-int 100)}))

  (dotimes [b 100] (b/publish! bus :fetched {:data b}))

  (b/active? bus :fetched)

  (def st (b/subscribe bus :fetched))

  (def v (s/take! s))

  @v
  @(s/try-take! s ::drained 1000 ::timeout)

  (s/consume (fn [x] (println x)) st))
