package com.eureka.face.domain.model

data class FaceImage(
    val imageData: ByteArray,
    val format: String,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceImage

        if (!imageData.contentEquals(other.imageData)) return false
        if (format != other.format) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageData.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

data class FaceMatchResult(
    val isMatch: Boolean,
    val confidence: Double,
    val sourceFaceDetected: Boolean,
    val targetFaceDetected: Boolean
)