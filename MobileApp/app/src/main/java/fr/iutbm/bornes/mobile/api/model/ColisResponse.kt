package fr.iutbm.bornes.mobile.api.model

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper matching the Node.js API format:
 * { "error": 0, "data": {...}, "message": "..." }
 */
data class ColisResponse(
    @SerializedName("error")   val error: Int = 0,
    @SerializedName("data")    val data: ColisInfo? = null,
    @SerializedName("message") val message: String? = null
)

/**
 * Colis document from MongoDB.
 * Matches the 'colis' collection schema.
 */
data class ColisInfo(
    @SerializedName("_id")           val uuid: String,
    @SerializedName("statut")        val statut: String,          // ATTENTE_DEPOT | SCAN_MOBILE_OK | DEPOSE | RETIRE
    @SerializedName("borne_id")      val borne_id: String?,
    @SerializedName("casier_numero") val casier_numero: Int?,
    @SerializedName("taille_colis")  val taille_colis: String?,   // S | M | L
    @SerializedName("code_retrait")  val code_retrait: String?,
    @SerializedName("date_depot")    val date_depot: String?,
    @SerializedName("date_retrait")  val date_retrait: String?
) {
    /** Returns a human-readable status label */
    fun statutLabel(): String = when (statut) {
        "ATTENTE_DEPOT"  -> "En attente de dépôt"
        "SCAN_MOBILE_OK" -> "Scan confirmé — en attente badge RFID"
        "DEPOSE"         -> "Déposé en casier"
        "RETIRE"         -> "Retiré par le client"
        else             -> statut
    }
}
