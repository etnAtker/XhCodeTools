package com.xhsoft.plugins.utils

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class SubfolderDialog() : DialogWrapper(false) {
    init {
        title = MessagesBundle.message("dialog.subfolder.title")
        init()
    }

    var subfolder: String
        get () = textField?.component?.text ?: ""
        private set (value) {  }

    var textField: Cell<JBTextField>? = null

    override fun createCenterPanel(): JComponent? {
        val panel = panel {
            row(MessagesBundle.message("dialog.subfolder.prompt")) {
                textField = textField().align(Align.FILL).comment(
                    MessagesBundle.message("dialog.subfolder.comment")
                )
            }
        }

        return panel
    }
}