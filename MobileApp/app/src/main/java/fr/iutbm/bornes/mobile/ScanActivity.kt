package fr.iutbm.bornes.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import fr.iutbm.bornes.mobile.databinding.ActivityScanBinding
import fr.iutbm.bornes.mobile.scanner.QrCodeAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Scanner QR Code utilisant CameraX + Google ML Kit.
 *
 * Flux :
 *  1. Vérification permission CAMERA (demande si nécessaire)
 *  2. Démarrage CameraX avec Preview + ImageAnalysis
 *  3. QrCodeAnalyzer analyse chaque frame
 *  4. QR détecté → arrêt caméra → DepotActivity(uuid)
 */
class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var qrAnalyzer: QrCodeAnalyzer

    // Launcher pour la demande de permission caméra
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else {
            Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnCancel.setOnClickListener { finish() }

        // Vérification permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview — affiche la caméra sur le PreviewView
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Analyse des frames pour détecter le QR code
            qrAnalyzer = QrCodeAnalyzer { uuid ->
                // Appelé depuis le thread d'analyse — on passe sur le main thread
                runOnUiThread { onQrDetected(uuid) }
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, qrAnalyzer) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.camera_error, e.message), Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun onQrDetected(uuid: String) {
        // Affiche le UUID détecté pour feedback visuel
        binding.tvDetectedUuid.text = getString(R.string.uuid_detected, uuid)
        binding.tvInstruction.text  = getString(R.string.verifying)

        // Lance DepotActivity avec l'UUID
        val intent = Intent(this, DepotActivity::class.java).apply {
            putExtra(DepotActivity.EXTRA_UUID, uuid)
        }
        startActivity(intent)
        // On ne ferme PAS ici — retour possible via Back
        // Pour permettre un nouveau scan si erreur, l'analyzer se réactivera dans onResume
    }

    override fun onResume() {
        super.onResume()
        // Réactive le scanner (cas du retour depuis DepotActivity après erreur)
        if (::qrAnalyzer.isInitialized) {
            qrAnalyzer.reset()
            binding.tvDetectedUuid.text = ""
            binding.tvInstruction.text  = getString(R.string.scan_instruction)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
