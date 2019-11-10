clojure -Sdeps '{:deps {reply {:mvn/version 0.4.3}}}' -m reply.main --attach localhost:$(echo .nrepl-port)
