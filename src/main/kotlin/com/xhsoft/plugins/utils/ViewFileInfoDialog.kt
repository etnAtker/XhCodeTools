package com.xhsoft.plugins.utils

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JComponent

/**
 * Dialog for entering view file information.
 * Follows IntelliJ IDEA UI guidelines for dialog design.
 */
class ViewFileInfoDialog(
    private val selectedClasses: List<String> = emptyList(),
    private val initSelectedClass: String? = null
) : DialogWrapper(false) {
    init {
        title = MessagesBundle.message("dialog.generateview.filename.title")
        init()
    }

    var filenameField: Cell<JBTextField>? = null
    var subfolderField: Cell<JBTextField>? = null
    var autoGenerateSelectCheckbox: Cell<JBCheckBox>? = null
    var mainClassComboBox: Cell<JComboBox<String>>? = null

    // State for auto-generate checkbox
    private var autoGenerateSelect = true

    // Selected main class
    private var selectedMainClass: String? = null

    val filename: String
        get() = filenameField?.component?.text ?: ""

    val subfolder: String
        get() = subfolderField?.component?.text ?: ""

    val isAutoGenerateSelect: Boolean
        get() = autoGenerateSelect

    val mainClass: String?
        get() = selectedMainClass

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                cell(JBLabel(MessagesBundle.message("dialog.generateview.filename.prompt")).apply {
                    font = JBUI.Fonts.label().asBold()
                }).align(Align.FILL)
            }
            row {
                filenameField = textField()
                    .align(Align.FILL)
                    .focused()
                    .comment(MessagesBundle.message("dialog.generateview.filename.comment"))
            }

            row {
                cell(JBLabel(MessagesBundle.message("dialog.generateview.subfolder.prompt")).apply {
                    font = JBUI.Fonts.label().asBold()
                }).align(Align.FILL)
            }
            row {
                subfolderField = textField()
                    .align(Align.FILL)
                    .comment(MessagesBundle.message("dialog.generateview.subfolder.comment"))
            }

            // Add auto-generate Select annotation checkbox
            row {
                autoGenerateSelectCheckbox = checkBox(MessagesBundle.message("dialog.generateview.autogenerate.checkbox"))
                    .bindSelected({ autoGenerateSelect }, { autoGenerateSelect = it })
                    .selected(true)
                    .align(Align.FILL)
            }

            // Add main class dropdown (only visible when checkbox is checked)
            row {
                cell(JBLabel(MessagesBundle.message("dialog.generateview.mainclass.prompt")).apply {
                    font = JBUI.Fonts.label()
                }).align(Align.FILL)
                    .visibleIf(autoGenerateSelectCheckbox!!.selected)

                // Create a combo box with the selected classes
                mainClassComboBox = comboBox(selectedClasses)
                    .visibleIf(autoGenerateSelectCheckbox!!.selected)
                    .applyToComponent {
                        addItemListener { event ->
                            if (event.stateChange == java.awt.event.ItemEvent.SELECTED) {
                                val selectedIndex = selectedIndex
                                if (selectedIndex >= 0 && selectedIndex < selectedClasses.size) {
                                    selectedMainClass = selectedClasses[selectedIndex]
                                }
                            }
                        }

                        // Set initial selection
                        if (selectedClasses.isNotEmpty()) {
                            val defaultIndex = if (initSelectedClass != null) {
                                selectedClasses.indexOf(initSelectedClass).takeIf { it >= 0 } ?: 0
                            } else {
                                0
                            }
                            selectedIndex = defaultIndex
                            selectedMainClass = selectedClasses[defaultIndex]
                        }
                    }
            }
        }.apply {
            border = JBUI.Borders.empty(10)
        }

        panel.preferredSize = Dimension(400, 250)
        return panel
    }
}
