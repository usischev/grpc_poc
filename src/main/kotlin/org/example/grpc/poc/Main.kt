package org.example.grpc.poc

import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.GreeterGrpcKt
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.examples.helloworld.HelloResponse
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class HelloWorldImpl : GreeterGrpcKt.GreeterCoroutineImplBase() {
    override suspend fun sayHello(request: HelloRequest): HelloResponse {
        return coroutineScope {
            HelloResponse
                .newBuilder()
                .setMessage("hello, ${request.name}".repeat(1024 * 1024 * 5).substring(0, 4194000))
                .build()
        }
    }
}

internal suspend fun requestCycle(stub: GreeterGrpcKt.GreeterCoroutineStub) {
    repeat(10) { i ->
        repeat(100) { j ->
            val request = HelloRequest.newBuilder().setName(UUID.randomUUID().toString()).build()
            val response = stub.sayHello(request)
            println("$i $j ${response.message.length}")
        }
    }
}

internal fun blockingRequestCycle(stub: GreeterGrpc.GreeterBlockingStub) {
    repeat(10) { i ->
        repeat(100) { j ->
            val request = HelloRequest.newBuilder().setName(UUID.randomUUID().toString()).build()
            val response = stub.sayHello(request)
            println("$i $j ${response.message.length}")
        }
    }
}

internal suspend fun asyncRequestCycle(scope: CoroutineScope, stub: GreeterGrpcKt.GreeterCoroutineStub) {
    repeat(10) { i ->
        scope.launch(Dispatchers.IO) {
            repeat(100) { j ->
                val request = HelloRequest.newBuilder().setName(UUID.randomUUID().toString()).build()
                val response = stub.sayHello(request)
                println("$i $j ${response.message.length}")
            }
        }
    }
}

fun main() {
    val port = 50051

    val server: Server = NettyServerBuilder.forPort(port)
        .maxConnectionAge(1, TimeUnit.SECONDS)
        .maxConnectionAgeGrace(300, TimeUnit.SECONDS)
        .addService(HelloWorldImpl())
        .build()

    server.start()

    val channel =
        ManagedChannelBuilder
            .forTarget("localhost:$port")
            .defaultLoadBalancingPolicy("round_robin").usePlaintext()
            .build()
    val coroutineStub = GreeterGrpcKt.GreeterCoroutineStub(channel)
    val blockingStub = GreeterGrpc.newBlockingStub(channel)

    runBlocking {
//        asyncRequestCycle(this, coroutineStub)
//        requestCycle(coroutineStub)
    }
    blockingRequestCycle(blockingStub)

    channel.shutdown()
    server.shutdown()
    server.awaitTermination()
}
