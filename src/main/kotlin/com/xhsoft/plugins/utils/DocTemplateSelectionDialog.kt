package com.xhsoft.plugins.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class DocTemplateSelectionDialog(
    private val project: Project,
    private val className: String
) : DialogWrapper(project) {

    private val docCheckBox = JBCheckBox(MessagesBundle.message("dialog.generatedoc.template.doc", className), true)
    private val listDocCheckBox = JBCheckBox(MessagesBundle.message("dialog.generatedoc.template.listdoc", className), false)
    private val pageDocCheckBox = JBCheckBox(MessagesBundle.message("dialog.generatedoc.template.pagedoc", className), false)

    init {
        title = MessagesBundle.message("dialog.generatedoc.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5)

        panel.add(JLabel(MessagesBundle.message("dialog.generatedoc.prompt", className)), gbc)

        gbc.gridy++
        panel.add(docCheckBox, gbc)

        gbc.gridy++
        panel.add(listDocCheckBox, gbc)

        gbc.gridy++
        panel.add(pageDocCheckBox, gbc)

        return panel
    }

    fun getSelectedTemplates(): List<DocTemplate> {
        val selectedTemplates = mutableListOf<DocTemplate>()
        
        if (docCheckBox.isSelected) {
            selectedTemplates.add(DocTemplate.DOC)
        }
        if (listDocCheckBox.isSelected) {
            selectedTemplates.add(DocTemplate.LIST_DOC)
        }
        if (pageDocCheckBox.isSelected) {
            selectedTemplates.add(DocTemplate.PAGE_DOC)
        }
        
        return selectedTemplates
    }
}