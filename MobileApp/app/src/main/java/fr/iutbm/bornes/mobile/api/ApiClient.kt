package fr.iutbm.bornes.mobile.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit — URL configurable via SharedPreferences (SettingsActivity).
 *
 * Choix de Retrofit (vs OkHttp brut) :
 * - Déclaratif (interface annotée), plus lisible
 * - Intégration Kotlin Coroutines native (suspend functions)
 * - Standard industrie, bien documenté, maintenu activement par Square
 */
object ApiClient {

    private const val PREFS_NAME = "bornes_prefs"
    private const val KEY_API_URL = "api_url"
    const val DEFAULT_API_URL = "http://10.0.2.2:3000/api/"
    // 10.0.2.2 = localhost depuis l'émulateur Android. Sur appareil physique,
    // utiliser l'IP locale du PC serveur (ex: http://192.168.1.X:3000/api/)

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    /**
     * Returns an ApiService instance, rebuilding Retrofit if the URL changed.
     */
    fun getService(context: Context): ApiService {
        val baseUrl = getSavedUrl(context)
        if (retrofit == null || currentBaseUrl != baseUrl) {
            retrofit = buildRetrofit(baseUrl)
            currentBaseUrl = baseUrl
        }
        return retrofit!!.create(ApiService::class.java)
    }

    fun getSavedUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
    }

    fun saveUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_URL, url)
            .apply()
        // Force rebuild on next call
        retrofit = null
        currentBaseUrl = null
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
