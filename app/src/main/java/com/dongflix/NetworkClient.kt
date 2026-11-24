package com.dongflix

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
	private var client: OkHttpClient? = null

	@JvmStatic
	fun getInstance(): OkHttpClient {
		if (client == null) {
			client = OkHttpClient.Builder()
				.connectTimeout(15, TimeUnit.SECONDS)
				.readTimeout(20, TimeUnit.SECONDS)
				.build()
		}
		return client!!
	}
}
