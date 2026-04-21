package com.eureka.face.adapter.controller

import com.eureka.face.domain.dto.FaceCompareRequest
import com.eureka.face.domain.dto.FaceCompareResponse
import com.eureka.face.domain.dto.FaceDetectionResult
import com.eureka.face.domain.model.FaceImage
import com.eureka.face.domain.model.LivenessResult
import com.eureka.face.domain.core.FaceRecognitionPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.awt.Rectangle

@Tag(name = "Face Recognition", description = "Face recognition and comparison endpoints")
@CrossOrigin(origins = ["*"], allowedHeaders = ["*"], methods = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS])
@RestController
@RequestMapping("/api/v1/face")
class FaceController(
    private val faceRecognitionPort: FaceRecognitionPort
) {
    
    @Operation(summary = "Compare two faces (Base64)", description = "Compare two face images and return match result")
    @PostMapping("/compare")
    fun compareFaces(@RequestBody request: FaceCompareRequest): ResponseEntity<FaceCompareResponse> {
        val sourceImage = faceRecognitionPort.loadImageFromBase64(request.sourceImage)
        val targetImage = faceRecognitionPort.loadImageFromBase64(request.targetImage)
        
        return performComparison(sourceImage, targetImage, request.detectSpoofing ?: false)
    }
    
    @Operation(summary = "Detect faces", description = "Detect faces in an image (Base64)")
    @PostMapping("/detect")
    fun detectFaces(@RequestBody request: Map<String, String>): ResponseEntity<FaceDetectionResult> {
        val imageData = request["image"] ?: return ResponseEntity.badRequest().build()
        
        val image = faceRecognitionPort.loadImageFromBase64(imageData)
        return performDetection(image)
    }

    @Operation(summary = "Compare two faces (Multipart)", description = "Compare two face images provided as files")
    @PostMapping("/compare/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun compareFacesFile(
        @RequestPart("source") sourceFile: MultipartFile,
        @RequestPart("target") targetFile: MultipartFile,
        @RequestParam(value = "detectSpoofing", defaultValue = "false") detectSpoofing: Boolean
    ): ResponseEntity<FaceCompareResponse> {
        val sourceImage = faceRecognitionPort.loadImageFromBytes(sourceFile.bytes, sourceFile.originalFilename ?: "source.jpg")
        val targetImage = faceRecognitionPort.loadImageFromBytes(targetFile.bytes, targetFile.originalFilename ?: "target.jpg")

        return performComparison(sourceImage, targetImage, detectSpoofing)
    }

    @Operation(summary = "Detect faces (Multipart)", description = "Detect faces in an image file")
    @PostMapping("/detect/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun detectFacesFile(
        @RequestPart("image") file: MultipartFile
    ): ResponseEntity<FaceDetectionResult> {
        val image = faceRecognitionPort.loadImageFromBytes(file.bytes, file.originalFilename ?: "image.jpg")
        return performDetection(image)
    }

    @Operation(summary = "Detect spoofing (Base64)", description = "Verify if the image is real or a photo of a photo/screen")
    @PostMapping("/detect-spoofing")
    fun detectSpoofing(@RequestBody request: Map<String, String>): ResponseEntity<LivenessResult> {
        val imageData = request["image"] ?: return ResponseEntity.badRequest().build()
        val image = faceRecognitionPort.loadImageFromBase64(imageData)
        return ResponseEntity.ok(faceRecognitionPort.verifySpoofing(image))
    }

    @Operation(summary = "Detect spoofing (Multipart)", description = "Verify if the image file is real or a photo of a photo/screen")
    @PostMapping("/detect-spoofing/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun detectSpoofingFile(
        @RequestPart("image") file: MultipartFile
    ): ResponseEntity<LivenessResult> {
        val image = faceRecognitionPort.loadImageFromBytes(file.bytes, file.originalFilename ?: "image.jpg")
        return ResponseEntity.ok(faceRecognitionPort.verifySpoofing(image))
    }

    private fun performComparison(sourceImage: FaceImage, targetImage: FaceImage, detectSpoofing: Boolean): ResponseEntity<FaceCompareResponse> {
        val result = faceRecognitionPort.compareFaces(sourceImage, targetImage, detectSpoofing)
        
        var message = when {
            !result.sourceFaceDetected && !result.targetFaceDetected -> "No faces detected in both images"
            !result.sourceFaceDetected -> "No face detected in source image"
            !result.targetFaceDetected -> "No face detected in target image"
            result.isMatch -> "Faces match with confidence ${String.format("%.2f", result.confidence * 100)}%"
            else -> "Faces do not match. Confidence: ${String.format("%.2f", result.confidence * 100)}%"
        }

        if (detectSpoofing) {
            val sourceSpoofingOk = result.sourceSpoofing?.isReal ?: true
            val targetSpoofingOk = result.targetSpoofing?.isReal ?: true
            if (!sourceSpoofingOk || !targetSpoofingOk) {
                message += " [SPOOFING ALERT: Possible fraud detected]"
            }
        }
        
        return ResponseEntity.ok(
            FaceCompareResponse(
                isMatch = result.isMatch,
                confidence = result.confidence,
                sourceFaceDetected = result.sourceFaceDetected,
                targetFaceDetected = result.targetFaceDetected,
                sourceSpoofing = result.sourceSpoofing,
                targetSpoofing = result.targetSpoofing,
                message = message
            )
        )
    }

    private fun performDetection(image: FaceImage): ResponseEntity<FaceDetectionResult> {
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