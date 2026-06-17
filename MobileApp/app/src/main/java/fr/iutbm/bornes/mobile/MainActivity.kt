package fr.iutbm.bornes.mobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.iutbm.bornes.mobile.api.ApiClient
import fr.iutbm.bornes.mobile.databinding.ActivityMainBinding
import fr.iutbm.bornes.mobile.utils.AppLogger

/**
 * Écran d'accueil de l'application.
 *
 * Affiche :
 *   - Titre + logo de l'application
 *   - URL du serveur actuellement configuré
 *   - Bouton "Scanner un colis" → ScanActivity
 *   - Bouton "Paramètres" → SettingsActivity
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private var stopLogObserver: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindLogsToScreen()
        AppLogger.d(TAG, "onCreate: ecran principal initialise")

        binding.btnScan.setOnClickListener {
            AppLogger.d(TAG, "Navigation: ouverture de ScanActivity")
            startActivity(Intent(this, ScanActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            AppLogger.d(TAG, "Navigation: ouverture de SettingsActivity")
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh displayed server URL (may have changed in Settings)
        val currentUrl = ApiClient.getSavedUrl(this)
        binding.tvServerUrl.text = getString(R.string.server_url_label, currentUrl)
        AppLogger.d(TAG, "onResume: URL serveur affichee=$currentUrl")
    }

    override fun onDestroy() {
        stopLogObserver?.invoke()
        stopLogObserver = null
        super.onDestroy()
    }

    private fun bindLogsToScreen() {
        stopLogObserver = AppLogger.observe { logs ->
            binding.tvDebugLogs.text = if (logs.isBlank()) {
                getString(R.string.debug_logs_empty)
            } else {
                logs
            }
            binding.svDebugLogs.post { binding.svDebugLogs.fullScroll(android.view.View.FOCUS_DOWN) }
        }
    }
}
