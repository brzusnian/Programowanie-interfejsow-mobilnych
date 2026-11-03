
package com.example.skaneralergenow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraPreview: PreviewView

    // Obsługa pytania o zgodę na korzystanie z aparatu
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

        // Obsługa marginesów ekranu
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

        // Pobranie elementu podglądu kamery z interfejsu
        cameraPreview = findViewById(R.id.cameraPreview)

        // Sprawdzenie uprawnień do aparatu
        checkCameraPermission()

        // Obsługa przycisku skanowania (tymczasowo)
        val scanButton = findViewById<Button>(R.id.scanButton)

        scanButton.setOnClickListener {
            Toast.makeText(
                this,
                "Podgląd kamery działa. OCR dodamy w kolejnym kroku.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // TEST ODCZYTYWANIA DANYCH I WYKRYWANIA ALERGENÓW
        try {

            val dataManager = DataManager(this)
            val detector = AllergenDetector(dataManager)

            val testText = "Skład: mleko, cukier, orzechy, E102"

            val result = detector.analyzeText(testText)

            Log.i("TEST_SKANERA", "Tekst testowy: $testText")
            Log.i("TEST_SKANERA", "Alergeny krytyczne: ${result.lethalAllergens}")
            Log.i("TEST_SKANERA", "Pozostałe alergeny: ${result.otherAllergens}")
            Log.i("TEST_SKANERA", "Substancje szkodliwe: ${result.harmfulSubstances}")
            Log.i("TEST_SKANERA", "Numery E: ${result.eNumbers}")

        } catch (e: Exception) {

            Log.e("TEST_SKANERA", "Błąd podczas testowania!", e)
        }
    }

    // Sprawdzenie, czy użytkownik udzielił zgody na aparat
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

    // Uruchomienie kamery za pomocą CameraX
    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {

                val cameraProvider = cameraProviderFuture.get()

                // Konfiguracja podglądu obrazu
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider =
                            cameraPreview.surfaceProvider
                    }

                // Wybór tylnej kamery
                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                // Odłączenie poprzednich przypadków użycia kamery
                cameraProvider.unbindAll()

                // Podłączenie kamery do aktywności
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )

                Log.i("CAMERA", "Kamera została uruchomiona!")

            } catch (e: Exception) {

                Log.e(
                    "CAMERA",
                    "Nie udało się uruchomić kamery.",
                    e
                )

                Toast.makeText(
                    this,
                    "Nie udało się uruchomić aparatu.",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }
}