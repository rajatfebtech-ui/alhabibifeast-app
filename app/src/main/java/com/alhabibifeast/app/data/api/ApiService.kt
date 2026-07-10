package com.alhabibifeast.app.data.api

import com.alhabibifeast.app.data.model.ApiResult
import com.alhabibifeast.app.data.model.CodOrderRequest
import com.alhabibifeast.app.data.model.DeliveryLocationResponse
import com.alhabibifeast.app.data.model.ProductsResponse
import com.alhabibifeast.app.data.model.TrackResponse
import retrofit2.http.*

interface ApiService {

    @GET("api/products.php")
    suspend fun getProducts(@Query("cat") cat: String? = null): ProductsResponse

    @GET("api/track-order.php")
    suspend fun trackOrder(@Query("id") orderId: String): TrackResponse

    @GET("api/delivery-location.php")
    suspend fun getDeliveryLocation(@Query("order_id") orderId: String): DeliveryLocationResponse

    @POST("api/cod-order.php")
    @Headers("Content-Type: application/json")
    suspend fun placeCodOrder(@Body body: CodOrderRequest): ApiResult
}
