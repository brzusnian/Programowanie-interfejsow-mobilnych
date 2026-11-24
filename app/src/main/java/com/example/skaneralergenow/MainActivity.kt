package com.example.skaneralergenow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var cameraPreview: PreviewView
    private lateinit var scanButton: Button
    private lateinit var dataManager: DataManager
    private lateinit var detector: AllergenDetector

    private var imageCapture: ImageCapture? = null
    private var isScanning = false

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val requestCameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Brak zgody na korzystanie z aparatu.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        dataManager = DataManager(this)
        detector = AllergenDetector(dataManager)

        cameraPreview = findViewById(R.id.cameraPreview)
        scanButton = findViewById(R.id.scanButton)

        scanButton.setOnClickListener {
            captureAndAnalyzeLabel()
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestCameraPermission.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = cameraPreview.surfaceProvider
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                Log.i("CAMERA", "Kamera i ImageCapture uruchomione.")
            } catch (e: Exception) {
                Log.e("CAMERA", "Nie udało się uruchomić kamery.", e)
                Toast.makeText(
                    this,
                    "Nie udało się uruchomić aparatu.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun captureAndAnalyzeLabel() {
        val capture = imageCapture ?: run {
            Toast.makeText(this, "Aparat nie jest gotowy.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isScanning) return
        isScanning = true
        scanButton.isEnabled = false

        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        resetScanState()
                        return
                    }

                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            val extractedText = visionText.text
                            Log.i("OCR_RESULT", "Rozpoznany tekst:\n$extractedText")

                            if (extractedText.isBlank()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Nie wykryto tekstu na etykiecie.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val result = detector.analyzeText(extractedText)
                                Log.i("ALLERGEN_RESULT", "Krytyczne: ${result.lethalAllergens}")
                                Log.i("ALLERGEN_RESULT", "Inne: ${result.otherAllergens}")
                                Log.i("ALLERGEN_RESULT", "Szkodliwe: ${result.harmfulSubstances}")
                                Log.i("ALLERGEN_RESULT", "E-numery: ${result.eNumbers}")
                                showResultDialog(extractedText, result)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("OCR_ERROR", "Błąd rozpoznawania tekstu", e)
                            Toast.makeText(
                                this@MainActivity,
                                "Błąd OCR: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                            resetScanState()
                        }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CAMERA_CAPTURE", "Błąd robienia zdjęcia", exception)
                    Toast.makeText(
                        this@MainActivity,
                        "Nie udało się zrobić zdjęcia.",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetScanState()
                }
            }
        )
    }

    private fun showResultDialog(
        rawText: String,
        result: AllergenDetector.DetectionResult
    ) {
        val message = buildString {
            appendLine("Alergeny krytyczne: ${result.lethalAllergens.ifEmpty { listOf("Brak") }.joinToString()}")
            appendLine("Pozostałe alergeny: ${result.otherAllergens.ifEmpty { listOf("Brak") }.joinToString()}")
            appendLine("Substancje szkodliwe: ${result.harmfulSubstances.ifEmpty { listOf("Brak") }.joinToString()}")
            if (result.eNumbers.isNotEmpty()) {
                appendLine("Numery E:")
                result.eNumbers.forEach { (code, desc) ->
                    appendLine("• $code — $desc")
                }
            } else {
                appendLine("Numery E: Brak")
            }
            appendLine()
            appendLine("Rozpoznany tekst:")
            append(rawText.take(400))
        }

        AlertDialog.Builder(this)
            .setTitle("Wynik skanowania etykiety")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun resetScanState() {
        isScanning = false
        scanButton.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        textRecognizer.close()
    }
}