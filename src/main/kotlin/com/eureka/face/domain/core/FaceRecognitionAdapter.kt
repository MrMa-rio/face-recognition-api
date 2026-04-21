package com.eureka.face.domain.core

import com.eureka.face.domain.model.FaceImage
import com.eureka.face.domain.model.FaceMatchResult
import com.eureka.face.domain.model.LivenessResult
import org.bytedeco.javacpp.Loader
import org.bytedeco.opencv.opencv_java
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.FaceDetectorYN
import org.opencv.objdetect.FaceRecognizerSF
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.awt.Rectangle
import java.io.File
import java.nio.file.Files
import java.util.Base64
import jakarta.annotation.PostConstruct

@Component
class FaceRecognitionAdapter : FaceRecognitionPort {
    private val logger = LoggerFactory.getLogger(FaceRecognitionAdapter::class.java)

    private lateinit var faceDetector: FaceDetectorYN
    private lateinit var faceRecognizer: FaceRecognizerSF

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.363 // Padrão recomendado para SFace
        private const val DETECTOR_THRESHOLD = 0.6f // Valor mais equilibrado para detecção
    }

    @PostConstruct
    fun init() {
        try {
            logger.info("Loading Native OpenCV via Bytedeco...")
            Loader.load(opencv_java::class.java)
            logger.info("OpenCV Native Library loaded via Bytedeco")

            val modelDir = File("models").apply { if (!exists()) mkdirs() }

            val detectorFile = extractResource("/models/face_detection_yunet_2023mar.onnx", "models/face_detection_yunet_2023mar.onnx")
            val recognizerFile = extractResource("/models/face_recognition_sface_2021dec.onnx", "models/face_recognition_sface_2021dec.onnx")

            // Usar Size(1, 1) como inicial para evitar erro !isDynamicShape no OpenCV 4.13.0
            faceDetector = FaceDetectorYN.create(
                detectorFile.absolutePath,
                "",
                Size(1.0, 1.0),
                DETECTOR_THRESHOLD, 
                0.3f,
                5000
            )

            faceRecognizer = FaceRecognizerSF.create(
                recognizerFile.absolutePath,
                ""
            )
            
            logger.info("FaceDetectorYN and FaceRecognizerSF initialized successfully")
        } catch (ex: Exception) {
            logger.error("Failed to initialize OpenCV models", ex)
        }
    }

    private fun extractResource(resourcePath: String, targetPath: String): File {
        val targetFile = File(targetPath)
        if (!targetFile.exists()) {
            val inputStream = javaClass.getResourceAsStream(resourcePath) 
                ?: throw RuntimeException("Resource not found: $resourcePath")
            Files.copy(inputStream, targetFile.toPath())
        }
        return targetFile
    }

    override fun compareFaces(sourceImage: FaceImage, targetImage: FaceImage, detectSpoofing: Boolean): FaceMatchResult {
        return try {
            val sourceSpoofing = if (detectSpoofing) verifySpoofing(sourceImage) else null
            val targetSpoofing = if (detectSpoofing) verifySpoofing(targetImage) else null

            val sourceMat = bytesToMat(sourceImage.imageData)
            val targetMat = bytesToMat(targetImage.imageData)

            if (sourceMat.empty() || targetMat.empty()) {
                logger.error("Failed to decode images. Source empty: ${sourceMat.empty()}, Target empty: ${targetMat.empty()}")
                return FaceMatchResult(false, 0.0, false, false)
            }

            // Precisamos das faces E possivelmente da imagem redimensionada se a detecção foi em redimensionada
            val sourceData = detectAndScale(sourceMat)
            val targetData = detectAndScale(targetMat)

            if (sourceData.faces.empty() || targetData.faces.empty()) {
                val result = FaceMatchResult(
                    isMatch = false,
                    confidence = 0.0,
                    sourceFaceDetected = !sourceData.faces.empty(),
                    targetFaceDetected = !targetData.faces.empty(),
                    sourceSpoofing = sourceSpoofing,
                    targetSpoofing = targetSpoofing
                )
                // Liberar recursos antes de retornar
                sourceMat.release()
                targetMat.release()
                sourceData.faces.release()
                targetData.faces.release()
                if (sourceData.mat !== sourceMat) sourceData.mat.release()
                if (targetData.mat !== targetMat) targetData.mat.release()
                return result
            }

            // Pegar a face com maior score de detecção (geralmente a primeira se ordenado)
            // Cada linha de 'faces' é [x1, y1, w, h, x_re, y_re, x_le, y_le, x_nt, y_nt, x_rc, y_rc, x_lc, y_lc, score]
            var bestSourceFaceIdx = 0
            var maxSourceScore = -1.0
            for (i in 0 until sourceData.faces.rows()) {
                val score = sourceData.faces.get(i, 14)[0]
                if (score > maxSourceScore) {
                    maxSourceScore = score
                    bestSourceFaceIdx = i
                }
            }

            var bestTargetFaceIdx = 0
            var maxTargetScore = -1.0
            for (i in 0 until targetData.faces.rows()) {
                val score = targetData.faces.get(i, 14)[0]
                if (score > maxTargetScore) {
                    maxTargetScore = score
                    bestTargetFaceIdx = i
                }
            }

            val sourceFace = sourceData.faces.row(bestSourceFaceIdx)
            val targetFace = targetData.faces.row(bestTargetFaceIdx)

            val sourceAligned = Mat()
            val targetAligned = Mat()
            
            // O modelo SFace espera imagens alinhadas de 112x112
            faceRecognizer.alignCrop(sourceData.mat, sourceFace, sourceAligned)
            faceRecognizer.alignCrop(targetData.mat, targetFace, targetAligned)

            // Limpar Mats de entrada após alinhar para liberar memória e evitar reuso acidental
            sourceFace.release()
            targetFace.release()
            if (sourceData.mat !== sourceMat) sourceData.mat.release()
            if (targetData.mat !== targetMat) targetData.mat.release()
            sourceData.faces.release()
            targetData.faces.release()
            sourceMat.release()
            targetMat.release()

            // Garantir redimensionamento para 112x112 se necessário
            if (sourceAligned.cols() != 112 || sourceAligned.rows() != 112) {
                val temp = Mat()
                Imgproc.resize(sourceAligned, temp, Size(112.0, 112.0))
                sourceAligned.release()
                temp.copyTo(sourceAligned)
                temp.release()
            }
            if (targetAligned.cols() != 112 || targetAligned.rows() != 112) {
                val temp = Mat()
                Imgproc.resize(targetAligned, temp, Size(112.0, 112.0))
                targetAligned.release()
                temp.copyTo(targetAligned)
                temp.release()
            }

            val sourceFeature = Mat()
            faceRecognizer.feature(sourceAligned, sourceFeature)
            val sFeat = sourceFeature.clone()
            sourceFeature.release()
            sourceAligned.release()

            val targetFeature = Mat()
            faceRecognizer.feature(targetAligned, targetFeature)
            val tFeat = targetFeature.clone()
            targetFeature.release()
            targetAligned.release()

            val cosineSimilarity = faceRecognizer.match(sFeat, tFeat, FaceRecognizerSF.FR_COSINE).toDouble()
            val l2Similarity = faceRecognizer.match(sFeat, tFeat, FaceRecognizerSF.FR_NORM_L2).toDouble()
            
            // Log feature stats for debug
            val sMean = Core.mean(sFeat).`val`[0]
            val tMean = Core.mean(tFeat).`val`[0]
            logger.info("Feature Stats - Source Mean: $sMean, Target Mean: $tMean")

            // Liberar clones
            sFeat.release()
            tFeat.release()
            
            val isMatch = cosineSimilarity > CONFIDENCE_THRESHOLD
            logger.info("Comparison result - Cosine: $cosineSimilarity, L2: $l2Similarity, Match: $isMatch (Threshold: $CONFIDENCE_THRESHOLD)")
            
            FaceMatchResult(
                isMatch = isMatch,
                confidence = cosineSimilarity,
                sourceFaceDetected = true,
                targetFaceDetected = true,
                sourceSpoofing = sourceSpoofing,
                targetSpoofing = targetSpoofing
            )
        } catch (ex: Exception) {
            logger.error("Error comparing faces", ex)
            FaceMatchResult(false, 0.0, false, false)
        }
    }

    private data class DetectionResult(val mat: Mat, val faces: Mat)

    private fun detectAndScale(mat: Mat): DetectionResult {
        // Converter para BGR se necessário (YuNet/SFace esperam BGR 3 canais)
        var inputMat = mat
        if (mat.channels() == 1) {
            inputMat = Mat()
            Imgproc.cvtColor(mat, inputMat, Imgproc.COLOR_GRAY2BGR)
        } else if (mat.channels() == 4) {
            inputMat = Mat()
            Imgproc.cvtColor(mat, inputMat, Imgproc.COLOR_BGRA2BGR)
        }

        faceDetector.inputSize = inputMat.size()
        var faces = Mat()
        faceDetector.detect(inputMat, faces)

        if (faces.empty()) {
            logger.info("No faces detected in original size (${inputMat.size()}). Trying with 320x320...")
            val resized = Mat()
            Imgproc.resize(inputMat, resized, Size(320.0, 320.0))
            faceDetector.inputSize = resized.size()
            val facesResized = Mat()
            faceDetector.detect(resized, facesResized)
            if (!facesResized.empty()) {
                logger.info("Faces detected in 320x320.")
                return DetectionResult(resized, facesResized)
            }
        }
        return DetectionResult(inputMat, faces)
    }

    override fun detectFaces(image: FaceImage): List<Rectangle> {
        return try {
            val mat = bytesToMat(image.imageData)
            val data = detectAndScale(mat)
            val faces = data.faces
            val result = mutableListOf<Rectangle>()
            
            // Escalar de volta se detectado em imagem redimensionada
            val scaleX = mat.cols().toDouble() / data.mat.cols()
            val scaleY = mat.rows().toDouble() / data.mat.rows()

            for (i in 0 until faces.rows()) {
                val x = (faces.get(i, 0)[0] * scaleX).toInt()
                val y = (faces.get(i, 1)[0] * scaleY).toInt()
                val w = (faces.get(i, 2)[0] * scaleX).toInt()
                val h = (faces.get(i, 3)[0] * scaleY).toInt()
                result.add(Rectangle(x, y, w, h))
            }
            
            // Liberar recursos
            mat.release()
            data.faces.release()
            if (data.mat !== mat) data.mat.release()
            
            result
        } catch (ex: Exception) {
            logger.error("Error detecting faces", ex)
            emptyList()
        }
    }


    private fun bytesToMat(bytes: ByteArray): Mat {
        if (bytes.isEmpty()) {
            logger.warn("Received empty byte array for image.")
            return Mat()
        }
        val matOfByte = MatOfByte(*bytes)
        val mat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR)
        if (mat == null || mat.empty()) {
            logger.warn("Image decoding failed, returned empty Mat.")
            return Mat()
        }
        return mat
    }

    override fun loadImageFromBase64(base64String: String): FaceImage {
        val cleanBase64 = base64String
            .replace(Regex("data:image/(png|jpeg|jpg);base64,"), "")

        val imageData = Base64.getDecoder().decode(cleanBase64)
        val format = if (cleanBase64.startsWith("iVBOR")) "png" else "jpg"
        return FaceImage(imageData, format, "image_${System.currentTimeMillis()}.$format")
    }

    override fun loadImageFromBytes(bytes: ByteArray, originalName: String): FaceImage {
        val format = originalName.substringAfterLast(".", "jpg")
        return FaceImage(bytes, format, originalName)
    }

    override fun encodeImageToBase64(image: FaceImage): String = Base64.getEncoder().encodeToString(image.imageData)

    override fun verifySpoofing(image: FaceImage): LivenessResult {
        val mat = bytesToMat(image.imageData)
        if (mat.empty()) {
            return LivenessResult(false, 0.0, emptyMap(), "Falha ao decodificar imagem")
        }

        try {
            // 1. Detecção de Bordas (Moire e Frames)
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

            // Blur para reduzir ruído
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)

            // Canny para encontrar bordas
            val edges = Mat()
            Imgproc.Canny(blurred, edges, 100.0, 200.0)

            // Procurar por linhas retas longas que podem indicar bordas de dispositivos/papel
            val lines = Mat()
            Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180, 50, 50.0, 10.0)

            val lineCount = lines.rows()
            
            // 2. Análise de Variância do Laplaciano (Blur/Foco)
            val laplacian = Mat()
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
            val mean = MatOfDouble()
            val stddev = MatOfDouble()
            Core.meanStdDev(laplacian, mean, stddev)
            val variance = Math.pow(stddev.get(0, 0)[0], 2.0)

            // 3. Detecção de Moire (Frequências altas)
            val edgeDensity = Core.countNonZero(edges).toDouble() / (edges.rows() * edges.cols())

            // Lógica de Score
            var score = 1.0
            val details = mutableMapOf<String, Any>()
            details["lineCount"] = lineCount
            details["variance"] = variance
            details["edgeDensity"] = edgeDensity

            var message = "Imagem parece real"

            if (lineCount > 25) {
                score -= 0.4
                message = "Possível captura de tela ou papel detectada (bordas lineares)"
            }

            if (variance < 200.0) {
                score -= 0.3
                message = "Imagem com baixa nitidez, possivelmente foto de foto"
            } else if (variance > 1000.0 && edgeDensity > 0.08) {
                score -= 0.5
                message = "Padrões de interferência (moire) detectados"
            }

            // Limpeza
            gray.release()
            blurred.release()
            edges.release()
            lines.release()
            laplacian.release()
            mat.release()

            return LivenessResult(
                isReal = score > 0.6,
                score = Math.max(0.0, Math.min(1.0, score)),
                details = details,
                message = message
            )

        } catch (e: Exception) {
            logger.error("Erro na verificação de spoofing", e)
            if (!mat.empty()) mat.release()
            return LivenessResult(false, 0.0, emptyMap(), "Erro interno no processamento: ${e.message}")
        }
    }
}