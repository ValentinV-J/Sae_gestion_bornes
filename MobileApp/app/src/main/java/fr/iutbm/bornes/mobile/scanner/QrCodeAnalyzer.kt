package fr.iutbm.bornes.mobile.scanner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import fr.iutbm.bornes.mobile.utils.AppLogger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX ImageAnalysis.Analyzer using Google ML Kit Barcode Scanning.
 *
 * Analyse chaque frame de la caméra et appelle [onQrCodeDetected] dès qu'un
 * QR code valide est trouvé. Utilise un verrou [processing] pour éviter
 * d'appeler le callback plusieurs fois pour le même colis.
 *
 * @param onQrCodeDetected Callback appelée avec la valeur brute du QR code (UUID).
 */
class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "QrCodeAnalyzer"
    }

    private val processing = AtomicBoolean(false)

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (processing.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                AppLogger.v(TAG, "Frame analysee: ${barcodes.size} code(s) detecte(s)")
                for (barcode in barcodes) {
                    val raw = barcode.rawValue
                    if (!raw.isNullOrBlank() && processing.compareAndSet(false, true)) {
                        AppLogger.i(TAG, "QR valide detecte=$raw")
                        onQrCodeDetected(raw)
                        break
                    }
                }
            }
            .addOnFailureListener { e ->
                // Scan failed — next frame will be tried
                AppLogger.w(TAG, "Echec analyse frame: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /** Réinitialise le verrou pour permettre un nouveau scan (ex: après erreur). */
    fun reset() {
        processing.set(false)
        AppLogger.d(TAG, "Verrou scan reinitialise")
    }
}
