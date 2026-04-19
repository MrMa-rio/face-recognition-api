package com.eureka.face.domain.core

import com.eureka.face.domain.model.FaceMatchResult
import com.eureka.face.domain.model.FaceImage

interface FaceRecognitionPort {
    fun compareFaces(sourceImage: FaceImage, targetImage: FaceImage): FaceMatchResult
    fun detectFaces(image: FaceImage): List<java.awt.Rectangle>
    fun loadImageFromBase64(base64String: String): FaceImage
    fun encodeImageToBase64(image: FaceImage): String
}