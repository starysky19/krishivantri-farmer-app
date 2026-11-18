package com.example.data.remote

import com.example.data.model.LatLngPoint
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class PilotRegisterRequest(
    val name: String,
    val phone: String,
    val drone_type: String,
    val tank_capacity: Double,
    val license_document: String,
    val bank_upi_id: String
)

@JsonClass(generateAdapter = true)
data class FarmerRegisterRequest(
    val name: String,
    val phone: String,
    val farmer_id_number: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val phone: String,
    val role: String,
    val admin_passcode: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val user_id: String,
    val name: String,
    val phone: String,
    val role: String,
    val token: String
)

@JsonClass(generateAdapter = true)
data class CreateOrderRequest(
    val farmer_id: String,
    val location_address: String,
    val field_polygon_coordinates: List<LatLngPoint>,
    val acres_to_spray: Double,
    val payment_method: String,
    val razorpay_payment_id: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderResponseDto(
    val id: String,
    val acres_to_spray: Double,
    val total_amount: Double,
    val admin_commission: Double,
    val pilot_earnings: Double,
    val payment_method: String,
    val payment_status: String,
    val order_status: String,
    val pilot_eta_minutes: Int
)

@JsonClass(generateAdapter = true)
data class ToggleOnlineRequest(val pilot_id: String, val is_online: Boolean)

@JsonClass(generateAdapter = true)
data class WalletTopUpRequest(val pilot_id: String, val amount: Double, val razorpay_payment_id: String)

@JsonClass(generateAdapter = true)
data class RatePilotRequest(val rating: Int)

@JsonClass(generateAdapter = true)
data class ApiStatusResponse(val success: Boolean, val message: String)

interface KrishivantriApiService {
    @POST("auth/pilot/register/")
    suspend fun registerPilot(@Body request: PilotRegisterRequest): AuthResponse

    @POST("auth/farmer/register/")
    suspend fun registerFarmer(@Body request: FarmerRegisterRequest): AuthResponse

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("farmer/orders/")
    suspend fun createOrder(@Body request: CreateOrderRequest): OrderResponseDto

    @POST("farmer/orders/{orderId}/rate/")
    suspend fun ratePilot(
        @Path("orderId") orderId: String,
        @Body request: RatePilotRequest
    ): ApiStatusResponse

    @POST("pilot/toggle-online/")
    suspend fun togglePilotOnline(@Body request: ToggleOnlineRequest): ApiStatusResponse

    @GET("pilot/{pilotId}/queue/")
    suspend fun getPilotQueue(@Path("pilotId") pilotId: String): List<OrderResponseDto>

    @POST("pilot/orders/{orderId}/accept/")
    suspend fun acceptOrder(@Path("orderId") orderId: String): OrderResponseDto

    @POST("pilot/orders/{orderId}/complete/")
    suspend fun completeOrder(@Path("orderId") orderId: String): OrderResponseDto

    @POST("pilot/wallet/topup/")
    suspend fun topUpWallet(@Body request: WalletTopUpRequest): ApiStatusResponse
}

object KrishivantriNetworkClient {
    fun create(): KrishivantriApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        return Retrofit.Builder()
            .baseUrl(KrishivantriRuntimeConfig.apiBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(KrishivantriApiService::class.java)
    }
}
