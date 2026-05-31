package fr.iutbm.bornes.mobile.api

import fr.iutbm.bornes.mobile.api.model.ColisResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

/**
 * Retrofit interface — routes du serveur Node.js API concernant le mobile livreur.
 *
 * Base URL définie dans ApiClient (configurable via SettingsActivity).
 */
interface ApiService {

    /**
     * Vérifie l'existence d'un colis par son UUID (scanné depuis le QR code).
     * GET /api/colis/:uuid
     *
     * Réponse:
     *   200 { error: 0, data: ColisInfo }     si le colis existe
     *   404 { error: 1, message: "..." }      si l'UUID est inconnu
     */
    @GET("colis/{uuid}")
    suspend fun verifierColis(
        @Path("uuid") uuid: String
    ): Response<ColisResponse>

    /**
     * Confirme que le QR code a été scanné par le livreur.
     * Met à jour le statut du colis : ATTENTE_DEPOT → SCAN_MOBILE_OK
     * PATCH /api/colis/:uuid/scan
     *
     * Réponse:
     *   200 { error: 0, data: ColisInfo }     si mise à jour OK
     *   404 / 400                             si erreur
     */
    @PATCH("colis/{uuid}/scan")
    suspend fun confirmerScan(
        @Path("uuid") uuid: String
    ): Response<ColisResponse>
}
