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

    @Value("\${EUREKA_HOST:localhost}")
    private val eurekaHost: String = "localhost"

    @Value("\${SWAGGER_SERVER_URL:}")
    private val swaggerServerUrl: String = ""
    
    @Bean
    fun customOpenAPI(): OpenAPI {
        val servers = mutableListOf<Server>()

        if (swaggerServerUrl.isNotBlank()) {
            servers.add(Server().url(swaggerServerUrl).description("Server URL"))
        }

        servers.add(Server().url("http://$eurekaHost:$serverPort").description("Internal Server"))
        servers.add(Server().url("http://localhost:$serverPort").description("Local Server"))

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
            .servers(servers)
    }
}