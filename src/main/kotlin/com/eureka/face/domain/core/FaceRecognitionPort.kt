package com.eureka.face.domain.core

import com.eureka.face.domain.model.FaceMatchResult
import com.eureka.face.domain.model.FaceImage
import com.eureka.face.domain.model.LivenessResult

interface FaceRecognitionPort {
    fun compareFaces(sourceImage: FaceImage, targetImage: FaceImage, detectSpoofing: Boolean = false): FaceMatchResult
    fun detectFaces(image: FaceImage): List<java.awt.Rectangle>
    fun verifySpoofing(image: FaceImage): LivenessResult
    fun loadImageFromBase64(base64String: String): FaceImage
    fun loadImageFromBytes(bytes: ByteArray, originalName: String): FaceImage
    fun encodeImageToBase64(image: FaceImage): String
}