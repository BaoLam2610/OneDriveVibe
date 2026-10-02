package com.lambao.odv.core.network

import java.io.IOException

internal actual fun Throwable.isNetworkFailure(): Boolean = this is IOException
