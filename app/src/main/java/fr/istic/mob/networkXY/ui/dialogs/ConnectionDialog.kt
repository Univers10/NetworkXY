package fr.istic.mob.networkXY.ui.dialogs

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.istic.mob.networkXY.R
import fr.istic.mob.networkXY.model.Connection
import fr.istic.mob.networkXY.model.ConnectionType

object ConnectionDialog {

    fun showAddDialog(
        context: Context,
        onConnectionCreated: (String, ConnectionType, Int) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_connection, null)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinner_connection_type)
        val labelEdit = dialogView.findViewById<EditText>(R.id.edit_connection_label)
        val colorSpinner = dialogView.findViewById<Spinner>(R.id.spinner_connection_color)

        setupTypeSpinner(context, typeSpinner)
        setupColorSpinner(context, colorSpinner)

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = ConnectionType.values()[position]
                colorSpinner.setSelection(getPositionFromColor(selectedType.defaultColor))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.add_connection)
            .setView(dialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                val label = labelEdit.text.toString()
                val connType = ConnectionType.values()[typeSpinner.selectedItemPosition]
                val color = getColorFromPosition(colorSpinner.selectedItemPosition)
                onConnectionCreated(label, connType, color)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showEditDialog(
        context: Context,
        conn: Connection,
        onConnectionUpdated: (String?, ConnectionType?, Int?, Float?) -> Unit,
        onConnectionDeleted: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_connection_edit, null)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinner_connection_type)
        val labelEdit = dialogView.findViewById<EditText>(R.id.edit_connection_label)
        val colorSpinner = dialogView.findViewById<Spinner>(R.id.spinner_connection_color)
        val thicknessSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_thickness)

        setupTypeSpinner(context, typeSpinner)
        typeSpinner.setSelection(conn.connectionType.ordinal)
        labelEdit.setText(conn.label)
        setupColorSpinner(context, colorSpinner)
        colorSpinner.setSelection(getPositionFromColor(conn.color))
        thicknessSeekBar.progress = (conn.thickness - 1).toInt()

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit_connection)
            .setView(dialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                val newLabel = labelEdit.text.toString()
                val newType = ConnectionType.values()[typeSpinner.selectedItemPosition]
                val newColor = getColorFromPosition(colorSpinner.selectedItemPosition)
                val newThickness = thicknessSeekBar.progress + 1f

                onConnectionUpdated(
                    if (newLabel != conn.label) newLabel else null,
                    if (newType != conn.connectionType) newType else null,
                    if (newColor != conn.color) newColor else null,
                    if (newThickness != conn.thickness) newThickness else null
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.delete) { _, _ ->
                showDeleteConfirmation(context, context.getString(R.string.confirm_delete_connection), onConnectionDeleted)
            }
            .show()
    }

    private fun setupTypeSpinner(context: Context, spinner: Spinner) {
        val typeNames = ConnectionType.values().map {
            when (it) {
                ConnectionType.WIFI -> context.getString(R.string.conn_type_wifi)
                ConnectionType.ETHERNET -> context.getString(R.string.conn_type_ethernet)
                ConnectionType.BLUETOOTH -> context.getString(R.string.conn_type_bluetooth)
                ConnectionType.USB -> context.getString(R.string.conn_type_usb)
            }
        }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, typeNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupColorSpinner(context: Context, spinner: Spinner) {
        val colorNames = listOf(
            context.getString(R.string.color_black),
            context.getString(R.string.color_red),
            context.getString(R.string.color_green),
            context.getString(R.string.color_blue),
            context.getString(R.string.color_orange),
            context.getString(R.string.color_cyan),
            context.getString(R.string.color_magenta),
            context.getString(R.string.color_wifi),
            context.getString(R.string.color_ethernet),
            context.getString(R.string.color_bluetooth),
            context.getString(R.string.color_usb)
        )
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, colorNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun getColorFromPosition(position: Int): Int {
        return when (position) {
            0 -> Color.BLACK
            1 -> Color.RED
            2 -> Color.GREEN
            3 -> Color.BLUE
            4 -> 0xFFFFA500.toInt()
            5 -> Color.CYAN
            6 -> Color.MAGENTA
            7 -> ConnectionType.WIFI.defaultColor
            8 -> ConnectionType.ETHERNET.defaultColor
            9 -> ConnectionType.BLUETOOTH.defaultColor
            10 -> ConnectionType.USB.defaultColor
            else -> Color.BLACK
        }
    }

    private fun getPositionFromColor(color: Int): Int {
        return when (color) {
            Color.BLACK -> 0
            Color.RED -> 1
            Color.GREEN -> 2
            Color.BLUE -> 3
            0xFFFFA500.toInt() -> 4
            Color.CYAN -> 5
            Color.MAGENTA -> 6
            ConnectionType.WIFI.defaultColor -> 7
            ConnectionType.ETHERNET.defaultColor -> 8
            ConnectionType.BLUETOOTH.defaultColor -> 9
            ConnectionType.USB.defaultColor -> 10
            else -> 0
        }
    }

    private fun showDeleteConfirmation(context: Context, message: String, onConfirmed: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.confirm_delete)
            .setMessage(message)
            .setPositiveButton(R.string.delete) { _, _ -> onConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
