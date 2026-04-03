package fr.istic.mob.networkXY.view

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import fr.istic.mob.networkXY.R
import fr.istic.mob.networkXY.model.Connection
import fr.istic.mob.networkXY.model.ConnectionType
import fr.istic.mob.networkXY.model.Graph
import fr.istic.mob.networkXY.model.NetworkObject
import fr.istic.mob.networkXY.model.NodeStatus
import fr.istic.mob.networkXY.model.NodeType
import fr.istic.mob.networkXY.viewmodel.NetworkViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class NetworkCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var graph: Graph? = null
    private var floorPlanBitmap: Bitmap? = null
    private var scaledFloorPlan: Bitmap? = null
    private var currentMode: NetworkViewModel.EditMode = NetworkViewModel.EditMode.VIEW

    // Canvas background
    private val canvasBgPaint = Paint().apply {
        color = 0xFFF0F2F6.toInt()
    }
    private val gridDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD4D8E0.toInt()
        style = Paint.Style.FILL
    }
    private val gridSpacing = 32f

    private val objectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val objectStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(60, 255, 255, 255)
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(30, 60, 80, 120)
        maskFilter = android.graphics.BlurMaskFilter(8f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        letterSpacing = 0.05f
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val statusStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.WHITE
    }

    private val connectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        color = 0xFF3A3F4B.toInt()
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 230
        style = Paint.Style.FILL
        setShadowLayer(4f, 0f, 1f, Color.argb(20, 0, 0, 0))
    }

    private val labelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(25, 0, 0, 0)
    }


    private var draggedObject: NetworkObject? = null
    private var draggedConnection: Connection? = null
    private var tempConnectionStart: NetworkObject? = null
    private var tempConnectionEnd: PointF? = null
    private var isDragging = false
    private var touchDownX = 0f
    private var touchDownY = 0f

    // Long press detection
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private val longPressTimeout = 500L
    private val touchSlop = 20f

    // Zoom & Pan state
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private val minScale = 0.5f
    private val maxScale = 4f
    private var isPanning = false
    private var lastPanX = 0f
    private var lastPanY = 0f
    private var isScaling = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            cancelPendingLongPress()
            return true
        }
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val focusX = detector.focusX
            val focusY = detector.focusY
            val oldScale = scaleFactor
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(minScale, min(maxScale, scaleFactor))
            val ds = scaleFactor / oldScale
            translateX = focusX - ds * (focusX - translateX)
            translateY = focusY - ds * (focusY - translateY)
            invalidate()
            onZoomChanged?.invoke(scaleFactor)
            return true
        }
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    })

    // Filter state
    private var filterObjectIds: Set<String>? = null  // null = no filter (show all)
    private var filterConnectionTypes: Set<ConnectionType>? = null
    private var highlightObjectIds: Set<String> = emptySet() // search matches get highlight ring
    private var isFilterActive = false

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = 0xFFFF9800.toInt() // orange highlight
    }

    fun setFilter(objectIds: Set<String>?, connectionTypes: Set<ConnectionType>?, searchMatchIds: Set<String> = emptySet()) {
        filterObjectIds = objectIds
        filterConnectionTypes = connectionTypes
        highlightObjectIds = searchMatchIds
        isFilterActive = objectIds != null || connectionTypes != null
        invalidate()
    }

    fun clearFilter() {
        filterObjectIds = null
        filterConnectionTypes = null
        highlightObjectIds = emptySet()
        isFilterActive = false
        invalidate()
    }

    var onObjectLongClick: ((NetworkObject) -> Unit)? = null
    var onConnectionLongClick: ((Connection) -> Unit)? = null
    var onEmptySpaceLongClick: ((Float, Float) -> Unit)? = null
    var onConnectionCreated: ((NetworkObject, NetworkObject) -> Unit)? = null
    var onObjectMoved: ((NetworkObject) -> Unit)? = null
    var onZoomChanged: ((Float) -> Unit)? = null

    fun isZoomed(): Boolean = scaleFactor != 1f || translateX != 0f || translateY != 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScaledFloorPlan()
    }

    private fun updateScaledFloorPlan() {
        // Keep original size for scrolling
        scaledFloorPlan = floorPlanBitmap
    }

    private fun screenToCanvasX(sx: Float): Float = (sx - translateX) / scaleFactor
    private fun screenToCanvasY(sy: Float): Float = (sy - translateY) / scaleFactor

    fun resetZoom() {
        scaleFactor = 1f
        translateX = 0f
        translateY = 0f
        invalidate()
        onZoomChanged?.invoke(scaleFactor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw canvas background with dot grid
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), canvasBgPaint)
        var gx = 0f
        while (gx < width) {
            var gy = 0f
            while (gy < height) {
                canvas.drawCircle(gx, gy, 1.2f, gridDotPaint)
                gy += gridSpacing
            }
            gx += gridSpacing
        }

        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)

        // Draw floor plan at origin for scrolling
        scaledFloorPlan?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        // Draw connections
        graph?.connections?.forEach { conn ->
            val connTypeMatch = filterConnectionTypes?.contains(conn.connectionType) ?: true
            val fromObj = graph?.getObject(conn.fromObjectId)
            val toObj = graph?.getObject(conn.toObjectId)
            val endpointsMatch = if (filterObjectIds != null && fromObj != null && toObj != null) {
                filterObjectIds!!.contains(fromObj.id) || filterObjectIds!!.contains(toObj.id)
            } else true
            val dimConn = isFilterActive && (!connTypeMatch || !endpointsMatch)
            if (dimConn) canvas.saveLayerAlpha(null, 40)
            drawConnection(canvas, conn)
            if (dimConn) canvas.restore()
        }

        // Draw temporary connection line
        drawTempConnection(canvas)

        // Draw objects on top
        graph?.objects?.forEach { obj ->
            val match = filterObjectIds?.contains(obj.id) ?: true
            val dimObj = isFilterActive && !match
            if (dimObj) canvas.saveLayerAlpha(null, 40)
            drawObject(canvas, obj)
            if (dimObj) canvas.restore()

            // Highlight search matches with orange ring
            if (highlightObjectIds.contains(obj.id)) {
                val r = obj.radius + 6f
                canvas.drawRoundRect(
                    obj.x - r, obj.y - r, obj.x + r, obj.y + r,
                    18f, 18f, highlightPaint
                )
            }
        }

        canvas.restore()
    }

    private fun drawObject(canvas: Canvas, obj: NetworkObject) {
        val r = obj.radius
        val cornerR = 16f
        val rect = RectF(obj.x - r, obj.y - r, obj.x + r, obj.y + r)

        // Soft shadow (offset down-right)
        val shadowRect = RectF(rect.left + 2f, rect.top + 4f, rect.right + 2f, rect.bottom + 4f)
        canvas.drawRoundRect(shadowRect, cornerR, cornerR, shadowPaint)

        // Node body with subtle darkened base
        val baseColor = if (obj.status == NodeStatus.HORS_LIGNE) {
            blendGray(obj.color.colorInt, 0.45f)
        } else {
            obj.color.colorInt
        }
        objectPaint.color = baseColor
        canvas.drawRoundRect(rect, cornerR, cornerR, objectPaint)

        // Subtle inner highlight (top edge light reflection)
        objectStrokePaint.color = Color.argb(50, 255, 255, 255)
        canvas.drawRoundRect(rect, cornerR, cornerR, objectStrokePaint)

        // Device icon centered inside node
        val iconDrawable = getIconForType(obj.type)
        iconDrawable?.let { drawable ->
            val iconSize = (r * 1.2f).toInt()
            val left = (obj.x - iconSize / 2f).toInt()
            val top = (obj.y - iconSize / 2f).toInt()
            drawable.setBounds(left, top, left + iconSize, top + iconSize)
            drawable.setTint(Color.WHITE)
            drawable.draw(canvas)
        }

        // Status dot (top-right corner) - slightly inset
        val dotRadius = 7f
        val dotX = obj.x + r - 6f
        val dotY = obj.y - r + 6f
        statusPaint.color = if (obj.status == NodeStatus.EN_LIGNE) 0xFF43A047.toInt() else 0xFFEF5350.toInt()
        canvas.drawCircle(dotX, dotY, dotRadius, statusPaint)
        canvas.drawCircle(dotX, dotY, dotRadius, statusStrokePaint)

        // Label below node with pill background
        val labelX = obj.x
        val labelY = obj.y + r + 22f
        val label = obj.label

        val textBounds = Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val halfW = textBounds.width() / 2f
        val padH = 10f
        val padV = 5f
        val pillRect = RectF(
            labelX - halfW - padH,
            labelY - textBounds.height() - padV,
            labelX + halfW + padH,
            labelY + padV
        )

        canvas.drawRoundRect(pillRect, 12f, 12f, labelBackgroundPaint)
        canvas.drawRoundRect(pillRect, 12f, 12f, labelBorderPaint)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, labelX, labelY, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun blendGray(color: Int, factor: Float): Int {
        val r = ((Color.red(color) * (1 - factor)) + (128 * factor)).toInt()
        val g = ((Color.green(color) * (1 - factor)) + (128 * factor)).toInt()
        val b = ((Color.blue(color) * (1 - factor)) + (128 * factor)).toInt()
        return Color.rgb(r, g, b)
    }

    private fun getIconForType(type: NodeType): Drawable? {
        val iconRes = when (type) {
            NodeType.ROUTEUR -> R.drawable.ic_router
            NodeType.TV -> R.drawable.ic_tv
            NodeType.ORDINATEUR -> R.drawable.ic_computer
            NodeType.SMARTPHONE -> R.drawable.ic_smartphone
            NodeType.TABLETTE -> R.drawable.ic_tablet
            NodeType.IMPRIMANTE -> R.drawable.ic_printer
            NodeType.ENCEINTE -> R.drawable.ic_speaker
            NodeType.CAMERA -> R.drawable.ic_camera
            NodeType.AUTRE -> R.drawable.ic_device_other
        }
        return ContextCompat.getDrawable(context, iconRes)
    }

    private fun drawConnection(canvas: Canvas, conn: Connection) {
        val fromObj = graph?.getObject(conn.fromObjectId) ?: return
        val toObj = graph?.getObject(conn.toObjectId) ?: return

        val bothOnline = fromObj.status == NodeStatus.EN_LIGNE && toObj.status == NodeStatus.EN_LIGNE

        connectionPaint.color = if (bothOnline) conn.color else Color.LTGRAY
        connectionPaint.strokeWidth = conn.thickness

        if (!bothOnline) {
            connectionPaint.pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        } else {
            connectionPaint.pathEffect = when (conn.connectionType) {
                ConnectionType.WIFI -> null
                ConnectionType.ETHERNET -> null
                ConnectionType.BLUETOOTH -> DashPathEffect(floatArrayOf(6f, 6f), 0f)
                ConnectionType.USB -> DashPathEffect(floatArrayOf(16f, 6f, 4f, 6f), 0f)
            }
        }

        val path = conn.getPath(fromObj, toObj)
        canvas.drawPath(path, connectionPaint)
        connectionPaint.pathEffect = null

        // Label at midpoint with pill
        val midPoint = conn.getMidPoint(fromObj, toObj)
        val displayLabel = if (conn.label.isNotEmpty()) conn.label else conn.connectionType.name
        val textBounds = Rect()
        textPaint.getTextBounds(displayLabel, 0, displayLabel.length, textBounds)

        val lx = midPoint.x + 10
        val ly = midPoint.y - 5
        val pillRect = RectF(
            lx - 8f,
            ly - textBounds.height() - 6f,
            lx + textBounds.width() + 8f,
            ly + 6f
        )

        canvas.drawRoundRect(pillRect, 10f, 10f, labelBackgroundPaint)
        canvas.drawRoundRect(pillRect, 10f, 10f, labelBorderPaint)
        canvas.drawText(displayLabel, lx, ly, textPaint)
    }

    private fun drawTempConnection(canvas: Canvas) {
        val start = tempConnectionStart ?: return
        val end = tempConnectionEnd ?: return

        connectionPaint.color = 0xFF90A4AE.toInt()
        connectionPaint.strokeWidth = 3f
        connectionPaint.pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)

        canvas.drawLine(start.x, start.y, end.x, end.y, connectionPaint)

        connectionPaint.pathEffect = null
    }

    fun setGraph(graph: Graph) {
        this.graph = graph
        invalidate()
    }

    fun setFloorPlan(bitmap: Bitmap) {
        this.floorPlanBitmap = bitmap
        updateScaledFloorPlan()
        invalidate()
    }

    fun setMode(mode: NetworkViewModel.EditMode) {
        this.currentMode = mode
        tempConnectionStart = null
        tempConnectionEnd = null
        draggedObject = null
        draggedConnection = null
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val pointerCount = event.pointerCount

        // Two-finger pan
        if (pointerCount == 2 && !scaleDetector.isInProgress) {
            val midX = (event.getX(0) + event.getX(1)) / 2f
            val midY = (event.getY(0) + event.getY(1)) / 2f
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    isPanning = true
                    lastPanX = midX
                    lastPanY = midY
                    cancelPendingLongPress()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPanning) {
                        translateX += midX - lastPanX
                        translateY += midY - lastPanY
                        lastPanX = midX
                        lastPanY = midY
                        invalidate()
                    }
                }
            }
            return true
        }

        if (isScaling || isPanning) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                isPanning = false
                isScaling = false
            }
            return true
        }

        // Single-finger: convert to canvas coordinates
        val cx = screenToCanvasX(event.x)
        val cy = screenToCanvasY(event.y)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = cx
                touchDownY = cy
                isDragging = false
                handleTouchDown(cx, cy)

                // If no object/connection grabbed in VIEW mode, start pan
                if (currentMode == NetworkViewModel.EditMode.VIEW && draggedObject == null) {
                    isPanning = true
                    lastPanX = event.x
                    lastPanY = event.y
                }

                schedulePendingLongPress(cx, cy)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = cx - touchDownX
                val dy = cy - touchDownY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance > touchSlop / scaleFactor) {
                    cancelPendingLongPress()
                    isDragging = true

                    if (isPanning && draggedObject == null) {
                        translateX += event.x - lastPanX
                        translateY += event.y - lastPanY
                        lastPanX = event.x
                        lastPanY = event.y
                        invalidate()
                    } else {
                        handleTouchMove(cx, cy)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                cancelPendingLongPress()
                isPanning = false
                handleTouchUp(cx, cy)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelPendingLongPress()
                isPanning = false
                draggedObject = null
                draggedConnection = null
                tempConnectionStart = null
                tempConnectionEnd = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun schedulePendingLongPress(x: Float, y: Float) {
        cancelPendingLongPress()
        longPressRunnable = Runnable {
            if (!isDragging) {
                handleLongPress(x, y)
            }
        }
        longPressHandler.postDelayed(longPressRunnable!!, longPressTimeout)
    }

    private fun cancelPendingLongPress() {
        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun handleTouchDown(x: Float, y: Float) {
        when (currentMode) {
            NetworkViewModel.EditMode.ADD_CONNECTION -> {
                tempConnectionStart = findObjectAt(x, y)
                if (tempConnectionStart != null) {
                    tempConnectionEnd = PointF(x, y)
                }
            }
            NetworkViewModel.EditMode.EDIT -> {
                draggedObject = findObjectAt(x, y)
                if (draggedObject == null) {
                    draggedConnection = findConnectionAt(x, y)
                }
            }
            NetworkViewModel.EditMode.VIEW, NetworkViewModel.EditMode.ADD_OBJECT -> {
                // Allow dragging objects in any mode
                draggedObject = findObjectAt(x, y)
            }
        }
    }

    private fun handleTouchMove(x: Float, y: Float) {
        when {
            // Drag object (works in all modes)
            draggedObject != null -> {
                draggedObject?.let {
                    it.x = x
                    it.y = y
                    invalidate()
                }
            }
            // Drag connection curvature (EDIT mode only)
            draggedConnection != null && currentMode == NetworkViewModel.EditMode.EDIT -> {
                val conn = draggedConnection ?: return
                val fromObj = graph?.getObject(conn.fromObjectId) ?: return
                val toObj = graph?.getObject(conn.toObjectId) ?: return

                conn.curvature = conn.calculateCurvatureFromPoint(x, y, fromObj, toObj)
                invalidate()
            }
            // Drag to create connection
            tempConnectionStart != null -> {
                tempConnectionEnd = PointF(x, y)
                invalidate()
            }
        }
    }

    private fun handleTouchUp(x: Float, y: Float) {
        // Notify ViewModel of object position change after drag
        draggedObject?.let { obj ->
            if (isDragging) {
                onObjectMoved?.invoke(obj)
            }
        }
        draggedObject = null
        draggedConnection = null

        if (tempConnectionStart != null) {
            val endObject = findObjectAt(x, y)
            if (endObject != null && endObject != tempConnectionStart) {
                onConnectionCreated?.invoke(tempConnectionStart!!, endObject)
            }
            tempConnectionStart = null
            tempConnectionEnd = null
            invalidate()
        }
    }

    private fun handleLongPress(x: Float, y: Float) {
        // Check if long-press on an object
        val obj = findObjectAt(x, y)
        if (obj != null) {
            // Cancel any ongoing drag
            draggedObject = null
            onObjectLongClick?.invoke(obj)
            return
        }

        // Check if long-press on a connection
        val conn = findConnectionAt(x, y)
        if (conn != null) {
            onConnectionLongClick?.invoke(conn)
            return
        }

        // Long-press on empty space in ADD_OBJECT mode => create object
        if (currentMode == NetworkViewModel.EditMode.ADD_OBJECT) {
            onEmptySpaceLongClick?.invoke(x, y)
        }
    }

    private fun findObjectAt(x: Float, y: Float): NetworkObject? {
        return graph?.objects?.findLast { it.contains(x, y) }
    }

    private fun findConnectionAt(x: Float, y: Float): Connection? {
        return graph?.connections?.findLast { conn ->
            val fromObj = graph?.getObject(conn.fromObjectId) ?: return@findLast false
            val toObj = graph?.getObject(conn.toObjectId) ?: return@findLast false
            conn.isNearMidPoint(x, y, fromObj, toObj)
        }
    }
}
