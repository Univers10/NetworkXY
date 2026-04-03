package fr.istic.mob.networkXY.model

import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.sqrt

@Serializable
data class Connection(
    val id: String = UUID.randomUUID().toString(),
    val fromObjectId: String,
    val toObjectId: String,
    var label: String = "",
    var connectionType: ConnectionType = ConnectionType.WIFI,
    var color: Int = Color.BLACK,
    var thickness: Float = 5f,
    var curvature: Float = 0f
) {
    fun getPath(fromObj: NetworkObject, toObj: NetworkObject): Path {
        val path = Path()
        path.moveTo(fromObj.x, fromObj.y)
        
        if (curvature == 0f || fromObj.distanceTo(toObj) < 10f) {
            path.lineTo(toObj.x, toObj.y)
        } else {
            val controlPoint = getControlPoint(fromObj, toObj)
            path.quadTo(controlPoint.x, controlPoint.y, toObj.x, toObj.y)
        }
        
        return path
    }
    
    fun getControlPoint(fromObj: NetworkObject, toObj: NetworkObject): PointF {
        val midX = (fromObj.x + toObj.x) / 2
        val midY = (fromObj.y + toObj.y) / 2
        
        val dx = toObj.x - fromObj.x
        val dy = toObj.y - fromObj.y
        val length = sqrt(dx * dx + dy * dy)
        
        if (length < 0.01f) return PointF(midX, midY)
        
        val perpX = -dy / length
        val perpY = dx / length
        
        return PointF(
            midX + perpX * curvature,
            midY + perpY * curvature
        )
    }
    
    fun getMidPoint(fromObj: NetworkObject, toObj: NetworkObject): PointF {
        val path = getPath(fromObj, toObj)
        val measure = PathMeasure(path, false)
        val coords = FloatArray(2)
        measure.getPosTan(measure.length / 2, coords, null)
        return PointF(coords[0], coords[1])
    }
    
    fun isNearMidPoint(x: Float, y: Float, fromObj: NetworkObject, toObj: NetworkObject, threshold: Float = 50f): Boolean {
        val midPoint = getMidPoint(fromObj, toObj)
        val dx = x - midPoint.x
        val dy = y - midPoint.y
        return (dx * dx + dy * dy) <= (threshold * threshold)
    }
    
    fun calculateCurvatureFromPoint(touchX: Float, touchY: Float, fromObj: NetworkObject, toObj: NetworkObject): Float {
        val midX = (fromObj.x + toObj.x) / 2
        val midY = (fromObj.y + toObj.y) / 2
        
        val dx = toObj.x - fromObj.x
        val dy = toObj.y - fromObj.y
        val length = sqrt(dx * dx + dy * dy)
        
        if (length < 0.01f) return 0f
        
        val perpX = -dy / length
        val perpY = dx / length
        
        val vectorToTouchX = touchX - midX
        val vectorToTouchY = touchY - midY
        
        val dotProduct = vectorToTouchX * perpX + vectorToTouchY * perpY
        
        return dotProduct
    }
}
