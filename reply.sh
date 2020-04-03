source ./secrets
clojure -A:reply --attach localhost:$(cat .nrepl-port)
