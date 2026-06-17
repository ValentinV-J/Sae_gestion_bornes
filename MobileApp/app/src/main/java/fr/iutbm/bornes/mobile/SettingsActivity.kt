package fr.iutbm.bornes.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.iutbm.bornes.mobile.api.ApiClient
import fr.iutbm.bornes.mobile.databinding.ActivitySettingsBinding
import fr.iutbm.bornes.mobile.utils.AppLogger

/**
 * Paramètres de l'application.
 * Permet à l'utilisateur de configurer l'URL de base de l'API Node.js.
 *
 * L'URL est sauvegardée dans SharedPreferences et lue par ApiClient.
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private lateinit var binding: ActivitySettingsBinding
    private var stopLogObserver: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindLogsToScreen()

        // Pré-remplir avec l'URL actuelle
        val savedUrl = ApiClient.getSavedUrl(this)
        binding.etApiUrl.setText(savedUrl)
        AppLogger.d(TAG, "onCreate: URL chargee=$savedUrl")

        binding.btnSave.setOnClickListener {
            val url = binding.etApiUrl.text.toString().trim()
            AppLogger.d(TAG, "Action utilisateur: tentative de sauvegarde URL=$url")
            if (url.isEmpty()) {
                AppLogger.w(TAG, "Validation URL: valeur vide")
                binding.etApiUrl.error = getString(R.string.error_url_empty)
                return@setOnClickListener
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                AppLogger.w(TAG, "Validation URL: protocole invalide")
                binding.etApiUrl.error = getString(R.string.error_url_format)
                return@setOnClickListener
            }
            // Ensure trailing slash for Retrofit
            val finalUrl = if (url.endsWith("/")) url else "$url/"
            ApiClient.saveUrl(this, finalUrl)
            AppLogger.i(TAG, "URL API sauvegardee=$finalUrl")
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnReset.setOnClickListener {
            binding.etApiUrl.setText(ApiClient.DEFAULT_API_URL)
            AppLogger.d(TAG, "Action utilisateur: reset URL vers valeur par defaut")
        }
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
