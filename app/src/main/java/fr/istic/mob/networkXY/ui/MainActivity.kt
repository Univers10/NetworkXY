package fr.istic.mob.networkXY.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import fr.istic.mob.networkXY.R
import fr.istic.mob.networkXY.model.ConnectionType
import fr.istic.mob.networkXY.model.NodeStatus
import fr.istic.mob.networkXY.model.NodeType
import fr.istic.mob.networkXY.ui.dialogs.ConnectionDialog
import fr.istic.mob.networkXY.ui.dialogs.ObjectDialog
import fr.istic.mob.networkXY.utils.ScreenshotUtil
import fr.istic.mob.networkXY.view.NetworkCanvas
import fr.istic.mob.networkXY.viewmodel.NetworkViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: NetworkViewModel by viewModels()
    private lateinit var coordinatorRoot: CoordinatorLayout
    private lateinit var networkCanvas: NetworkCanvas
    private lateinit var chipMode: Chip
    private lateinit var fabMode: FloatingActionButton
    private lateinit var btnResetZoom: MaterialButton
    private lateinit var importJsonLauncher: ActivityResultLauncher<Array<String>>

    private val floorPlans = listOf(
        R.drawable.plan1,
        R.drawable.plan2,
        R.drawable.plan3,
        R.drawable.plan4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerImportLauncher()

        coordinatorRoot = findViewById(R.id.coordinator_root)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        networkCanvas = findViewById(R.id.network_canvas)
        chipMode = findViewById(R.id.chip_mode)
        fabMode = findViewById(R.id.fab_mode)
        btnResetZoom = findViewById(R.id.btn_reset_zoom)

        btnResetZoom.setOnClickListener {
            networkCanvas.resetZoom()
        }

        setupToolbarMenu(toolbar)
        setupFab()
        setupObservers()
        setupCanvasCallbacks()

        if (savedInstanceState == null) {
            loadDefaultFloorPlan()
            showNavigationGuideIfNeeded()
        }
    }

    private fun showNavigationGuideIfNeeded() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        // Reset to show guide again after app update
        prefs.edit().remove("has_seen_navigation_guide").apply()
        val hasSeenGuide = prefs.getBoolean("has_seen_navigation_guide", false)
        
        if (!hasSeenGuide) {
            showNavigationGuide()
        }
    }

    private fun showNavigationGuide() {
        val guideOverlay = findViewById<View>(R.id.navigation_guide_overlay)
        val btnGotIt = guideOverlay.findViewById<MaterialButton>(R.id.btn_got_it)
        
        guideOverlay.visibility = View.VISIBLE
        
        btnGotIt.setOnClickListener {
            guideOverlay.visibility = View.GONE
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("has_seen_navigation_guide", true).apply()
        }
    }

    private fun setupToolbarMenu(toolbar: MaterialToolbar) {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_reset -> { showResetConfirmation(); true }
                R.id.menu_save -> { viewModel.saveGraph(); true }
                R.id.menu_load -> {
                    if (viewModel.hasSavedGraph()) {
                        viewModel.loadGraph()
                    } else {
                        Snackbar.make(coordinatorRoot, R.string.no_saved_network, Snackbar.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.menu_email -> { sendNetworkByEmail(); true }
                R.id.menu_mode_view -> { viewModel.setMode(NetworkViewModel.EditMode.VIEW); true }
                R.id.menu_mode_add_object -> { viewModel.setMode(NetworkViewModel.EditMode.ADD_OBJECT); true }
                R.id.menu_mode_add_connection -> { viewModel.setMode(NetworkViewModel.EditMode.ADD_CONNECTION); true }
                R.id.menu_mode_edit -> { viewModel.setMode(NetworkViewModel.EditMode.EDIT); true }
                R.id.menu_filter -> { showFilterBottomSheet(); true }
                R.id.menu_stats -> { showNetworkStats(); true }
                R.id.menu_language -> { showLanguageDialog(); true }
                R.id.menu_select_floor_plan -> { showFloorPlanSelector(); true }
                else -> false
            }
        }
    }

    private fun setupFab() {
        fabMode.setOnClickListener {
            showModeBottomSheet()
        }
    }

    private fun showModeBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_modes, null)
        bottomSheet.setContentView(view)

        val currentMode = viewModel.currentMode.value ?: NetworkViewModel.EditMode.VIEW

        val btnView = view.findViewById<MaterialButton>(R.id.btn_mode_view)
        val btnAddObj = view.findViewById<MaterialButton>(R.id.btn_mode_add_object)
        val btnAddConn = view.findViewById<MaterialButton>(R.id.btn_mode_add_connection)
        val btnEdit = view.findViewById<MaterialButton>(R.id.btn_mode_edit)

        val allButtons = listOf(
            btnView to NetworkViewModel.EditMode.VIEW,
            btnAddObj to NetworkViewModel.EditMode.ADD_OBJECT,
            btnAddConn to NetworkViewModel.EditMode.ADD_CONNECTION,
            btnEdit to NetworkViewModel.EditMode.EDIT
        )
        for ((btn, mode) in allButtons) {
            if (mode == currentMode) {
                btn.setBackgroundColor(getColor(R.color.md_theme_primary))
                btn.setTextColor(getColor(R.color.md_theme_on_primary))
                btn.setIconTintResource(R.color.md_theme_on_primary)
            } else {
                btn.alpha = 0.75f
            }
        }

        btnView.setOnClickListener {
            viewModel.setMode(NetworkViewModel.EditMode.VIEW)
            bottomSheet.dismiss()
        }
        btnAddObj.setOnClickListener {
            viewModel.setMode(NetworkViewModel.EditMode.ADD_OBJECT)
            bottomSheet.dismiss()
        }
        btnAddConn.setOnClickListener {
            viewModel.setMode(NetworkViewModel.EditMode.ADD_CONNECTION)
            bottomSheet.dismiss()
        }
        btnEdit.setOnClickListener {
            viewModel.setMode(NetworkViewModel.EditMode.EDIT)
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    private fun setupObservers() {
        viewModel.graph.observe(this) { graph ->
            networkCanvas.setGraph(graph)
        }

        viewModel.selectedFloorPlan.observe(this) { planIndex ->
            if (planIndex in floorPlans.indices) {
                val bitmap = loadBitmap(floorPlans[planIndex])
                if (bitmap != null) {
                    networkCanvas.setFloorPlan(bitmap)
                }
            }
        }

        viewModel.currentMode.observe(this) { mode ->
            networkCanvas.setMode(mode)
            updateModeChip(mode)
        }

        viewModel.saveStatus.observe(this) { status ->
            when (status) {
                "saved" -> Snackbar.make(coordinatorRoot, R.string.saved, Snackbar.LENGTH_SHORT).show()
                "loaded" -> Snackbar.make(coordinatorRoot, R.string.loaded, Snackbar.LENGTH_SHORT).show()
                "error" -> Snackbar.make(coordinatorRoot, R.string.error_save, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateModeChip(mode: NetworkViewModel.EditMode) {
        when (mode) {
            NetworkViewModel.EditMode.VIEW -> {
                chipMode.text = getString(R.string.mode_view)
                chipMode.setChipIconResource(R.drawable.ic_visibility)
            }
            NetworkViewModel.EditMode.ADD_OBJECT -> {
                chipMode.text = getString(R.string.mode_add_object)
                chipMode.setChipIconResource(R.drawable.ic_add_circle)
            }
            NetworkViewModel.EditMode.ADD_CONNECTION -> {
                chipMode.text = getString(R.string.mode_add_connection)
                chipMode.setChipIconResource(R.drawable.ic_link)
            }
            NetworkViewModel.EditMode.EDIT -> {
                chipMode.text = getString(R.string.mode_edit)
                chipMode.setChipIconResource(R.drawable.ic_edit)
            }
        }
    }

    private fun setupCanvasCallbacks() {
        networkCanvas.onObjectLongClick = { obj ->
            ObjectDialog.showEditDialog(
                this,
                obj,
                onObjectUpdated = { label, type, color, status ->
                    viewModel.updateObject(obj.id, label, type, color, status)
                },
                onObjectDeleted = {
                    viewModel.removeObject(obj.id)
                }
            )
        }

        networkCanvas.onConnectionLongClick = { conn ->
            ConnectionDialog.showEditDialog(
                this,
                conn,
                onConnectionUpdated = { label, connType, color, thickness ->
                    viewModel.updateConnection(conn.id, label, connType, color, thickness)
                },
                onConnectionDeleted = {
                    viewModel.removeConnection(conn.id)
                }
            )
        }

        networkCanvas.onEmptySpaceLongClick = { x, y ->
            if (viewModel.currentMode.value == NetworkViewModel.EditMode.ADD_OBJECT) {
                ObjectDialog.showAddDialog(this, x, y) { label, type, color, status ->
                    viewModel.addObject(label, x, y, type, color, status)
                }
            }
        }

        networkCanvas.onConnectionCreated = { fromObj, toObj ->
            ConnectionDialog.showAddDialog(this) { label, connType, color ->
                val success = viewModel.addConnection(fromObj.id, toObj.id, label, connType, color)
                if (!success) {
                    Snackbar.make(coordinatorRoot, R.string.connection_exists, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        networkCanvas.onObjectMoved = { obj ->
            viewModel.moveObject(obj.id, obj.x, obj.y)
        }

        networkCanvas.onZoomChanged = { _ ->
            btnResetZoom.visibility = if (networkCanvas.isZoomed()) View.VISIBLE else View.GONE
        }
    }

    private fun loadDefaultFloorPlan() {
        viewModel.setFloorPlan(0)
    }

    private fun showResetConfirmation() {
        val graph = viewModel.graph.value
        val objCount = graph?.objects?.size ?: 0
        val connCount = graph?.connections?.size ?: 0
        val message = getString(R.string.confirm_reset_detail, objCount, connCount)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_reset)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ ->
                viewModel.resetGraph()
                Snackbar.make(coordinatorRoot, R.string.reset_done, Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sendNetworkByEmail() {
        lifecycleScope.launch {
            val screenshot = ScreenshotUtil.takeScreenshot(networkCanvas)
            val uri = ScreenshotUtil.saveToCache(this@MainActivity, screenshot)

            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_network)))
            } else {
                Snackbar.make(coordinatorRoot, R.string.error_save, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFloorPlanSelector() {
        val planNames = arrayOf(
            getString(R.string.floor_plan_1),
            getString(R.string.floor_plan_2),
            getString(R.string.floor_plan_3),
            getString(R.string.floor_plan_4)
        )

        val currentPlan = viewModel.selectedFloorPlan.value ?: 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_select_floor_plan)
            .setSingleChoiceItems(planNames, currentPlan) { dialog, which ->
                viewModel.setFloorPlan(which)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showNetworkStats() {
        val graph = viewModel.graph.value ?: return
        val objects = graph.objects
        val connections = graph.connections

        if (objects.isEmpty()) {
            Snackbar.make(coordinatorRoot, R.string.stats_empty, Snackbar.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_stats, null)
        bottomSheet.setContentView(view)

        val totalDevices = objects.size
        val onlineCount = objects.count { it.status == NodeStatus.EN_LIGNE }
        val offlineCount = totalDevices - onlineCount

        view.findViewById<TextView>(R.id.stats_total_devices).text =
            getString(R.string.stats_total_devices, totalDevices)
        view.findViewById<TextView>(R.id.stats_online_devices).text =
            getString(R.string.stats_online, onlineCount)
        view.findViewById<TextView>(R.id.stats_offline_devices).text =
            getString(R.string.stats_offline, offlineCount)

        val typeBreakdown = objects.groupBy { it.type }.entries.joinToString("\n") { (type, list) ->
            val name = when (type) {
                NodeType.ROUTEUR -> getString(R.string.type_routeur)
                NodeType.TV -> getString(R.string.type_tv)
                NodeType.ORDINATEUR -> getString(R.string.type_ordinateur)
                NodeType.SMARTPHONE -> getString(R.string.type_smartphone)
                NodeType.TABLETTE -> getString(R.string.type_tablette)
                NodeType.IMPRIMANTE -> getString(R.string.type_imprimante)
                NodeType.ENCEINTE -> getString(R.string.type_enceinte)
                NodeType.CAMERA -> getString(R.string.type_camera)
                NodeType.AUTRE -> getString(R.string.type_autre)
            }
            "  $name : ${list.size}"
        }
        view.findViewById<TextView>(R.id.stats_device_types).text = typeBreakdown

        val totalConnections = connections.size
        view.findViewById<TextView>(R.id.stats_total_connections).text =
            getString(R.string.stats_total_connections, totalConnections)

        val connBreakdown = if (connections.isNotEmpty()) {
            connections.groupBy { it.connectionType }.entries.joinToString("\n") { (type, list) ->
                val name = when (type) {
                    ConnectionType.WIFI -> getString(R.string.conn_type_wifi)
                    ConnectionType.ETHERNET -> getString(R.string.conn_type_ethernet)
                    ConnectionType.BLUETOOTH -> getString(R.string.conn_type_bluetooth)
                    ConnectionType.USB -> getString(R.string.conn_type_usb)
                }
                "  $name : ${list.size}"
            }
        } else ""
        view.findViewById<TextView>(R.id.stats_connection_types).text = connBreakdown

        bottomSheet.show()
    }

    private fun registerImportLauncher() {
        importJsonLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().readText()
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.menu_import_json)
                        .setMessage(R.string.import_json_confirm)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val success = viewModel.importGraphJson(jsonString)
                            if (success) {
                                val g = viewModel.graph.value
                                val msg = getString(R.string.import_json_success, g?.objects?.size ?: 0, g?.connections?.size ?: 0)
                                Snackbar.make(coordinatorRoot, msg, Snackbar.LENGTH_LONG).show()
                            } else {
                                Snackbar.make(coordinatorRoot, R.string.import_json_error, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            } catch (e: Exception) {
                Snackbar.make(coordinatorRoot, R.string.import_json_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportJsonShare() {
        val jsonString = viewModel.exportGraphJson()
        if (jsonString == null) {
            Snackbar.make(coordinatorRoot, R.string.export_json_empty, Snackbar.LENGTH_SHORT).show()
            return
        }
        try {
            val cachePath = java.io.File(cacheDir, "json")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "network_export.json")
            file.writeText(jsonString)

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.export_json_share)))
        } catch (e: Exception) {
            Snackbar.make(coordinatorRoot, R.string.error_save, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun importJsonFromFile() {
        importJsonLauncher.launch(arrayOf("*/*"))
    }

    private fun exportPngShare() {
        lifecycleScope.launch {
            val bitmap = ScreenshotUtil.takeScreenshot(networkCanvas)
            val uri = ScreenshotUtil.saveToCache(this@MainActivity, bitmap)
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.export_png_share)))
            } else {
                Snackbar.make(coordinatorRoot, R.string.error_save, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLanguageDialog() {
        val currentLocale = resources.configuration.locales[0].language
        val items = arrayOf(getString(R.string.language_french), getString(R.string.language_english))
        val checked = if (currentLocale == "fr") 0 else 1

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.menu_language)
            .setSingleChoiceItems(items, checked) { dialog, which ->
                val newLocale = if (which == 0) "fr" else "en"
                if (newLocale != currentLocale) {
                    val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(newLocale)
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFilterBottomSheet() {
        val graph = viewModel.graph.value ?: return
        val objects = graph.objects
        if (objects.isEmpty()) {
            Snackbar.make(coordinatorRoot, R.string.stats_empty, Snackbar.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_filter, null)
        bottomSheet.setContentView(view)

        val searchInput = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.filter_search_input)
        val chipStatusAll = view.findViewById<Chip>(R.id.chip_status_all)
        val chipStatusOnline = view.findViewById<Chip>(R.id.chip_status_online)
        val chipStatusOffline = view.findViewById<Chip>(R.id.chip_status_offline)
        val chipConnWifi = view.findViewById<Chip>(R.id.chip_conn_wifi)
        val chipConnEthernet = view.findViewById<Chip>(R.id.chip_conn_ethernet)
        val chipConnBluetooth = view.findViewById<Chip>(R.id.chip_conn_bluetooth)
        val chipConnUsb = view.findViewById<Chip>(R.id.chip_conn_usb)
        val resultCount = view.findViewById<TextView>(R.id.filter_result_count)
        val btnClear = view.findViewById<MaterialButton>(R.id.btn_clear_filter)

        fun applyFilter() {
            val query = searchInput.text?.toString()?.trim()?.lowercase() ?: ""

            val statusFilter: NodeStatus? = when {
                chipStatusOnline.isChecked -> NodeStatus.EN_LIGNE
                chipStatusOffline.isChecked -> NodeStatus.HORS_LIGNE
                else -> null
            }

            val connTypes = mutableSetOf<ConnectionType>()
            if (chipConnWifi.isChecked) connTypes.add(ConnectionType.WIFI)
            if (chipConnEthernet.isChecked) connTypes.add(ConnectionType.ETHERNET)
            if (chipConnBluetooth.isChecked) connTypes.add(ConnectionType.BLUETOOTH)
            if (chipConnUsb.isChecked) connTypes.add(ConnectionType.USB)

            val filteredObjects = objects.filter { obj ->
                val matchesQuery = query.isEmpty() || obj.label.lowercase().contains(query) || obj.type.name.lowercase().contains(query)
                val matchesStatus = statusFilter == null || obj.status == statusFilter
                matchesQuery && matchesStatus
            }

            val searchMatches = if (query.isNotEmpty()) {
                filteredObjects.filter { it.label.lowercase().contains(query) }.map { it.id }.toSet()
            } else emptySet()

            val objectIds = filteredObjects.map { it.id }.toSet()
            val hasActiveFilter = query.isNotEmpty() || statusFilter != null || connTypes.size < 4

            if (hasActiveFilter) {
                networkCanvas.setFilter(objectIds, connTypes, searchMatches)
            } else {
                networkCanvas.clearFilter()
            }

            resultCount.text = getString(R.string.filter_result_count, filteredObjects.size)
        }

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { applyFilter() }
        })

        view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_status)
            .setOnCheckedStateChangeListener { _, _ -> applyFilter() }

        chipConnWifi.setOnCheckedChangeListener { _, _ -> applyFilter() }
        chipConnEthernet.setOnCheckedChangeListener { _, _ -> applyFilter() }
        chipConnBluetooth.setOnCheckedChangeListener { _, _ -> applyFilter() }
        chipConnUsb.setOnCheckedChangeListener { _, _ -> applyFilter() }

        btnClear.setOnClickListener {
            searchInput.text?.clear()
            chipStatusAll.isChecked = true
            chipConnWifi.isChecked = true
            chipConnEthernet.isChecked = true
            chipConnBluetooth.isChecked = true
            chipConnUsb.isChecked = true
            networkCanvas.clearFilter()
            resultCount.text = getString(R.string.filter_result_count, objects.size)
        }

        bottomSheet.setOnDismissListener {
            // Keep filter active after dismiss
        }

        resultCount.text = getString(R.string.filter_result_count, objects.size)
        bottomSheet.show()
    }

    private fun loadBitmap(drawableId: Int): Bitmap? {
        return android.graphics.BitmapFactory.decodeResource(resources, drawableId)
    }
}
