package com.eureka.face.domain.core

import com.eureka.face.domain.model.FaceImage
import com.eureka.face.domain.model.FaceMatchResult
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.MatVector
import org.bytedeco.opencv.opencv_face.EigenFaceRecognizer
import org.bytedeco.opencv.opencv_face.FaceRecognizer
import org.bytedeco.opencv.opencv_objdetect
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.slf4j.LoggerFactory
import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.sqrt

class FaceRecognitionAdapter : FaceRecognitionPort {
    
    private val logger = LoggerFactory.getLogger(FaceRecognitionAdapter::class.java)
    
    private val faceClassifier: CascadeClassifier by lazy {
        val classifierPath = Loader.loadResource(
            "/org/bytedeco/opencv/data/lbpcascades/lbpcascade_frontalface.xml",
            javaClass.classLoader
        )
        CascadeClassifier(classifierPath)
    }
    
    private val faceRecognizer: FaceRecognizer by lazy {
        EigenFaceRecognizer.create()
    }
    
    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.6
        private const val MATCH_DISTANCE_THRESHOLD = 0.5
    }
    
    override fun compareFaces(sourceImage: FaceImage, targetImage: FaceImage): FaceMatchResult {
        return try {
            logger.info("Comparing faces between images")
            
            val sourceMat = decodeImage(sourceImage.imageData)
            val targetMat = decodeImage(targetImage.imageData)
            
            val sourceFaces = detectFaces(sourceImage)
            val targetFaces = detectFaces(targetImage)
            
            val sourceFaceDetected = sourceFaces.isNotEmpty()
            val targetFaceDetected = targetFaces.isNotEmpty()
            
            if (!sourceFaceDetected || !targetFaceDetected) {
                return FaceMatchResult(
                    isMatch = false,
                    confidence = 0.0,
                    sourceFaceDetected = sourceFaceDetected,
                    targetFaceDetected = targetFaceDetected
                )
            }
            
            val sourceFace = cropFace(sourceMat, sourceFaces.first())
            val targetFace = cropFace(targetMat, targetFaces.first())
            
            val sfaceDescriptor = extractSFaceDescriptor(sourceFace)
            val tfaceDescriptor = extractSFaceDescriptor(targetFace)
            
            val similarity = calculateSFaceSimilarity(sfaceDescriptor, tfaceDescriptor)
            val isMatch = similarity >= CONFIDENCE_THRESHOLD
            
            logger.info("Face comparison result: isMatch=$isMatch, confidence=$similarity")
            
            FaceMatchResult(
                isMatch = isMatch,
                confidence = similarity,
                sourceFaceDetected = sourceFaceDetected,
                targetFaceDetected = targetFaceDetected
            )
        } catch (e: Exception) {
            logger.error("Error comparing faces", e)
            FaceMatchResult(
                isMatch = false,
                confidence = 0.0,
                sourceFaceDetected = false,
                targetFaceDetected = false
            )
        }
    }
    
    override fun detectFaces(image: FaceImage): List<Rectangle> {
        return try {
            val mat = decodeImage(image.imageData)
            val grayMat = Mat()
            opencv_imgproc.cvtColor(mat, grayMat, opencv_imgproc.COLOR_BGR2GRAY)
            
            val faces = MatVector()
            faceClassifier.detectMultiScale(grayMat, faces)
            
            val rectangles = mutableListOf<Rectangle>()
            for (i in 0 until faces.size()) {
                val face = faces.get(i)
                rectangles.add(
                    Rectangle(
                        face.x(),
                        face.y(),
                        face.width(),
                        face.height()
                    )
                )
            }
            
            logger.debug("Detected ${rectangles.size} faces")
            rectangles
        } catch (e: Exception) {
            logger.error("Error detecting faces", e)
            emptyList()
        }
    }
    
    override fun loadImageFromBase64(base64String: String): FaceImage {
        val cleanBase64 = base64String
            .replace("data:image/png;base64,", "")
            .replace("data:image/jpeg;base64,", "")
            .replace("data:image/jpg;base64,", "")
        
        val imageData = Base64.getDecoder().decode(cleanBase64)
        
        val format = when {
            cleanBase64.startsWith("iVBOR") -> "png"
            else -> "jpg"
        }
        
        return FaceImage(
            imageData = imageData,
            format = format,
            name = "image_${System.currentTimeMillis()}.$format"
        )
    }
    
    override fun encodeImageToBase64(image: FaceImage): String {
        return Base64.getEncoder().encodeToString(image.imageData)
    }
    
    private fun decodeImage(imageData: ByteArray): Mat {
        val inputStream = ByteArrayInputStream(imageData)
        val bufferedImage = ImageIO.read(inputStream) ?: throw IllegalArgumentException("Could not decode image")
        
        val mat = Mat(bufferedImage.height, bufferedImage.width, org.bytedeco.opencv.global.opencv_core.CV_8UC3)
        
        val bytes = ByteArray(bufferedImage.width * bufferedImage.height * 3)
        val raster = bufferedImage.raster
        val data = raster.getDataElements(0, 0, bufferedImage.width, bufferedImage.height, null)
        
        var idx = 0
        for (y in 0 until bufferedImage.height) {
            for (x in 0 until bufferedImage.width) {
                val pixel = raster.getDataElements(x, y, null) as IntArray
                bytes[idx++] = pixel[0].toByte()
                bytes[idx++] = pixel[1].toByte()
                bytes[idx++] = pixel[2].toByte()
            }
        }
        
        mat.put(0, 0, bytes)
        return mat
    }
    
    private fun cropFace(imageMat: Mat, rectangle: Rectangle): Mat {
        val x = rectangle.x.coerceAtLeast(0)
        val y = rectangle.y.coerceAtLeast(0)
        val width = rectangle.width.coerceAtMost(imageMat.cols() - x)
        val height = rectangle.height.coerceAtMost(imageMat.rows() - y)
        
        val faceRegion = org.bytedeco.opencv.global.opencv_core.Mat(imageMat, org.bytedeco.opencv.opencv_core.Rect(x, y, width, height))
        val resizedFace = Mat()
        opencv_imgproc.resize(faceRegion, resizedFace, org.bytedeco.opencv.opencv_core.Size(160, 160))
        
        val grayFace = Mat()
        opencv_imgproc.cvtColor(resizedFace, grayFace, opencv_imgproc.COLOR_BGR2GRAY)
        
        return grayFace
    }
    
    private fun extractSFaceDescriptor(faceMat: Mat): DoubleArray {
        val histSize = 256
        val range = doubleArrayOf(0.0, 256.0)
        val hist = org.bytedeco.opencv.opencv_core.Mat()
        val mask = org.bytedeco.opencv.opencv_core.Mat()
        
        opencv_imgproc.calcHist(
            listOf(faceMat), 
            org.bytedeco.opencv.opencv_core.MatOfInt(0), 
            mask, 
            hist, 
            org.bytedeco.opencv.opencv_core.MatOfInt(histSize), 
            org.bytedeco.opencv.opencv_core.MatOfDouble(*range),
            false
        )
        
        return hist.data().asDoubleBuffer().let { buffer ->
            val array = DoubleArray(histSize)
            buffer.get(array)
            array
        }
    }
    
    private fun calculateSFaceSimilarity(descriptor1: DoubleArray, descriptor2: DoubleArray): Double {
        if (descriptor1.size != descriptor2.size) {
            return 0.0
        }
        
        var sumSquares = 0.0
        for (i in descriptor1.indices) {
            val diff = descriptor1[i] - descriptor2[i]
            sumSquares += diff * diff
        }
        
        val euclideanDistance = sqrt(sumSquares)
        val maxDistance = sqrt(descriptor1.size.toDouble() * 256.0 * 256.0)
        
        return 1.0 - (euclideanDistance / maxDistance)
    }
}