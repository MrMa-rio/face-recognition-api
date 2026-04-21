package com.eureka.face.domain.dto

import com.eureka.face.domain.model.LivenessResult

data class FaceCompareRequest(
    val sourceImage: String,
    val targetImage: String,
    val detectSpoofing: Boolean? = false
)

data class FaceCompareResponse(
    val isMatch: Boolean,
    val confidence: Double,
    val sourceFaceDetected: Boolean,
    val targetFaceDetected: Boolean,
    val sourceSpoofing: LivenessResult? = null,
    val targetSpoofing: LivenessResult? = null,
    val message: String
)

data class FaceDetectionResult(
    val faceDetected: Boolean,
    val faceCount: Int,
    val boundingBoxes: List<FaceBoundingBox>
)

data class FaceBoundingBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)