package com.example.firmwaredemoplunge.data.api

import com.example.firmwaredemoplunge.data.model.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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
        @Body json: PumpDetailModal,
    ): Response<CommonResponse>

    @POST("test_pub_sub")
    suspend fun getPlungeDetailResponse(
        @Body json: PumpSpeedDetailModal,
    ): Response<CommonResponse>

    @POST("test_pub_sub")
    suspend fun getPlungeLightDetailResponse(
        @Body json: PlungeLightModel,
    ): Response<CommonResponse>

    @POST("test_pub_sub")
    suspend fun getPlungeTempDetailResponse(
        @Body json: TemperartureDetailModel,
    ): Response<CommonResponse>


    @GET("/wifi_list")
    suspend fun getWifiList(): Response<WfiNameList>


    @GET("delete_thing")
    suspend fun deleteThing(@Query("delete_thing") deviceName: String): Response<CreateThingResponse>


    @POST("/test")
    suspend fun connectDeviceWithWifi(
        @Body credential: ConnectDeviceWithWifiReq,
    ): Response<CommonResponse>

}