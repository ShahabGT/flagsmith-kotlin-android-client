package com.blu.flagsmith

import com.blu.flagsmith.entities.TraitWithIdentityModel
import com.flagsmith.entities.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FlagsmithServices {
    @GET("identities/")
    suspend fun getIdentityFlagsAndTraits(@Query("identity") identity: String): IdentityFlagsAndTraitsModel

    @GET("flags/")
    suspend fun getFlags(): List<FlagModel>

    @POST("traits/")
    suspend fun postTraits(@Body trait: TraitWithIdentityModel): TraitWithIdentityModel

    @POST("analytics/flags/")
    suspend fun postAnalytics(@Body eventMap: Map<String, Int?>): Any
}