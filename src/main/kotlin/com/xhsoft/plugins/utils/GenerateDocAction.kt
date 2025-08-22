package com.xhsoft.plugins.utils

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiJavaFile

class GenerateDocAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val psiClass = (psiFile as? PsiJavaFile)?.classes?.firstOrNull() ?: return

        val className = psiClass.name ?: return
        val objectPackage = psiFile.packageName
        
        // 显示模板选择对话框
        val templateSelectionDialog = DocTemplateSelectionDialog(project, className)
        if (templateSelectionDialog.showAndGet()) {
            val selectedTemplates = templateSelectionDialog.getSelectedTemplates()
            if (selectedTemplates.isNotEmpty()) {
                generateDocFiles(project, className, objectPackage, selectedTemplates)
            }
        }
    }

    private fun generateDocFiles(
        project: com.intellij.openapi.project.Project,
        className: String,
        objectPackage: String,
        selectedTemplates: List<DocTemplate>
    ) {
        val docPackage = "com.xhsoft.base.helper.doc.response"
        val docDir = "xhsoft-base/src/main/java/com/xhsoft/base/helper/doc/response"

        selectedTemplates.forEach { template ->
            val docClassName = when (template) {
                DocTemplate.DOC -> "${className}Doc"
                DocTemplate.LIST_DOC -> "${className}ListDoc"
                DocTemplate.PAGE_DOC -> "${className}PageDoc"
            }

            val content = when (template) {
                DocTemplate.DOC -> generateDocTemplate(docPackage, objectPackage, className)
                DocTemplate.LIST_DOC -> generateListDocTemplate(docPackage, objectPackage, className)
                DocTemplate.PAGE_DOC -> generatePageDocTemplate(docPackage, objectPackage, className)
            }

            FileUtils.createFile(
                project = project,
                dir = docDir,
                className = docClassName,
                content = content
            )
        }
    }

    private fun generateDocTemplate(docPackage: String, objectPackage: String, className: String): String {
        return """
            package $docPackage;
            
            import $objectPackage.$className;
            import com.xhsoft.lib.ext.doc.AbstractDocMessage;
            
            public class ${className}Doc extends AbstractDocMessage<${className}, Object> {
            }
        """.trimIndent()
    }

    private fun generateListDocTemplate(docPackage: String, objectPackage: String, className: String): String {
        return """
            package $docPackage;
            
            import $objectPackage.$className;
            import com.xhsoft.lib.ext.doc.AbstractListDoc;
            
            public class ${className}ListDoc extends AbstractListDoc<${className}, Object> {
            }
        """.trimIndent()
    }

    private fun generatePageDocTemplate(docPackage: String, objectPackage: String, className: String): String {
        return """
            package $docPackage;
            
            import $objectPackage.$className;
            import com.xhsoft.lib.ext.doc.AbstractDocPage;
            
            public class ${className}PageDoc extends AbstractDocPage<${className}, Object> {
            }
        """.trimIndent()
    }
}

enum class DocTemplate(val displayName: String) {
    DOC("基础响应文档"),
    LIST_DOC("列表响应文档"),
    PAGE_DOC("分页响应文档")
}