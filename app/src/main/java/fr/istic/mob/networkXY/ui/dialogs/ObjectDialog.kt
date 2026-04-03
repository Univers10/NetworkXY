package fr.istic.mob.networkXY.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import fr.istic.mob.networkXY.R
import fr.istic.mob.networkXY.model.NetworkObject
import fr.istic.mob.networkXY.model.NodeStatus
import fr.istic.mob.networkXY.model.NodeType
import fr.istic.mob.networkXY.model.ObjectColor

object ObjectDialog {

    fun showAddDialog(
        context: Context,
        x: Float,
        y: Float,
        onObjectCreated: (String, NodeType, ObjectColor, NodeStatus) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_object, null)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinner_object_type)
        val labelEdit = dialogView.findViewById<EditText>(R.id.edit_object_label)
        val colorSpinner = dialogView.findViewById<Spinner>(R.id.spinner_object_color)
        val statusSwitch = dialogView.findViewById<SwitchMaterial>(R.id.switch_object_status)

        setupTypeSpinner(context, typeSpinner)
        setupColorSpinner(context, colorSpinner)
        statusSwitch.isChecked = true
        updateStatusText(context, statusSwitch)

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (labelEdit.text.isNullOrEmpty()) {
                    labelEdit.setText(getTypeName(context, NodeType.values()[position]))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.add_object)
            .setView(dialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                val label = labelEdit.text.toString().ifEmpty { context.getString(R.string.default_object_label) }
                val type = NodeType.values()[typeSpinner.selectedItemPosition]
                val color = ObjectColor.values()[colorSpinner.selectedItemPosition]
                val status = if (statusSwitch.isChecked) NodeStatus.EN_LIGNE else NodeStatus.HORS_LIGNE
                onObjectCreated(label, type, color, status)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showEditDialog(
        context: Context,
        obj: NetworkObject,
        onObjectUpdated: (String?, NodeType?, ObjectColor?, NodeStatus?) -> Unit,
        onObjectDeleted: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_object, null)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinner_object_type)
        val labelEdit = dialogView.findViewById<EditText>(R.id.edit_object_label)
        val colorSpinner = dialogView.findViewById<Spinner>(R.id.spinner_object_color)
        val statusSwitch = dialogView.findViewById<SwitchMaterial>(R.id.switch_object_status)

        setupTypeSpinner(context, typeSpinner)
        typeSpinner.setSelection(obj.type.ordinal)
        labelEdit.setText(obj.label)
        setupColorSpinner(context, colorSpinner)
        colorSpinner.setSelection(obj.color.ordinal)
        statusSwitch.isChecked = obj.status == NodeStatus.EN_LIGNE
        updateStatusText(context, statusSwitch)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit_object)
            .setView(dialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                val newLabel = labelEdit.text.toString()
                val newType = NodeType.values()[typeSpinner.selectedItemPosition]
                val newColor = ObjectColor.values()[colorSpinner.selectedItemPosition]
                val newStatus = if (statusSwitch.isChecked) NodeStatus.EN_LIGNE else NodeStatus.HORS_LIGNE
                onObjectUpdated(
                    if (newLabel != obj.label) newLabel else null,
                    if (newType != obj.type) newType else null,
                    if (newColor != obj.color) newColor else null,
                    if (newStatus != obj.status) newStatus else null
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.delete) { _, _ ->
                showDeleteConfirmation(context, context.getString(R.string.confirm_delete_object), onObjectDeleted)
            }
            .show()
    }

    private fun setupTypeSpinner(context: Context, spinner: Spinner) {
        val typeNames = NodeType.values().map {
            when (it) {
                NodeType.ROUTEUR -> context.getString(R.string.type_routeur)
                NodeType.TV -> context.getString(R.string.type_tv)
                NodeType.ORDINATEUR -> context.getString(R.string.type_ordinateur)
                NodeType.SMARTPHONE -> context.getString(R.string.type_smartphone)
                NodeType.TABLETTE -> context.getString(R.string.type_tablette)
                NodeType.IMPRIMANTE -> context.getString(R.string.type_imprimante)
                NodeType.ENCEINTE -> context.getString(R.string.type_enceinte)
                NodeType.CAMERA -> context.getString(R.string.type_camera)
                NodeType.AUTRE -> context.getString(R.string.type_autre)
            }
        }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, typeNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupColorSpinner(context: Context, spinner: Spinner) {
        val colorNames = ObjectColor.values().map {
            when (it) {
                ObjectColor.RED -> context.getString(R.string.color_red)
                ObjectColor.GREEN -> context.getString(R.string.color_green)
                ObjectColor.BLUE -> context.getString(R.string.color_blue)
                ObjectColor.ORANGE -> context.getString(R.string.color_orange)
                ObjectColor.CYAN -> context.getString(R.string.color_cyan)
                ObjectColor.MAGENTA -> context.getString(R.string.color_magenta)
                ObjectColor.BLACK -> context.getString(R.string.color_black)
            }
        }
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, colorNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun updateStatusText(context: Context, switch: SwitchMaterial) {
        switch.text = if (switch.isChecked) context.getString(R.string.status_online) else context.getString(R.string.status_offline)
        switch.setOnCheckedChangeListener { _, isChecked ->
            switch.text = if (isChecked) context.getString(R.string.status_online) else context.getString(R.string.status_offline)
        }
    }

    private fun getTypeName(context: Context, type: NodeType): String {
        return when (type) {
            NodeType.ROUTEUR -> context.getString(R.string.type_routeur)
            NodeType.TV -> context.getString(R.string.type_tv)
            NodeType.ORDINATEUR -> context.getString(R.string.type_ordinateur)
            NodeType.SMARTPHONE -> context.getString(R.string.type_smartphone)
            NodeType.TABLETTE -> context.getString(R.string.type_tablette)
            NodeType.IMPRIMANTE -> context.getString(R.string.type_imprimante)
            NodeType.ENCEINTE -> context.getString(R.string.type_enceinte)
            NodeType.CAMERA -> context.getString(R.string.type_camera)
            NodeType.AUTRE -> context.getString(R.string.type_autre)
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
