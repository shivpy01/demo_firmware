package com.example.firmwaredemoplunge.data.api

import com.example.firmwaredemoplunge.data.model.CommonResponse
import com.example.firmwaredemoplunge.data.model.CreateThingResponse
import com.example.firmwaredemoplunge.data.model.WfiNameList
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RouterApi {

    @GET("create_thing")
    suspend fun createThing(@Query("device_name") deviceName: String): Response<CreateThingResponse>

    @POST("/test")
    suspend fun getRouterResponse(
        @Body credential: RequestBody,
    ): Response<CommonResponse>


    @POST("test_pub_sub")
    suspend fun getPlungeDetailResponse(
        @Query("device_name") deviceName: String,
        @Body json: String,
    ): Response<CommonResponse>


    @GET("/wifi_list")
    suspend fun getWifiList(): Response<WfiNameList>
}