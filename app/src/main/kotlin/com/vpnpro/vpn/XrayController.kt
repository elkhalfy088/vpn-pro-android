package com.vpnpro.vpn

import android.content.Context
import android.util.Log
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Controls the Xray-core engine via AndroidLibXrayLite.
 */
object XrayController {

    private const val TAG = "XrayController"
    private var controller: CoreController? = null

    val isRunning: Boolean
        get() = try { controller?.isRunning == true } catch (_: Exception) { false }

    fun init(context: Context) {
        try {
            val envPath = context.filesDir.absolutePath
            Libv2ray.initCoreEnv(envPath, "xray")
            Log.i(TAG, "initCoreEnv done: $envPath | core=${Libv2ray.checkVersionX()}")
        } catch (e: Exception) {
            Log.e(TAG, "init failed: ${e.message}")
        }
    }

    fun start(configJson: String, tunFd: Int, handler: CoreCallbackHandler) {
        stopInternal()
        try {
            val ctrl = Libv2ray.newCoreController(handler)
            ctrl.startLoop(configJson, tunFd.toLong())
            controller = ctrl
            Log.i(TAG, "Xray startLoop called, tunFd=$tunFd")
        } catch (e: Exception) {
            Log.e(TAG, "start failed: ${e.message}")
            throw e
        }
    }

    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {
        try {
            controller?.stopLoop()
        } catch (e: Exception) {
            Log.w(TAG, "stopLoop error: ${e.message}")
        }
        controller = null
    }
}
