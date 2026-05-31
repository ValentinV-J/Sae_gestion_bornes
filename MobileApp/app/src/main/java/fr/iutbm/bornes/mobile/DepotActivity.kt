package fr.iutbm.bornes.mobile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.iutbm.bornes.mobile.api.ApiClient
import fr.iutbm.bornes.mobile.api.model.ColisInfo
import fr.iutbm.bornes.mobile.databinding.ActivityDepotBinding
import kotlinx.coroutines.launch

/**
 * Écran de vérification et confirmation de dépôt.
 *
 * Reçoit un UUID via Intent depuis ScanActivity.
 *
 * Flux :
 *  1. Affiche l'UUID scanné
 *  2. Auto-appel GET /api/colis/:uuid → vérification existence
 *  3a. Si OK  → affiche infos colis + bouton "Confirmer arrivée"
 *  3b. Si ERR → affiche message d'erreur + bouton "Rescanner"
 *  4. Sur confirmation → PATCH /api/colis/:uuid/scan (statut = SCAN_MOBILE_OK)
 *  5. Affiche succès
 */
class DepotActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UUID = "extra_uuid"
    }

    private lateinit var binding: ActivityDepotBinding
    private lateinit var uuid: String
    private var colisInfo: ColisInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uuid = intent.getStringExtra(EXTRA_UUID) ?: run {
            showError(getString(R.string.error_no_uuid))
            return
        }

        binding.tvUuid.text = getString(R.string.uuid_label, uuid)

        binding.btnConfirmer.setOnClickListener { confirmerDepot() }
        binding.btnRescanner.setOnClickListener { finish() }  // retour → ScanActivity

        verifierColis()
    }

    // -------------------------------------------------------------------------
    // Step 1 — Verify UUID with API
    // -------------------------------------------------------------------------

    private fun verifierColis() {
        showLoading(true)
        setStatus(getString(R.string.verifying))

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@DepotActivity)
                    .verifierColis(uuid)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.error == 0 && body.data != null) {
                        onColisOk(body.data)
                    } else {
                        showError(body?.message ?: getString(R.string.error_unknown))
                    }
                } else {
                    showError(getString(R.string.error_http, response.code()))
                }
            } catch (e: Exception) {
                showError(getString(R.string.error_network, e.message))
            }
        }
    }

    private fun onColisOk(colis: ColisInfo) {
        showLoading(false)
        colisInfo = colis

        // Check the statut is valid for a new depot
        if (colis.statut == "DEPOSE" || colis.statut == "RETIRE") {
            showError(getString(R.string.error_colis_already, colis.statutLabel()))
            return
        }

        // Show colis info
        binding.cardColisInfo.visibility = View.VISIBLE
        binding.tvStatut.text    = getString(R.string.statut_label, colis.statutLabel())
        binding.tvTaille.text    = getString(R.string.taille_label, colis.taille_colis ?: "?")
        binding.tvCasier.text    = getString(R.string.casier_label,
            colis.casier_numero?.toString() ?: getString(R.string.not_yet_assigned))

        setStatus(getString(R.string.colis_found))
        binding.btnConfirmer.visibility = View.VISIBLE
        binding.btnRescanner.visibility = View.GONE
    }

    // -------------------------------------------------------------------------
    // Step 2 — Confirm scan (PATCH → SCAN_MOBILE_OK)
    // -------------------------------------------------------------------------

    private fun confirmerDepot() {
        showLoading(true)
        binding.btnConfirmer.isEnabled = false
        setStatus(getString(R.string.confirming))

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@DepotActivity)
                    .confirmerScan(uuid)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.error == 0) {
                        onConfirmationOk()
                    } else {
                        showError(body?.message ?: getString(R.string.error_unknown))
                    }
                } else {
                    showError(getString(R.string.error_http, response.code()))
                }
            } catch (e: Exception) {
                showError(getString(R.string.error_network, e.message))
            }
        }
    }

    private fun onConfirmationOk() {
        showLoading(false)
        setStatus(getString(R.string.confirmation_ok))
        binding.tvStatusIcon.text = "✅"
        binding.btnConfirmer.visibility = View.GONE
        binding.btnRescanner.text       = getString(R.string.btn_back_home)
        binding.btnRescanner.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnConfirmer.isEnabled = !show
    }

    private fun setStatus(msg: String) {
        binding.tvStatus.text = msg
    }

    private fun showError(msg: String) {
        showLoading(false)
        binding.tvStatusIcon.text = "❌"
        setStatus(msg)
        binding.cardColisInfo.visibility = View.GONE
        binding.btnConfirmer.visibility  = View.GONE
        binding.btnRescanner.text        = getString(R.string.btn_retry)
        binding.btnRescanner.visibility  = View.VISIBLE
    }
}
