package com.eureka.face.infrastructure.feign

import com.eureka.face.domain.dto.FaceCompareRequest
import com.eureka.face.domain.dto.FaceCompareResponse
import com.eureka.face.domain.dto.FaceDetectionResult
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@FeignClient(
    name = "face-recognition-api",
    url = "\${feign.client.face-recognition-api.url:http://localhost:8082}",
    configuration = [FeignClientConfig::class]
)
interface FaceRecognitionFeignClient {
    
    @PostMapping(
        value = ["/api/v1/face/compare"],
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun compareFaces(@RequestBody request: FaceCompareRequest): FaceCompareResponse
    
    @PostMapping(
        value = ["/api/v1/face/detect"],
        consumes = [APPLICATION_JSON_VALUE],
        produces = [APPLICATION_JSON_VALUE]
    )
    fun detectFaces(@RequestBody request: Map<String, String>): FaceDetectionResult
}