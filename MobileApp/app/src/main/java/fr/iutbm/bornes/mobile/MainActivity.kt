package fr.iutbm.bornes.mobile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import fr.iutbm.bornes.mobile.api.ApiClient
import fr.iutbm.bornes.mobile.databinding.ActivityMainBinding

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

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh displayed server URL (may have changed in Settings)
        binding.tvServerUrl.text = getString(R.string.server_url_label, ApiClient.getSavedUrl(this))
    }
}
