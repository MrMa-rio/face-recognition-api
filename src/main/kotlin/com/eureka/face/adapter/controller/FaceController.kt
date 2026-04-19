package com.eureka.face.adapter.controller

import com.eureka.face.domain.dto.FaceCompareRequest
import com.eureka.face.domain.dto.FaceCompareResponse
import com.eureka.face.domain.dto.FaceDetectionResult
import com.eureka.face.domain.model.FaceImage
import com.eureka.face.domain.core.FaceRecognitionPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.awt.Rectangle

@Tag(name = "Face Recognition", description = "Face recognition and comparison endpoints")
@RestController
@RequestMapping("/api/v1/face")
class FaceController(
    private val faceRecognitionPort: FaceRecognitionPort
) {
    
    @Operation(summary = "Compare two faces", description = "Compare two face images and return match result")
    @PostMapping("/compare")
    fun compareFaces(@RequestBody request: FaceCompareRequest): ResponseEntity<FaceCompareResponse> {
        val sourceImage = faceRecognitionPort.loadImageFromBase64(request.sourceImage)
        val targetImage = faceRecognitionPort.loadImageFromBase64(request.targetImage)
        
        val result = faceRecognitionPort.compareFaces(sourceImage, targetImage)
        
        val message = when {
            !result.sourceFaceDetected && !result.targetFaceDetected -> "No faces detected in both images"
            !result.sourceFaceDetected -> "No face detected in source image"
            !result.targetFaceDetected -> "No face detected in target image"
            result.isMatch -> "Faces match with confidence ${String.format("%.2f", result.confidence * 100)}%"
            else -> "Faces do not match. Confidence: ${String.format("%.2f", result.confidence * 100)}%"
        }
        
        return ResponseEntity.ok(
            FaceCompareResponse(
                isMatch = result.isMatch,
                confidence = result.confidence,
                sourceFaceDetected = result.sourceFaceDetected,
                targetFaceDetected = result.targetFaceDetected,
                message = message
            )
        )
    }
    
    @Operation(summary = "Detect faces", description = "Detect faces in an image")
    @PostMapping("/detect")
    fun detectFaces(@RequestBody request: Map<String, String>): ResponseEntity<FaceDetectionResult> {
        val imageData = request["image"] ?: return ResponseEntity.badRequest().build()
        
        val image = faceRecognitionPort.loadImageFromBase64(imageData)
        val rectangles = faceRecognitionPort.detectFaces(image)
        
        val boundingBoxes = rectangles.map { rect ->
            com.eureka.face.domain.dto.FaceBoundingBox(
                x = rect.x,
                y = rect.y,
                width = rect.width,
                height = rect.height
            )
        }
        
        return ResponseEntity.ok(
            FaceDetectionResult(
                faceDetected = rectangles.isNotEmpty(),
                faceCount = rectangles.size,
                boundingBoxes = boundingBoxes
            )
        )
    }
}