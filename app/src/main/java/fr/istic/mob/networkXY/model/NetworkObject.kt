package fr.istic.mob.networkXY.model

import android.graphics.RectF
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NetworkObject(
    val id: String = UUID.randomUUID().toString(),
    var label: String,
    var x: Float,
    var y: Float,
    var type: NodeType = NodeType.AUTRE,
    var color: ObjectColor = ObjectColor.BLUE,
    var status: NodeStatus = NodeStatus.EN_LIGNE,
    val radius: Float = 40f
) {
    fun contains(touchX: Float, touchY: Float): Boolean {
        val dx = touchX - x
        val dy = touchY - y
        return (dx * dx + dy * dy) <= (radius * radius)
    }
    
    fun getBounds(): RectF {
        return RectF(
            x - radius,
            y - radius,
            x + radius,
            y + radius
        )
    }
    
    fun distanceTo(other: NetworkObject): Float {
        val dx = other.x - x
        val dy = other.y - y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
