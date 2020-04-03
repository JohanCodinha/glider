clojure -A:nrepl | (grep -m 1 "nREPL server" ; sleep .5s ; echo $(cat .nrepl-port))

