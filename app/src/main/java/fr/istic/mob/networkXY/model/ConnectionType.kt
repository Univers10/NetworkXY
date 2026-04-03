package fr.istic.mob.networkXY.model

import android.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionType(val defaultColor: Int, val defaultThickness: Float) {
    WIFI(0xFF1565C0.toInt(), 4f),
    ETHERNET(0xFF2E7D32.toInt(), 6f),
    BLUETOOTH(0xFF6A1B9A.toInt(), 3f),
    USB(0xFFE65100.toInt(), 5f);
}
