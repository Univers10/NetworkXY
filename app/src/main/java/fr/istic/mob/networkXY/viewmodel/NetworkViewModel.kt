package fr.istic.mob.networkXY.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fr.istic.mob.networkXY.model.Connection
import fr.istic.mob.networkXY.model.ConnectionType
import fr.istic.mob.networkXY.model.Graph
import fr.istic.mob.networkXY.model.NetworkObject
import fr.istic.mob.networkXY.model.NodeStatus
import fr.istic.mob.networkXY.model.NodeType
import fr.istic.mob.networkXY.model.ObjectColor
import fr.istic.mob.networkXY.repository.NetworkRepository
import kotlinx.coroutines.launch

class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = NetworkRepository(application)
    
    private val _graph = MutableLiveData<Graph>(Graph())
    val graph: LiveData<Graph> = _graph
    
    private val _currentMode = MutableLiveData<EditMode>(EditMode.VIEW)
    val currentMode: LiveData<EditMode> = _currentMode
    
    private val _selectedFloorPlan = MutableLiveData<Int>(0)
    val selectedFloorPlan: LiveData<Int> = _selectedFloorPlan
    
    private val _saveStatus = MutableLiveData<String>()
    val saveStatus: LiveData<String> = _saveStatus
    
    enum class EditMode {
        VIEW,
        ADD_OBJECT,
        ADD_CONNECTION,
        EDIT
    }
    
    fun addObject(label: String, x: Float, y: Float, type: NodeType = NodeType.AUTRE, color: ObjectColor = ObjectColor.BLUE, status: NodeStatus = NodeStatus.EN_LIGNE) {
        val currentGraph = _graph.value ?: Graph()
        val newObject = NetworkObject(
            label = label,
            x = x,
            y = y,
            type = type,
            color = color,
            status = status
        )
        currentGraph.addObject(newObject)
        _graph.value = currentGraph
    }
    
    fun removeObject(objId: String) {
        val currentGraph = _graph.value ?: return
        currentGraph.removeObject(objId)
        _graph.value = currentGraph
    }
    
    fun updateObject(objId: String, label: String? = null, type: NodeType? = null, color: ObjectColor? = null, status: NodeStatus? = null) {
        val currentGraph = _graph.value ?: return
        currentGraph.updateObject(objId, label, type, color, status)
        _graph.value = currentGraph
    }
    
    fun moveObject(objId: String, newX: Float, newY: Float) {
        val currentGraph = _graph.value ?: return
        currentGraph.moveObject(objId, newX, newY)
        _graph.value = currentGraph
    }
    
    fun addConnection(fromId: String, toId: String, label: String = "", connectionType: ConnectionType = ConnectionType.WIFI, color: Int = connectionType.defaultColor, thickness: Float = connectionType.defaultThickness): Boolean {
        val currentGraph = _graph.value ?: return false
        val newConnection = Connection(
            fromObjectId = fromId,
            toObjectId = toId,
            label = label,
            connectionType = connectionType,
            color = color,
            thickness = thickness
        )
        val success = currentGraph.addConnection(newConnection)
        if (success) {
            _graph.value = currentGraph
        }
        return success
    }
    
    fun removeConnection(connId: String) {
        val currentGraph = _graph.value ?: return
        currentGraph.removeConnection(connId)
        _graph.value = currentGraph
    }
    
    fun updateConnection(connId: String, label: String? = null, connectionType: ConnectionType? = null, color: Int? = null, thickness: Float? = null) {
        val currentGraph = _graph.value ?: return
        currentGraph.updateConnection(connId, label, connectionType, color, thickness)
        _graph.value = currentGraph
    }
    
    fun updateConnectionCurvature(connId: String, curvature: Float) {
        val currentGraph = _graph.value ?: return
        currentGraph.updateConnectionCurvature(connId, curvature)
        _graph.value = currentGraph
    }
    
    fun setMode(mode: EditMode) {
        _currentMode.value = mode
    }
    
    fun resetGraph() {
        _graph.value = Graph(selectedFloorPlan = _selectedFloorPlan.value ?: 0)
    }
    
    fun setFloorPlan(planIndex: Int) {
        _selectedFloorPlan.value = planIndex
        val currentGraph = _graph.value ?: Graph()
        currentGraph.selectedFloorPlan = planIndex
        _graph.value = currentGraph
    }
    
    fun saveGraph() {
        viewModelScope.launch {
            val currentGraph = _graph.value ?: return@launch
            val success = repository.saveGraph(currentGraph)
            _saveStatus.value = if (success) "saved" else "error"
        }
    }
    
    fun loadGraph() {
        viewModelScope.launch {
            val loadedGraph = repository.loadGraph()
            if (loadedGraph != null) {
                _graph.value = loadedGraph
                _selectedFloorPlan.value = loadedGraph.selectedFloorPlan
                _saveStatus.value = "loaded"
            } else {
                _saveStatus.value = "error"
            }
        }
    }
    
    fun hasSavedGraph(): Boolean {
        return repository.hasSavedGraph()
    }

    fun exportGraphJson(): String? {
        val currentGraph = _graph.value ?: return null
        return repository.exportGraphToJson(currentGraph)
    }

    fun importGraphJson(jsonString: String): Boolean {
        val importedGraph = repository.importGraphFromJson(jsonString) ?: return false
        _graph.value = importedGraph
        _selectedFloorPlan.value = importedGraph.selectedFloorPlan
        return true
    }
}
