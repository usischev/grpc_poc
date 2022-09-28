package org.example.grpc.poc

import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.GreeterGrpcKt
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.HelloResponse
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

internal fun buildHelloResponse(name: String) = HelloResponse
    .newBuilder()
    .setMessage("hello, $name".repeat(1024 * 1024 * 5).substring(0, 4194000))
    .build()

internal class HelloWorldCoroutineImpl : GreeterGrpcKt.GreeterCoroutineImplBase() {
    override suspend fun sayHello(request: HelloRequest): HelloResponse {
        return coroutineScope {
            buildHelloResponse(request.name)
        }
    }
}

internal class HelloWorldBlockingImpl : GreeterGrpc.GreeterImplBase() {
    override fun sayHello(request: HelloRequest, responseObserver: StreamObserver<HelloResponse>) {
        responseObserver.onNext(buildHelloResponse(request.name))
        responseObserver.onCompleted()
    }
}

internal fun createCoroutineServer(port: Int): Server {
    return NettyServerBuilder.forPort(port)
        .maxConnectionAge(1, TimeUnit.SECONDS)
        .maxConnectionAgeGrace(300, TimeUnit.SECONDS)
        .addService(HelloWorldCoroutineImpl())
        .build()
}

internal fun createBlockingServer(port: Int): Server {
    return NettyServerBuilder.forPort(port)
        .maxConnectionAge(1, TimeUnit.SECONDS)
        .maxConnectionAgeGrace(300, TimeUnit.SECONDS)
        .addService(HelloWorldBlockingImpl())
        .build()
}

internal fun requestCycle(stub: GreeterGrpcKt.GreeterCoroutineStub) {
    runBlocking {
        repeat(10) { i ->
            repeat(100) { j ->
                val request = HelloRequest.newBuilder().setName(UUID.randomUUID().toString()).build()
                val response = stub.sayHello(request)
                println("${LocalDateTime.now()} $i $j ${response.message.length}")
            }
        }
    }
}

internal fun blockingRequestCycle(stub: GreeterGrpc.GreeterBlockingStub) {
    repeat(10) { i ->
        repeat(100) { j ->
            val request = HelloRequest.newBuilder().setName(UUID.randomUUID().toString()).build()
            val response = stub.sayHello(request)
            println("${LocalDateTime.now()} $i $j ${response.message.length}")
        }
    }
}

internal fun asyncRequestCycle(stub: GreeterGrpcKt.GreeterCoroutineStub) {
    runBlocking {
        repeat(10) { i ->
            launch(Dispatchers.IO) {
                repeat(100) { j ->
                    val request = HelloRequest.newBuilder().setName(UUID.randomUUID().toString()).build()
                    val response = stub.sayHello(request)
                    println("${LocalDateTime.now()} $i $j ${response.message.length}")
                }
            }
        }
    }
}

fun main() {
    val port = 50051

//    val server: Server = createCoroutineServer(port)
    val server: Server = createBlockingServer(port)

    server.start()

    val channel =
        ManagedChannelBuilder
            .forTarget("localhost:$port")
            .defaultLoadBalancingPolicy("round_robin").usePlaintext()
            .build()
//    val coroutineStub = GreeterGrpcKt.GreeterCoroutineStub(channel)
    val blockingStub = GreeterGrpc.newBlockingStub(channel)

    println("${LocalDateTime.now()} STARTING")
//    asyncRequestCycle(coroutineStub)
//    requestCycle(coroutineStub)
    blockingRequestCycle(blockingStub)
    println("${LocalDateTime.now()} FINISHING")

    channel.shutdown()
    server.shutdown()
    server.awaitTermination()
}
