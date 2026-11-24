package com.dongflix

import android.app.Activity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class RequestNetwork(private val activity: Activity) {

	private val params = HashMap<String, Any>()
	private val headers = HashMap<String, Any>()
	private val requestType = 0

	fun startRequestNetwork(
		method: String,
		url: String,
		tag: String,
		requestListener: RequestListener
	) {
		val client = NetworkClient.getInstance()
		val reqBuilder = Request.Builder().url(url)

		if (method == "GET") {
			reqBuilder.get()
		}

		client.newCall(reqBuilder.build()).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				activity.runOnUiThread {
					requestListener.onErrorResponse(tag, e.message ?: "Unknown network error")
				}
			}

			@Throws(IOException::class)
			override fun onResponse(call: Call, response: Response) {
				val body = response.body?.string() ?: ""
				activity.runOnUiThread {
					requestListener.onResponse(tag, body, HashMap())
				}
			}
		})
	}

	interface RequestListener {
		fun onResponse(tag: String, response: String, responseHeaders: HashMap<String, Any>)
		fun onErrorResponse(tag: String, message: String)
	}
}
