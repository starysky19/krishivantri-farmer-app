package com.example

import com.example.data.remote.KrishivantriRuntimeConfig
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseConnectionStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseClientTest {

    @After
    fun resetConfig() {
        KrishivantriRuntimeConfig.configure(
            apiBaseUrl = "",
            supabaseUrl = "",
            supabaseAnonKey = "",
            razorpayKeyId = "",
            satelliteTileUrl = "",
            satelliteApiKey = ""
        )
    }

    @Test
    fun normalizedBaseUrl_stripsRestV1AndTrailingSlashes() {
        KrishivantriRuntimeConfig.configure(
            apiBaseUrl = "",
            supabaseUrl = "https://exampleproject.supabase.co/rest/v1/",
            supabaseAnonKey = "eyJ.abc.def",
            razorpayKeyId = "",
            satelliteTileUrl = "",
            satelliteApiKey = ""
        )
        assertEquals("https://exampleproject.supabase.co", SupabaseClient.normalizedBaseUrl())
        assertTrue(
            SupabaseClient.realtimeWebSocketUrl()
                .startsWith("wss://exampleproject.supabase.co/realtime/v1/websocket")
        )
    }

    @Test
    fun checkConnection_reportsMissingAnonKey_whenUrlIsConfiguredAndKeyIsEmpty() = runBlocking {
        KrishivantriRuntimeConfig.configure(
            apiBaseUrl = "",
            supabaseUrl = "https://exampleproject.supabase.co/rest/v1/",
            supabaseAnonKey = "",
            razorpayKeyId = "",
            satelliteTileUrl = "",
            satelliteApiKey = ""
        )
        assertTrue(SupabaseClient.isUrlConfigured())
        assertFalse(SupabaseClient.isKeyConfigured())

        val status = SupabaseClient.checkConnection()
        assertTrue(status is SupabaseConnectionStatus.MissingConfig)
        val missing = status as SupabaseConnectionStatus.MissingConfig
        assertEquals("SUPABASE_ANON_KEY", missing.missingKey)
        assertFalse(missing.message.contains("SUPABASE_ANON_KEY"))
        assertFalse(missing.message.contains("Secrets"))
    }

    @Test
    fun checkConnection_reportsMissingUrl_whenUrlIsPlaceholder() = runBlocking {
        KrishivantriRuntimeConfig.configure(
            apiBaseUrl = "",
            supabaseUrl = "https://your-project.supabase.co",
            supabaseAnonKey = "MY_SUPABASE_ANON_KEY",
            razorpayKeyId = "",
            satelliteTileUrl = "",
            satelliteApiKey = ""
        )
        assertFalse(SupabaseClient.isUrlConfigured())
        assertFalse(SupabaseClient.isKeyConfigured())

        val status = SupabaseClient.checkConnection()
        assertTrue(status is SupabaseConnectionStatus.MissingConfig)
        val missing = status as SupabaseConnectionStatus.MissingConfig
        assertEquals("SUPABASE_URL", missing.missingKey)
        assertFalse(missing.message.contains("SUPABASE_URL"))
        assertFalse(missing.message.contains("Secrets"))
    }
}
