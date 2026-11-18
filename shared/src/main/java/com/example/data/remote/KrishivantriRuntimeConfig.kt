package com.example.data.remote

/**
 * Runtime configuration populated at startup from the application's BuildConfig
 * (supplied via .env / AI Studio Secrets panel).
 */
object KrishivantriRuntimeConfig {
    @Volatile
    var apiBaseUrl: String = "https://api.krishivantri.in/api/v1/"

    @Volatile
    var supabaseUrl: String = "https://leawuzhjkuzwgzdduwzq.supabase.co"

    @Volatile
    var supabaseAnonKey: String = "MY_SUPABASE_ANON_KEY"

    @Volatile
    var razorpayKeyId: String = "MY_RAZORPAY_KEY_ID"

    @Volatile
    var satelliteTileUrl: String =
        "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"

    @Volatile
    var satelliteApiKey: String = "MY_SATELLITE_API_KEY"

    private fun sanitize(value: String): String =
        value.trim().trim('"', '\'')

    fun isRazorpayConfigured(): Boolean {
        val key = sanitize(razorpayKeyId)
        return key.startsWith("rzp_") && !key.contains("MY_RAZORPAY") && !key.contains("placeholder")
    }

    fun configure(
        apiBaseUrl: String,
        supabaseUrl: String,
        supabaseAnonKey: String,
        razorpayKeyId: String = this.razorpayKeyId,
        satelliteTileUrl: String = this.satelliteTileUrl,
        satelliteApiKey: String = this.satelliteApiKey
    ) {
        this.apiBaseUrl = sanitize(apiBaseUrl)
        this.supabaseUrl = sanitize(supabaseUrl)
        this.supabaseAnonKey = sanitize(supabaseAnonKey)
        this.razorpayKeyId = sanitize(razorpayKeyId)
        this.satelliteTileUrl = sanitize(satelliteTileUrl)
        this.satelliteApiKey = sanitize(satelliteApiKey)
    }
}
