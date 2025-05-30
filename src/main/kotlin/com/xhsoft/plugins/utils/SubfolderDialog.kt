package com.xhsoft.plugins.utils

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JComponent

/**
 * Dialog for entering subfolder information.
 * Follows IntelliJ IDEA UI guidelines for dialog design.
 */
class SubfolderDialog() : DialogWrapper(false) {
    init {
        title = MessagesBundle.message("dialog.subfolder.title")
        init()
    }

    var subfolder: String
        get() = textField?.component?.text ?: ""
        private set(value) { }

    var textField: Cell<JBTextField>? = null

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                cell(JBLabel(MessagesBundle.message("dialog.subfolder.prompt")).apply {
                    font = JBUI.Fonts.label().asBold()
                }).align(Align.FILL)
            }
            row {
                textField = textField()
                    .align(Align.FILL)
                    .focused()
                    .comment(MessagesBundle.message("dialog.subfolder.comment"))
            }
        }.apply {
            border = JBUI.Borders.empty(10)
        }

        panel.preferredSize = Dimension(400, 150)
        return panel
    }
}
