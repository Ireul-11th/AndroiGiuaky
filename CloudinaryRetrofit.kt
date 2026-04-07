package com.example.giuaky

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CloudinaryRetrofit {
    private const val BASE_URL = "https://api.cloudinary.com/"

    val api: CloudinaryApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApiService::class.java)
    }
}