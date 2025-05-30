package com.xhsoft.plugins.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListSelectionModel

/**
 * Dialog for selecting one or more classes from a list.
 * Follows IntelliJ IDEA UI guidelines for dialog design.
 */
class ClassSelectionDialog(
    project: Project,
    private val classes: List<String>,
    defaultSelection: String? = null
) : DialogWrapper(project) {

    private val listModel = DefaultListModel<String>()
    private val classList = JBList(listModel).apply {
        cellRenderer = ClassListCellRenderer()
        border = JBUI.Borders.empty(5)
        emptyText.text = MessagesBundle.message("dialog.generateview.class.empty")
    }

    init {
        title = MessagesBundle.message("dialog.generateview.class.title")
        init()

        // Add classes to the list model
        classes.forEach { listModel.addElement(it) }

        // Configure list for multi-selection
        classList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        // Set default selection if provided
        defaultSelection?.let { defaultClass ->
            val index = classes.indexOf(defaultClass)
            if (index >= 0) {
                classList.selectedIndex = index
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                cell(JBLabel(MessagesBundle.message("dialog.generateview.class.prompt")).apply {
                    font = JBUI.Fonts.label().asBold()
                }).align(Align.FILL)
            }
            row {
                cell(JBScrollPane(classList).apply {
                    border = JBUI.Borders.empty(5)
                    viewport.background = classList.background
                })
                    .resizableColumn()
                    .align(Align.FILL)
            }.resizableRow()
        }.apply {
            border = JBUI.Borders.empty(10)
        }

        panel.preferredSize = Dimension(500, 350)
        return panel
    }

    fun getSelectedClasses(): List<String> {
        return classList.selectedValuesList
    }

    /**
     * Custom cell renderer for the class list to improve readability
     */
    private class ClassListCellRenderer : ColoredListCellRenderer<String>() {
        override fun customizeCellRenderer(
            list: JList<out String>,
            value: String,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            // Extract package and class name for better display
            val lastDotIndex = value.lastIndexOf('.')
            if (lastDotIndex > 0) {
                val packageName = value.substring(0, lastDotIndex)
                val className = value.substring(lastDotIndex + 1)

                append(className, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                append("($packageName)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            } else {
                append(value, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            }

            // Add some padding
            ipad = JBUI.insets(3)
        }
    }
}
