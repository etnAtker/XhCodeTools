package com.xhsoft.plugins.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

/**
 * Data class representing a field from a bean class
 */
data class FieldInfo(
    val className: String,
    val fieldName: String,
    val type: String,
    val annotations: List<String> = emptyList(),
    val psiField: com.intellij.psi.PsiField? = null
) {
    var mainClassName: String? = null
        set(value) {
            field = value
            mainClass = if (className == value) 1 else 0
        }
    var mainClass: Byte = 0
        private set

    override fun toString(): String {
        return "$className.$fieldName: $type"
    }
}

/**
 * Dialog for selecting fields from bean classes to include in the view.
 * Follows IntelliJ IDEA UI guidelines for dialog design.
 * Groups fields by their class and allows expanding/collapsing fields by class.
 */
class FieldSelectionDialog(
    private val project: Project,
    private val fields: List<FieldInfo>
) : DialogWrapper(project) {

    // Root node for the tree
    private val rootNode = DefaultMutableTreeNode("Root")

    // Tree model
    private val treeModel = DefaultTreeModel(rootNode)

    // Tree component
    private val fieldTree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
        cellRenderer = FieldTreeCellRenderer()
        border = JBUI.Borders.empty(5)
        selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    }

    // Map to store class nodes for quick access
    private val classNodes = mutableMapOf<String, DefaultMutableTreeNode>()

    // Map to store field nodes for quick access
    private val fieldNodes = mutableMapOf<FieldInfo, DefaultMutableTreeNode>()

    init {
        title = MessagesBundle.message("dialog.generateview.field.title")

        // Group fields by class
        val fieldsByClass = fields.groupBy { it.className }

        // Create class nodes and field nodes
        fieldsByClass.forEach { (className, classFields) ->
            // Create class node
            val simpleClassName = className.substringAfterLast('.')
            val classNode = DefaultMutableTreeNode(simpleClassName)
            rootNode.add(classNode)
            classNodes[className] = classNode

            // Create field nodes for this class
            classFields.forEach { field ->
                val fieldNode = DefaultMutableTreeNode(field)
                classNode.add(fieldNode)
                fieldNodes[field] = fieldNode
            }
        }

        // Expand all nodes by default
        TreeUtil.expandAll(fieldTree)

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row {
                cell(JBLabel(MessagesBundle.message("dialog.generateview.field.prompt")).apply {
                    font = JBUI.Fonts.label().asBold()
                }).align(Align.FILL)
            }
            row {
                cell(JBScrollPane(fieldTree).apply {
                    border = JBUI.Borders.empty(5)
                    viewport.background = fieldTree.background
                })
                    .resizableColumn()
                    .align(Align.FILL)
            }.resizableRow()
        }.apply {
            border = JBUI.Borders.empty(10)
        }

        panel.preferredSize = Dimension(550, 450)
        return panel
    }

    /**
     * Get the selected fields from the tree
     */
    fun getSelectedFields(): List<FieldInfo> {
        val selectedPaths = fieldTree.selectionPaths ?: return emptyList()

        return selectedPaths.mapNotNull { path ->
            val node = path.lastPathComponent as? DefaultMutableTreeNode
            val userObject = node?.userObject

            userObject as? FieldInfo
        }
    }

    /**
     * Custom cell renderer for the field tree to improve readability
     */
    private class FieldTreeCellRenderer : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as DefaultMutableTreeNode
            val userObject = node.userObject

            if (userObject is String) {
                // Class node
                append(userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                icon = com.intellij.icons.AllIcons.Nodes.Class
            } else if (userObject is FieldInfo) {
                // Field node
                append(userObject.fieldName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                append(": ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
                append(userObject.type, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)


                icon = com.intellij.icons.AllIcons.Nodes.Field
            }

            // Add some padding
            ipad = JBUI.insets(3)
        }
    }
}
