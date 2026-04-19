plugins {
    id("org.springframework.boot")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    implementation("org.bytedeco:opencv:5.10.0-2.0.1")
    implementation("org.bytedeco:opencv:5.10.0-2.0.1:linux-x86_64-gpl")
    implementation("org.bytedeco:opencv:5.10.0-2.0.1:windows-x86_64-gpl")
    implementation("org.bytedeco:opencv:5.10.0-2.0.1:macosx-x86_64-gpl")
    implementation("org.bytedeco:opencv:5.10.0-2.0.1:macosx-arm64-gpl")
    
    implementation("org.bytedeco:opencv-python:5.10.0-2.0.1")
    implementation("org.bytedeco:opencv-platform:5.10.0-2.0.1")
    
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.24")
    implementation("org.springdoc:springdoc-openapi-starter-common:2.2.24")
    
    implementation("ch.qos.logback:logback-classic")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    
    runtimeOnly("org.yaml:snakeyaml")
    runtimeOnly("org.springframework.boot:spring-boot-starter-logging")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

springBoot {
    mainClass.set("com.eureka.face.FaceRecognitionApplication")
}