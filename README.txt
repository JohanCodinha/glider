clojure -Sdeps '{:deps {reply {:mvn/version 0.4.3}}}' -m reply.main --attach localhost:$(echo .nrepl-port)

I want to list all users from legacy
  Process command "Sync legacy users"
  	  Fetch users
	  If not in system then
 	     Generate new event fact
	     Generate updated event fact
  	  Publish system event	     
  Generate view layer
  	  Build denormalized view
  Serve data via http API
  
