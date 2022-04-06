plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.6.10"
}

group = "bitshares-kit"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinVersion = "1.6.10"
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:0.1.16")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.65")
}

dependencies {
    val ktorVersion = "2.0.0-beta-1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
}

dependencies {
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")
    testImplementation("com.github.bilthon:graphenej:0.4.2")
    testImplementation("com.github.bilthon:graphenej:0.4.2")
    testImplementation("junit:junit:4.13")
    testImplementation("org.slf4j:slf4j-jdk14:1.7.30")
    testImplementation("org.bouncycastle:bcpkix-jdk15on:1.65")
    testImplementation("org.bitcoinj:bitcoinj-core:0.14.3")
}