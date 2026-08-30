# Configuration Over Code

# The target service provides services over the HTTP protocol."
carries-http-client - (rsocket carries http) -> carries-server - (http) -> carries-http-server

# The target service receives HTTP data carried over the RSocket protocol.
carries-http-client - (rsocket carries http) -> carries-server - (rsocket carries http) -> carries-http-server

# When invoking RSocket services over HTTP, a gateway typically handles the protocol translation, while inter-service communication uses the same protocol.
carries-http-client - (rsocket carries http) -> carries-server - (rsocket) -> carries-http-server

# All by RSocket
## rpc
consumer -(rsocket) -> carries-server -(rsocket)-> provider
## mq
producer -(rsocket) -> carries-server - (mq) -> mq-server -(mq)-> carries-server -(rsocket)-> consumer

# start
* start
* pull configuration
* send service availability information

