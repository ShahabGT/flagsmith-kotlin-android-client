package com.blu.flagsmith.util

import android.util.Log
import com.blu.flagsmith.util.ErrorEntity
import com.blu.flagsmith.util.ErrorHandler
import retrofit2.HttpException

class DefaultErrorHandler : ErrorHandler {
    override fun getError(throwable: Throwable): ErrorEntity {
        Log.i("FLAGSMITH", throwable.localizedMessage)
        return if (throwable is HttpException)
            ErrorEntity.ApiError(
                throwable.message(),
                throwable.code()
            )
        else ErrorEntity.Generic(throwable.localizedMessage)
    }
}
