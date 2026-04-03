package fr.istic.mob.networkXY.model

import android.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class ObjectColor(val colorInt: Int) {
    RED(Color.RED),
    GREEN(Color.GREEN),
    BLUE(Color.BLUE),
    ORANGE(0xFFFFA500.toInt()),
    CYAN(Color.CYAN),
    MAGENTA(Color.MAGENTA),
    BLACK(Color.BLACK);
    
    companion object {
        fun fromInt(color: Int): ObjectColor {
            return values().find { it.colorInt == color } ?: BLUE
        }
    }
}
