package com.eureka.face.infrastructure.feign

import feign.Logger
import feign.Request
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class FeignClientConfig {
    
    @Bean
    fun loggerLevel(): Logger.Level {
        return Logger.Level.FULL
    }
    
    @Bean
    fun requestOptions(): Request.Options {
        return Request.Options(Duration.ofSeconds(10), Duration.ofSeconds(30), true)
    }
}