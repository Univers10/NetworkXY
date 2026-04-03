package fr.istic.mob.networkXY.model

import kotlinx.serialization.Serializable

@Serializable
enum class NodeType(val abbreviation: String) {
    ROUTEUR("R"),
    TV("TV"),
    ORDINATEUR("PC"),
    SMARTPHONE("SP"),
    TABLETTE("TB"),
    IMPRIMANTE("IM"),
    ENCEINTE("EN"),
    CAMERA("CA"),
    AUTRE("?");
}
