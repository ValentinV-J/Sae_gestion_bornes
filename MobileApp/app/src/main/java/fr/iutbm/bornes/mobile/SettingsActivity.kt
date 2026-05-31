package fr.iutbm.bornes.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.iutbm.bornes.mobile.api.ApiClient
import fr.iutbm.bornes.mobile.databinding.ActivitySettingsBinding

/**
 * Paramètres de l'application.
 * Permet à l'utilisateur de configurer l'URL de base de l'API Node.js.
 *
 * L'URL est sauvegardée dans SharedPreferences et lue par ApiClient.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pré-remplir avec l'URL actuelle
        binding.etApiUrl.setText(ApiClient.getSavedUrl(this))

        binding.btnSave.setOnClickListener {
            val url = binding.etApiUrl.text.toString().trim()
            if (url.isEmpty()) {
                binding.etApiUrl.error = getString(R.string.error_url_empty)
                return@setOnClickListener
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                binding.etApiUrl.error = getString(R.string.error_url_format)
                return@setOnClickListener
            }
            // Ensure trailing slash for Retrofit
            val finalUrl = if (url.endsWith("/")) url else "$url/"
            ApiClient.saveUrl(this, finalUrl)
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnReset.setOnClickListener {
            binding.etApiUrl.setText(ApiClient.DEFAULT_API_URL)
        }
    }
}
