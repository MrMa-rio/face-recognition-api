package com.eureka.face.adapter.config

import com.eureka.face.domain.core.FaceRecognitionAdapter
import com.eureka.face.domain.core.FaceRecognitionPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FaceAdapterConfig {
    
    @Bean
    fun faceRecognitionPort(): FaceRecognitionPort {
        return FaceRecognitionAdapter()
    }
}