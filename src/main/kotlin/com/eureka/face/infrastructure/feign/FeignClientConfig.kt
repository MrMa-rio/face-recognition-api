package com.eureka.face.infrastructure.feign

import feign.Logger
import feign.Request
import feign.codec.Decoder
import feign.codec.Encoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder

@Configuration
class FeignClientConfig {
    
    @Bean
    fun loggerLevel(): Logger.Level {
        return Logger.Level.FULL
    }
    
    @Bean
    fun requestOptions(): Request.Options {
        return Request.Options(
            connectTimeout = 10000,
            readTimeout = 30000,
            followRedirects = true
        )
    }
    
    @Bean
    fun encoder(): Encoder {
        return JacksonEncoder()
    }
    
    @Bean
    fun decoder(): Decoder {
        return JacksonDecoder()
    }
}