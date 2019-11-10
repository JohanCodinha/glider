source ./secrets
clojure -A:nrepl | (grep -m 1 "nREPL server" ; sleep .5s ; echo $(cat .nrepl-port) ; clojure -A:reply --attach localhost:$(cat .nrepl-port) < /dev/tty)

