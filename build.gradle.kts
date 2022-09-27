import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.proto
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "1.7.10"
val kotlinCoroutinesVersion = "1.6.4"
val protobufVersion = "3.21.6"
val grpcVersion = "1.49.0"
val grpcKotlinVersion = "1.3.0"
val nettyVersion = "4.1.82.Final"

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.19")
    }
}

plugins {
    application
    id("idea")
    id("com.google.protobuf") version "0.8.19"
    kotlin("jvm") version "1.7.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("org.example.grpc.poc.MainKt")
}

sourceSets {
    val main by getting { }
    main.java.srcDirs("build/generated/source/proto/main/grpc")
    main.java.srcDirs("build/generated/source/proto/main/grpckt")
    main.java.srcDirs("build/generated/source/proto/main/java")
    main.proto {
        srcDirs("src/main/proto")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(enforcedPlatform("io.netty:netty-bom:$nettyVersion"))
    implementation(enforcedPlatform("io.grpc:grpc-bom:$grpcVersion"))
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(enforcedPlatform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$kotlinCoroutinesVersion"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("io.grpc:grpc-core:$grpcVersion")
    implementation("io.grpc:grpc-netty:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")

    runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion")

    compileOnly("org.apache.tomcat:annotations-api:6.0.53")

    testImplementation(kotlin("test"))
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType(Copy::class.java) {
    filesMatching("**/*.proto") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
