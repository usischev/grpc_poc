# grpc_poc

Reproducing a maxConnectionAge bug in grpc-java. Run `main()` in `Main.kt` (or `./gradlew run` from command line).
Exception is thrown not on every run but pretty often. You can comment the line calling `blockingRequestCycle` and
uncomment one of `asyncRequestCycle` or `requestCycle`. Any of these three functions eventually throw `StatusException`,
and `asyncRequestCycle` seems to do it more often.

When GRPC server closes a connection because of max connection age, it sends `GOAWAY` HTTP2 packages to connected
clients and stops accepting new requests. Then it finishes processing requests it has already received, then sends
second `GOAWAY` packages to clients.

We found out that when a GRPC client is processing a large request (size of request or response body is several MB),
the first `GOAWAY` from the server causes `io.grpc.StatusException: UNAVAILABLE`, and the request fails, even though
the server processed the request successfully.

* [Stacktrace with coroutineStub and coroutineServer](logs/coroutine_stub_error.log)
* [Stacktrace with blockingStub and coroutineServer](logs/blocking_stub_error.log)
* [Stacktrace with blockingStub and blockingServer](logs/blocking_stub_and_server_error.log)
