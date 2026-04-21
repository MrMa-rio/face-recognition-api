package com.eureka.face.domain.core

import com.eureka.face.domain.model.FaceImage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.nio.file.Files

@SpringBootTest
@ActiveProfiles("local")
class FaceRecognitionTest {

    @Autowired
    private lateinit var faceRecognitionAdapter: FaceRecognitionAdapter

    private lateinit var person1Bytes: ByteArray
    private lateinit var person2Bytes: ByteArray
    private lateinit var person3Bytes: ByteArray
    private var spoof1Bytes: ByteArray? = null

    @BeforeEach
    fun setup() {
        val person1File = File("src/test/resources/test-images/person1.jpg")
        val person2File = File("src/test/resources/test-images/person2.jpg")
        val person3File = File("src/test/resources/test-images/person3.jpg")
        val spoof1File = File("src/test/resources/test-images/spoof1.jpg")
        
        assertTrue(person1File.exists(), "Image person1.jpg not found")
        assertTrue(person2File.exists(), "Image person2.jpg not found")
        assertTrue(person3File.exists(), "Image person3.jpg not found")
        
        person1Bytes = Files.readAllBytes(person1File.toPath())
        person2Bytes = Files.readAllBytes(person2File.toPath())
        person3Bytes = Files.readAllBytes(person3File.toPath())

        if (spoof1File.exists() && spoof1File.length() > 100) {
            spoof1Bytes = Files.readAllBytes(spoof1File.toPath())
        }
    }

    @Test
    fun `should match same person`() {
        val image1 = FaceImage(person1Bytes, "jpg", "person1.jpg")
        val image2 = FaceImage(person1Bytes, "jpg", "person1_copy.jpg")

        val result = faceRecognitionAdapter.compareFaces(image1, image2)

        println("[DEBUG_LOG] Match same person result: $result")
        assertTrue(result.isMatch, "Should be a match for the same person")
        assertTrue(result.confidence >= 0.99, "Confidence should be very high for same image")
    }

    @Test
    fun `should not match different persons`() {
        val image1 = FaceImage(person1Bytes, "jpg", "person1.jpg")
        val image2 = FaceImage(person3Bytes, "jpg", "person3.jpg")

        val result = faceRecognitionAdapter.compareFaces(image1, image2)

        println("[DEBUG_LOG] No match result: $result")
        assertFalse(result.isMatch, "Should NOT be a match for different persons")
    }
    @Test
    fun `should verify spoofing for real image`() {
        val image = FaceImage(person1Bytes, "jpg", "person1.jpg")
        val result = faceRecognitionAdapter.verifySpoofing(image)

        println("[DEBUG_LOG] Spoofing real image result: $result")
        assertTrue(result.isReal, "Should be considered real: ${result.message}")
    }

    @Test
    fun `should detect spoofing for fake image`() {
        spoof1Bytes?.let { bytes ->
            val image = FaceImage(bytes, "jpg", "spoof1.jpg")
            val result = faceRecognitionAdapter.verifySpoofing(image)

            println("[DEBUG_LOG] Spoofing fake image result: $result")
            // Dependendo da imagem baixada, pode não detectar 100% sem ajuste, 
            // mas esperamos que ou falhe em isReal ou tenha score baixo.
            // Para o teste ser rigoroso, verificamos se isReal é falso.
            assertFalse(result.isReal, "Should be considered fake (spoofing): ${result.message}")
        } ?: println("[DEBUG_LOG] Skipping spoof test: spoof1.jpg not available")
    }

    @Test
    fun `should match person 1 with its comparison image`() {
        val person1File = File("src/test/resources/test-images/person1.jpg")
        val comparePerson1File = File("src/test/resources/test-images/compare_person1.jpeg")

        assertTrue(comparePerson1File.exists(), "Image compare_person1.jpeg not found")

        val img1 = FaceImage(Files.readAllBytes(person1File.toPath()), "jpg", "person1.jpg")
        val img2 = FaceImage(Files.readAllBytes(comparePerson1File.toPath()), "jpeg", "compare_person1.jpeg")

        val result = faceRecognitionAdapter.compareFaces(img1, img2)
        println("[DEBUG_LOG] Person 1 Match Result: $result")
        assertTrue(result.isMatch, "Person 1 should match with compare_person1: ${result.confidence}")
    }

    @Test
    fun `should match person 2 with its comparison images`() {
        val person2File = File("src/test/resources/test-images/person2.jpg")
        val compareFiles = listOf(
            "compare_person2.jpeg",
            "compare_person2(2).jpeg",
            "compare_person2(3).jpeg"
        )

        val imgBase = FaceImage(Files.readAllBytes(person2File.toPath()), "jpg", "person2.jpg")

        compareFiles.forEach { fileName ->
            val file = File("src/test/resources/test-images/$fileName")
            assertTrue(file.exists(), "Image $fileName not found")
            
            val imgCompare = FaceImage(Files.readAllBytes(file.toPath()), "jpeg", fileName)
            val result = faceRecognitionAdapter.compareFaces(imgBase, imgCompare)
            
            println("[DEBUG_LOG] Person 2 Match with $fileName Result: $result")
            assertTrue(result.isMatch, "Person 2 should match with $fileName: ${result.confidence}")
        }
    }

    @Test
    fun `should detect spoofing in messi border image`() {
        val messiFile = File("src/test/resources/test-images/messi_border.jpeg")
        assertTrue(messiFile.exists(), "Image messi_border.jpeg not found")

        val image = FaceImage(Files.readAllBytes(messiFile.toPath()), "jpeg", "messi_border.jpeg")
        val result = faceRecognitionAdapter.verifySpoofing(image)

        println("[DEBUG_LOG] Messi Spoofing Result: $result")
        assertFalse(result.isReal, "Messi border image should be detected as spoofing: ${result.message}")
    }

    @Test
    fun `should analyze spoofing for WhatsApp images`() {
        val whatsAppImages = listOf(
            "WhatsApp Image 2026-04-21 at 01.50.28 (1).jpeg",
            "WhatsApp Image 2026-04-21 at 01.50.28 (3).jpeg",
            "WhatsApp Image 2026-04-21 at 01.50.28 (4).jpeg"
        )

        whatsAppImages.forEach { fileName ->
            val file = File("src/test/resources/test-images/$fileName")
            assertTrue(file.exists(), "Image $fileName not found")

            val image = FaceImage(Files.readAllBytes(file.toPath()), "jpeg", fileName)
            val result = faceRecognitionAdapter.verifySpoofing(image)
            
            println("[DEBUG_LOG] WhatsApp Spoofing ($fileName) Result: $result")
            // WhatsApp images usually have low variance/sharpness, being detected as spoofing
            assertFalse(result.isReal, "WhatsApp image $fileName should be detected as spoofing/low quality: ${result.message}")
        }
    }
    @Test
    fun `should match same person with detect spoofing`() {
        val image1 = FaceImage(person1Bytes, "jpg", "person1.jpg")
        val image2 = FaceImage(person1Bytes, "jpg", "person1_copy.jpg")

        val result = faceRecognitionAdapter.compareFaces(image1, image2, detectSpoofing = true)

        println("[DEBUG_LOG] Match same person with spoofing result: $result")
        assertTrue(result.isMatch, "Should be a match for the same person")
        assertTrue(result.sourceSpoofing != null, "Source spoofing should be present")
        assertTrue(result.targetSpoofing != null, "Target spoofing should be present")
        assertTrue(result.sourceSpoofing?.isReal == true, "Source should be real")
        assertTrue(result.targetSpoofing?.isReal == true, "Target should be real")
    }

    @Test
    fun `should detect spoofing during comparison`() {
        spoof1Bytes?.let { bytes ->
            val imageReal = FaceImage(person1Bytes, "jpg", "person1.jpg")
            val imageSpoof = FaceImage(bytes, "jpg", "spoof1.jpg")

            val result = faceRecognitionAdapter.compareFaces(imageReal, imageSpoof, detectSpoofing = true)

            println("[DEBUG_LOG] Comparison with spoof result: $result")
            // Mesmo que não dê match (porque são pessoas diferentes), o liveness do target deve ser falso
            assertTrue(result.sourceSpoofing?.isReal == true, "Source should be real")
            assertFalse(result.targetSpoofing?.isReal == true, "Target should be detected as spoofing")
        } ?: println("[DEBUG_LOG] Skipping comparison spoof test: spoof1.jpg not available")
    }
}
