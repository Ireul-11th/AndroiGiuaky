package com.example.giuaky

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

interface CloudinaryApiService {
    @FormUrlEncoded
    @POST("v1_1/{cloudName}/image/upload")
    fun uploadImage(
        @Path("cloudName") cloudName: String,
        @Field("file") fileUrl: String,
        @Field("upload_preset") uploadPreset: String
    ): Call<CloudinaryResponse>
}