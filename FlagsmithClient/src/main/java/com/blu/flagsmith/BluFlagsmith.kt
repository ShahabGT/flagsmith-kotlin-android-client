package com.blu.flagsmith

import android.content.Context
import com.blu.flagsmith.entities.TraitWithIdentityModel
import com.blu.flagsmith.util.ErrorHandler
import com.blu.flagsmith.util.ResultEntity
import com.blu.flagsmith.util.DefaultErrorHandler
import com.flagsmith.entities.FlagModel
import com.flagsmith.entities.IdentityModel
import com.flagsmith.entities.Trait

/**
 * Flagsmith
 *
 * The main interface to all of the Flagsmith functionality
 *
 * @property environmentKey Take this API key from the Flagsmith dashboard and pass here
 * @property baseUrl By default we'll connect to the Flagsmith backend, but if you self-host you can configure here
 * @property context The current context is required to use the Flagsmith Analytics functionality
 * @property enableAnalytics Enable analytics - default true
 * @property analyticsFlushPeriod The period in seconds between attempts by the Flagsmith SDK to push analytic events to the server
 * @constructor Create empty Flagsmith
 */

class BluFlagsmith constructor(
    private val environmentKey: String,
    private val baseUrl: String = "https://edge.api.flagsmith.com/api/v1",
    private val context: Context? = null,
    private val enableAnalytics: Boolean = ANALYTICS_IS_ENABLE,
    private val analyticsFlushPeriod: Int = DEFAULT_ANALYTICS_FLUSH_PERIOD_SECONDS,
    private val cacheConfig: FlagsmithCacheConfigModel = FlagsmithCacheConfigModel(),
    private val defaultFlags: List<FlagModel> = emptyList(),
    private val errorHandler: ErrorHandler= DefaultErrorHandler(),
    private val requestTimeoutSeconds: Long = 4L,
    private val readTimeoutSeconds: Long = 6L,
    private val writeTimeoutSeconds: Long = 6L
) {

    private val retrofit: FlagsmithServices = FlagsmithRetrofitHelper.create(
        baseUrl,
        environmentKey,
        context,
        cacheConfig,
        requestTimeoutSeconds,
        readTimeoutSeconds,
        writeTimeoutSeconds
    )

    private val services = FlagsmithRemoteDataSource(
        retrofit, errorHandler = errorHandler
    )

    private val analytics: BluFlagsmithAnalytics? =
        if (!enableAnalytics) null
        else if (context != null) BluFlagsmithAnalytics(context, services, analyticsFlushPeriod)
        else throw IllegalArgumentException("Flagsmith requires a context to use the analytics feature")


    init {
        if (cacheConfig.enableCache && context == null)
            throw IllegalArgumentException("Flagsmith requires a context to use the cache feature")
    }

    companion object {
        const val ANALYTICS_IS_ENABLE = false
        const val DEFAULT_ANALYTICS_FLUSH_PERIOD_SECONDS = 10
    }

    suspend fun getFeatureFlags(identity: String? = null) =
        if (identity != null) {
            services.getIdentityFlagsAndTraits(identity)
        } else {
            throw IllegalArgumentException("Call getFlags if you cant set Identity")
        }

    suspend fun getFlags() =
        services.getFlags()


    suspend fun hasFeatureFlag(
        featureId: String,
        identity: String? = null
    ) = getFeatureFlag(featureId, identity)

    suspend fun getValueForFeature(
        featureId: String,
        identity: String? = null
    ) = getFeatureFlag(featureId, identity)

    suspend fun getTrait(id: String, identity: String) =
        services.getIdentityFlagsAndTraits(identity)

    suspend fun getTraits(identity: String) =
        services.getIdentityFlagsAndTraits(identity)

    suspend fun setTrait(trait: Trait, identity: String) =
        services.postTraits(TraitWithIdentityModel(trait.key, trait.value, IdentityModel(identity)))

    suspend fun getIdentity(identity: String) =
        services.getIdentityFlagsAndTraits(identity)


    suspend fun getFeatureFlag(
        featureId: String,
        identity: String?
    ) = getFeatureFlags(identity).apply {
        when (this) {
            is ResultEntity.Error -> {
                ResultEntity.Success(defaultFlags)
            }

            ResultEntity.Loading -> Unit
            is ResultEntity.Success -> {
                this.data.flags.find { flag -> flag.feature.name == featureId && flag.enabled }
                analytics?.trackEvent(featureId)
            }
        }
    }
}
