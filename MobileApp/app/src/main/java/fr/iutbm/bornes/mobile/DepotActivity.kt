package fr.iutbm.bornes.mobile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.iutbm.bornes.mobile.api.ApiClient
import fr.iutbm.bornes.mobile.api.model.ColisInfo
import fr.iutbm.bornes.mobile.databinding.ActivityDepotBinding
import fr.iutbm.bornes.mobile.utils.AppLogger
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
        private const val TAG = "DepotActivity"
    }

    private lateinit var binding: ActivityDepotBinding
    private lateinit var uuid: String
    private var colisInfo: ColisInfo? = null
    private var stopLogObserver: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindLogsToScreen()
        AppLogger.d(TAG, "onCreate: ecran depot initialise")

        uuid = intent.getStringExtra(EXTRA_UUID) ?: run {
            AppLogger.e(TAG, "UUID manquant dans l'intent")
            showError(getString(R.string.error_no_uuid))
            return
        }
        AppLogger.d(TAG, "UUID recu depuis scan=$uuid")

        binding.tvUuid.text = getString(R.string.uuid_label, uuid)

        binding.btnConfirmer.setOnClickListener {
            AppLogger.d(TAG, "Action utilisateur: confirmer depot pour uuid=$uuid")
            confirmerDepot()
        }
        binding.btnRescanner.setOnClickListener {
            AppLogger.d(TAG, "Action utilisateur: retour au scan")
            finish()
        }  // retour → ScanActivity

        verifierColis()
    }

    // -------------------------------------------------------------------------
    // Step 1 — Verify UUID with API
    // -------------------------------------------------------------------------

    private fun verifierColis() {
        showLoading(true)
        setStatus(getString(R.string.verifying))
        AppLogger.d(TAG, "Etape verification: appel GET verifierColis uuid=$uuid")

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@DepotActivity)
                    .verifierColis(uuid)
                AppLogger.d(TAG, "Reponse verifierColis: http=${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.error == 0 && body.data != null) {
                        AppLogger.i(TAG, "Verification OK: colis trouve uuid=$uuid")
                        onColisOk(body.data)
                    } else {
                        AppLogger.w(TAG, "Verification KO: body invalide ou erreur metier message=${body?.message}")
                        showError(body?.message ?: getString(R.string.error_unknown))
                    }
                } else {
                    AppLogger.w(TAG, "Verification KO: erreur HTTP ${response.code()}")
                    showError(getString(R.string.error_http, response.code()))
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Verification KO: exception reseau ${e.message}", e)
                showError(getString(R.string.error_network, e.message))
            }
        }
    }

    private fun onColisOk(colis: ColisInfo) {
        showLoading(false)
        colisInfo = colis
        AppLogger.d(TAG, "Affichage colis: statut=${colis.statut}, casier=${colis.casier_numero}")

        // Check the statut is valid for a new depot
        if (colis.statut == "DEPOSE" || colis.statut == "RETIRE") {
            AppLogger.w(TAG, "Colis non deposable: statut=${colis.statut}")
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
        AppLogger.d(TAG, "Etape verification terminee: attente confirmation utilisateur")
    }

    // -------------------------------------------------------------------------
    // Step 2 — Confirm scan (PATCH → SCAN_MOBILE_OK)
    // -------------------------------------------------------------------------

    private fun confirmerDepot() {
        showLoading(true)
        binding.btnConfirmer.isEnabled = false
        setStatus(getString(R.string.confirming))
        AppLogger.d(TAG, "Etape confirmation: appel PATCH confirmerScan uuid=$uuid")

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService(this@DepotActivity)
                    .confirmerScan(uuid)
                AppLogger.d(TAG, "Reponse confirmerScan: http=${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.error == 0) {
                        AppLogger.i(TAG, "Confirmation OK pour uuid=$uuid")
                        onConfirmationOk()
                    } else {
                        AppLogger.w(TAG, "Confirmation KO: erreur metier message=${body?.message}")
                        showError(body?.message ?: getString(R.string.error_unknown))
                    }
                } else {
                    AppLogger.w(TAG, "Confirmation KO: erreur HTTP ${response.code()}")
                    showError(getString(R.string.error_http, response.code()))
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Confirmation KO: exception reseau ${e.message}", e)
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
        AppLogger.d(TAG, "Etape confirmation terminee avec succes")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnConfirmer.isEnabled = !show
        AppLogger.v(TAG, "UI loading=$show")
    }

    private fun setStatus(msg: String) {
        binding.tvStatus.text = msg
        AppLogger.v(TAG, "Statut UI=$msg")
    }

    private fun showError(msg: String) {
        showLoading(false)
        binding.tvStatusIcon.text = "❌"
        setStatus(msg)
        binding.cardColisInfo.visibility = View.GONE
        binding.btnConfirmer.visibility  = View.GONE
        binding.btnRescanner.text        = getString(R.string.btn_retry)
        binding.btnRescanner.visibility  = View.VISIBLE
        AppLogger.e(TAG, "Etat erreur affiche a l'utilisateur: $msg")
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
            binding.svDebugLogs.post { binding.svDebugLogs.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
