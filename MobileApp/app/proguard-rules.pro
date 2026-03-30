# Règles ProGuard par défaut générées par Android Studio
# Le code n'est pas minifié en debug, seulement en release.

# Garder les data classes Kotlin pour Gson (sérialisation JSON)
-keep class fr.iutbm.bornes.mobile.api.model.** { *; }

# Garder les noms de champs annotés @SerializedName
-keepattributes *Annotation*

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
