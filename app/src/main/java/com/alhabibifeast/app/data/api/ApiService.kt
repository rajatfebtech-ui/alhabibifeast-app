package com.alhabibifeast.app.data.api

import com.alhabibifeast.app.data.model.AcceptOrderResponse
import com.alhabibifeast.app.data.model.AdminOrdersResponse
import com.alhabibifeast.app.data.model.ApiResult
import com.alhabibifeast.app.data.model.CodOrderRequest
import com.alhabibifeast.app.data.model.PendingOrdersResponse
import com.alhabibifeast.app.data.model.ProductsResponse
import com.alhabibifeast.app.data.model.RiderLoginResponse
import com.alhabibifeast.app.data.model.TrackResponse
import com.alhabibifeast.app.data.model.UpdateStatusRequest
import retrofit2.http.*

interface ApiService {

    @GET("api/products.php")
    suspend fun getProducts(@Query("cat") cat: String? = null): ProductsResponse

    @GET("api/track-order.php")
    suspend fun trackOrder(@Query("id") orderId: String): TrackResponse

    @POST("api/cod-order.php")
    @Headers("Content-Type: application/json")
    suspend fun placeCodOrder(@Body body: CodOrderRequest): ApiResult

    // Admin APIs
    @GET("api/admin-orders.php")
    suspend fun getAdminOrders(
        @Query("token") token: String,
        @Query("since") since: String? = null,
    ): AdminOrdersResponse

    @POST("api/admin-update-status.php")
    @Headers("Content-Type: application/json")
    suspend fun updateOrderStatus(@Body body: UpdateStatusRequest): ApiResult

    // Rider APIs
    @GET("api/rider-login.php")
    suspend fun riderLogin(@Query("phone") phone: String, @Query("pin") pin: String): RiderLoginResponse

    @GET("api/pending-orders.php")
    suspend fun getPendingOrders(@Query("rider_id") riderId: String): PendingOrdersResponse

    @GET("api/accept-order.php")
    suspend fun acceptOrder(
        @Query("order_id") orderId: String,
        @Query("rider_id") riderId: String,
        @Query("rider_name") riderName: String,
    ): AcceptOrderResponse
}
