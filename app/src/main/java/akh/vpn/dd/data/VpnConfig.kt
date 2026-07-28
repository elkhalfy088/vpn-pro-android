package akh.vpn.dd.data

data class VpnConfig(
    val serverHost: String = "",
    val serverPort: Int = 8443,
    val decoyDomain: String = "www.facebook.com",
    val setupDone: Boolean = false
) {
    fun isValid(): Boolean =
        serverHost.isNotBlank() && serverPort in 1..65535
}
