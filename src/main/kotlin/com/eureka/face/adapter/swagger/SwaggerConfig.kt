package com.eureka.face.adapter.swagger

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    
    @Value("\${server.port:8080}")
    private val serverPort: String = "8080"
    
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Face Recognition API")
                    .description("API for face recognition and comparison using OpenCV SFace and FaceRecognizer")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Eureka Team")
                            .email("team@eureka.com")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:$serverPort")
                        .description("Local Server"),
                    Server()
                        .url("http://localhost:8080")
                        .description("Gateway Server")
                )
            )
    }
}