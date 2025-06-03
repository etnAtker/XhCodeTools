package com.xhsoft.plugins.utils

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class GenerateViewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        // Get the current class name if it's a Java file
        val currentClassName = if (psiFile is PsiJavaFile) {
            psiFile.classes.firstOrNull()?.name
        } else {
            null
        }

        // Find all bean classes in the specified directory with a progress indicator
        var beanClasses = listOf<PsiClass>()
        ProgressManager.getInstance().run(object : Task.Modal(
            project, MessagesBundle.message("dialog.generateview.progress.title"), false
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = MessagesBundle.message("dialog.generateview.progress.scanning")
                indicator.isIndeterminate = true
                ReadAction.run<Throwable> {
                    beanClasses = findBeanClasses(project)
                }
            }
        })

        if (beanClasses.isEmpty()) {
            return
        }

        // Show class selection dialog
        val classSelectionDialog = ClassSelectionDialog(
            project = project,
            classes = beanClasses.map { it.name ?: "" },
            defaultSelection = currentClassName?.takeIf { it.endsWith("Bean") }
        )

        if (!classSelectionDialog.showAndGet()) {
            return
        }

        val selectedClassNames = classSelectionDialog.getSelectedClasses()
        if (selectedClassNames.isEmpty()) {
            return
        }

        // Find the selected PsiClass objects
        val selectedClasses = selectedClassNames.mapNotNull { className ->
            beanClasses.find { it.qualifiedName == className || it.name == className }
        }

        // Extract fields from selected classes
        val fields = extractFields(selectedClasses)

        // Show field selection dialog
        val fieldSelectionDialog = FieldSelectionDialog(
            project = project, fields = fields
        )

        if (!fieldSelectionDialog.showAndGet()) {
            return
        }

        var selectedFields = fieldSelectionDialog.getSelectedFields()
        if (selectedFields.isEmpty()) {
            return
        }

        // Show a dialog for subfolder and filename
        val fileInfoDialog = ViewFileInfoDialog(selectedClassNames, currentClassName)
        if (!fileInfoDialog.showAndGet()) {
            return
        }

        val filename = fileInfoDialog.filename
        if (filename.isBlank()) {
            return
        }

        val subfolder = fileInfoDialog.subfolder
        val autoGenerateSelect = fileInfoDialog.isAutoGenerateSelect
        val mainClass = fileInfoDialog.mainClass

        selectedFields.forEach { field -> field.mainClassName = mainClass }
        selectedFields = selectedFields.sortedWith { field1, field2 ->
             field2.mainClass - field1.mainClass
        }

        // Generate the View file
        generateViewFile(
            project = project,
            fields = selectedFields,
            filename = filename,
            subfolder = subfolder,
            autoGenerateSelect = autoGenerateSelect,
            mainClass = mainClass
        )
    }

    private fun findBeanClasses(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.projectScope(project)
        val psiManager = PsiManager.getInstance(project)
        val classes = mutableListOf<PsiClass>()

        // Find all Java files in the bean directory and its subdirectories
        val beanDir = "xhsoft-base/src/main/java/com/xhsoft/base/model/bean"

        // Get all Java files in the project
        val javaFiles =
            FilenameIndex.getAllFilesByExt(project, "java", scope)
                .filter { file -> file.path.contains(beanDir) }

        // Extract classes from the Java files
        javaFiles.forEach { file ->
            val psiFile = psiManager.findFile(file)
            if (psiFile is PsiJavaFile) {
                psiFile.classes.forEach { psiClass ->
                    classes.add(psiClass)
                }
            }
        }

        return classes
    }

    private fun extractFields(classes: List<PsiClass>): List<FieldInfo> {
        val fields = mutableListOf<FieldInfo>()

        classes.forEach { psiClass ->
            val className = psiClass.name ?: return@forEach

            psiClass.allFields
                .filter { f -> !f.hasModifierProperty(PsiModifier.STATIC) }
                .forEach { field ->
                    val fieldName = field.name
                    val fieldType = field.type.presentableText

                    // Extract annotations
                    val annotations = field.annotations.map { it.text }

                    fields.add(
                        FieldInfo(
                            className = className,
                            fieldName = fieldName,
                            type = fieldType,
                            annotations = annotations,
                            psiField = field
                        )
                    )
                }
        }

        return fields
    }

    private fun generateViewFile(
        project: Project,
        fields: List<FieldInfo>,
        filename: String,
        subfolder: String,
        autoGenerateSelect: Boolean = false,
        mainClass: String? = null
    ) {
        // Target directory
        val viewDir =
            "xhsoft-base/src/main/java/com/xhsoft/base/model/view" + if (subfolder.isNotEmpty()) "/$subfolder" else ""

        // Target package
        val viewPackage = "com.xhsoft.base.model.view" + if (subfolder.isNotEmpty()) ".$subfolder" else ""

        // Collect imports
        val imports = mutableSetOf<String>()
        imports.add("import io.swagger.v3.oas.annotations.media.Schema;")
        imports.add("import lombok.Data;")

        // Add Select annotation import if auto-generate is enabled
        if (autoGenerateSelect) {
            imports.add("import com.xhsoft.lib.ext.hql_builder.anno.Select;")
        }

        // Collect field type imports
        fields.forEach { field ->
            field.psiField?.let { psiField ->
                val psiType = psiField.type

                // Check if the type is a class (not primitive or java.lang.*)
                if (needsImport(psiType)) {
                    // Add import for the type
                    val canonicalText = psiType.canonicalText
                    imports.add("import $canonicalText;")

                    // If it's a generic type, try to add imports for type parameters
                    if (!canonicalText.contains("<")) {
                        return@let
                    }
                    // Extract type parameters
                    val typeParams = canonicalText.substringAfter("<").substringBefore(">")
                    typeParams.split(",").forEach { param ->
                        val trimmedParam = param.trim()
                        if (needsImport(trimmedParam)) {
                            imports.add("import $trimmedParam;")
                        }
                    }
                }
            }
        }

        val constantMap = mutableMapOf<String, String>()
        fun toConstant(beanName: String): String {
            val joinName = "join$beanName"
            if (constantMap.containsKey(joinName)) {
                return constantMap[joinName]!!
            }

            val constantName = beanName.camelToUpperUnderline()
            constantMap[joinName] = constantName
            return constantName
        }

        fun constantDeclarations(): String {
            constantMap.map { (key, value) ->
                "private static final String $value = \"$key\";"
            }.joinToString("\n").let {
                return if (it.isEmpty()) "" else "\n\n$it"
            }
        }

        // Generate fields with proper naming
        val fieldDefinitions = fields.joinToString("\n\n") { field ->
            // Convert class name (remove "Bean" suffix)
            val realClassName = field.className.removeSuffix("Bean")
            val fieldPrefix = realClassName.decapitalize()

            // Determine if this field belongs to the main class
            val isMainClassField = mainClass != null && mainClass.contains(field.className)

            // Create a new field name based on whether it's from the main class or not
            val newFieldName = if (
                (isMainClassField && autoGenerateSelect)
                || field.fieldName.startsWith(fieldPrefix)
            ) {
                // For main class fields or already have a right prefix, keep the original name
                field.fieldName
            } else {
                // For other classes, prefix with class name
                fieldPrefix + field.fieldName.replaceFirstChar { it.uppercase() }
            }

            // Find @Schema annotation if present
            val schemaAnnotation = field.annotations.find { it.contains("Schema") }
            val schemaAnnotationText = schemaAnnotation ?: "@Schema(description = \"\")"

            // Build annotations
            val annotations = mutableListOf<String>()
            annotations.add(schemaAnnotationText)

            // Add @Select annotation if auto-generate is enabled
            if (autoGenerateSelect) {
                if (isMainClassField) {
                    // For main class fields, add @Select
                    annotations.add("@Select")
                } else {
                    // For other class fields, add @Select with name and from attributes
                    annotations.add("@Select(name = \"${field.fieldName}\", from = ${toConstant(realClassName)})")
                }
            }

            // Return field definition
            "${annotations.joinToString("\n")}\nprivate ${field.type} $newFieldName;"
        }

        // Generate file content
        val content = """
            package $viewPackage;

            ${imports.joinToString("\n")}

            @Data
            public class $filename {${constantDeclarations()}

            $fieldDefinitions

            }
        """.trimIndent()

        // Create the file and get the created file reference
        val createdFile = FileUtils.createFile(
            project = project, dir = viewDir, className = filename, content = content
        )

        // Navigate to the created file
        if (createdFile != null) {
            FileUtils.navigateToFile(project, createdFile)
        }
    }

    private fun needsImport(type: PsiType): Boolean {
        return type !is PsiPrimitiveType && needsImport(type.canonicalText)
    }

    private fun needsImport(text: String): Boolean {
        return text.contains(".") && !text.startsWith("java.lang.")
    }

    private fun String.decapitalize(): String {
        if (isEmpty() || !first().isUpperCase()) return this
        return first().lowercase() + substring(1)
    }

    fun String.camelToUpperUnderline(): String {
        val words = mutableListOf<String>()
        var st = 0
        this.forEachIndexed { index, c ->
            if (c.isUpperCase() && index > 0) {
                words.add(this.substring(st, index))
                st = index
            }
        }
        words.add(this.substring(st))
        return words.joinToString("_").uppercase()
    }
}
