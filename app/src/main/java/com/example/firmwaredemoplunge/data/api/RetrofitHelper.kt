package com.example.firmwaredemoplunge.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitHelper {

    fun getInstance(baseUrl: String? = null): Retrofit {

        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder().also {
            it.addInterceptor(logging)
            it.readTimeout(5,TimeUnit.MINUTES)
            it.connectTimeout(5, TimeUnit.MINUTES)
            it.retryOnConnectionFailure(true)
        }.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl!!)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}