package fr.istic.mob.networkXY.model

import kotlinx.serialization.Serializable

@Serializable
data class Graph(
    val objects: MutableList<NetworkObject> = mutableListOf(),
    val connections: MutableList<Connection> = mutableListOf(),
    var selectedFloorPlan: Int = 0
) {
    fun addObject(obj: NetworkObject) {
        objects.add(obj)
    }
    
    fun removeObject(objId: String) {
        objects.removeAll { it.id == objId }
        connections.removeAll { it.fromObjectId == objId || it.toObjectId == objId }
    }
    
    fun updateObject(objId: String, label: String? = null, type: NodeType? = null, color: ObjectColor? = null, status: NodeStatus? = null) {
        objects.find { it.id == objId }?.let { obj ->
            label?.let { obj.label = it }
            type?.let { obj.type = it }
            color?.let { obj.color = it }
            status?.let { obj.status = it }
        }
    }
    
    fun moveObject(objId: String, newX: Float, newY: Float) {
        objects.find { it.id == objId }?.let { obj ->
            obj.x = newX
            obj.y = newY
        }
    }
    
    fun addConnection(conn: Connection): Boolean {
        if (conn.fromObjectId == conn.toObjectId) return false
        
        if (getConnectionsBetween(conn.fromObjectId, conn.toObjectId) != null) {
            return false
        }
        
        connections.add(conn)
        return true
    }
    
    fun removeConnection(connId: String) {
        connections.removeAll { it.id == connId }
    }
    
    fun updateConnection(connId: String, label: String? = null, connectionType: ConnectionType? = null, color: Int? = null, thickness: Float? = null) {
        connections.find { it.id == connId }?.let { conn ->
            label?.let { conn.label = it }
            connectionType?.let { conn.connectionType = it }
            color?.let { conn.color = it }
            thickness?.let { conn.thickness = it }
        }
    }
    
    fun updateConnectionCurvature(connId: String, curvature: Float) {
        connections.find { it.id == connId }?.let { conn ->
            conn.curvature = curvature
        }
    }
    
    fun getConnectionsBetween(obj1Id: String, obj2Id: String): Connection? {
        return connections.find { 
            (it.fromObjectId == obj1Id && it.toObjectId == obj2Id) ||
            (it.fromObjectId == obj2Id && it.toObjectId == obj1Id)
        }
    }
    
    fun getConnectionsForObject(objId: String): List<Connection> {
        return connections.filter { it.fromObjectId == objId || it.toObjectId == objId }
    }
    
    fun getObject(objId: String): NetworkObject? {
        return objects.find { it.id == objId }
    }
    
    fun clear() {
        objects.clear()
        connections.clear()
    }
}
