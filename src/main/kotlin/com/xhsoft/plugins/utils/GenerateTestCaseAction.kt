package com.xhsoft.plugins.utils

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

class GenerateTestCaseAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return
        val psiMethod = findTargetMethod(e, psiFile) ?: return
        val psiClass = psiMethod.containingClass ?: return

        val sourcePackage = psiFile.packageName
        if (!sourcePackage.startsWith(SOURCE_PACKAGE_PREFIX)) {
            showInfo(
                project = project,
                title = MessagesBundle.message("dialog.generatetestcase.invalidpackage.title"),
                message = MessagesBundle.message(
                    "dialog.generatetestcase.invalidpackage.message",
                    SOURCE_PACKAGE_PREFIX
                )
            )
            return
        }

        val sourceClassName = psiClass.name ?: return
        val sourceFileName = psiFile.virtualFile?.nameWithoutExtension ?: sourceClassName
        val packageSuffix = sourcePackage.removePrefix(SOURCE_PACKAGE_PREFIX).removePrefix(".")
        val testPackage = buildTestPackage(packageSuffix)
        val testDir = buildTestDir(packageSuffix)
        val testClassName = "Test$sourceFileName"

        val testFile = FileUtils.createFile(
            project = project,
            dir = testDir,
            className = testClassName,
            content = buildTestClassContent(
                testPackage = testPackage,
                sourcePackage = sourcePackage,
                sourceClassName = sourceClassName,
                testClassName = testClassName
            )
        ) as? PsiJavaFile ?: return

        val testClass = testFile.classes.firstOrNull() ?: return
        val testMethodName = "test" + psiMethod.name.capitalizeFirst()

        if (testClass.findMethodsByName(testMethodName, false).isNotEmpty()) {
            showInfo(
                project = project,
                title = MessagesBundle.message("dialog.generatetestcase.method.exists.title"),
                message = MessagesBundle.message(
                    "dialog.generatetestcase.method.exists.message",
                    testMethodName,
                    testClassName
                )
            )
            return
        }

        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val newMethod = elementFactory.createMethodFromText(
            """
                @Test
                public void $testMethodName() {
                }
            """.trimIndent(),
            testClass
        )

        WriteCommandAction.runWriteCommandAction(project) {
            val added = testClass.add(newMethod)
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(added)
            CodeStyleManager.getInstance(project).reformat(added)
        }

        FileUtils.navigateToFile(project, testFile)
    }

    private fun findTargetMethod(e: AnActionEvent, psiFile: PsiJavaFile): PsiMethod? {
        val element = e.getData(CommonDataKeys.PSI_ELEMENT)
        val elementMethod = when (element) {
            is PsiMethod -> element
            else -> PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        }
        if (elementMethod != null) {
            return elementMethod
        }

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return null
        val caretElement = psiFile.findElementAt(editor.caretModel.offset) ?: return null
        return PsiTreeUtil.getParentOfType(caretElement, PsiMethod::class.java)
    }

    private fun buildTestPackage(packageSuffix: String): String {
        return if (packageSuffix.isBlank()) {
            TARGET_PACKAGE_PREFIX
        } else {
            "$TARGET_PACKAGE_PREFIX.$packageSuffix"
        }
    }

    private fun buildTestDir(packageSuffix: String): String {
        return if (packageSuffix.isBlank()) {
            TARGET_ROOT_DIR
        } else {
            "$TARGET_ROOT_DIR/${packageSuffix.replace('.', '/')}"
        }
    }

    private fun buildTestClassContent(
        testPackage: String,
        sourcePackage: String,
        sourceClassName: String,
        testClassName: String
    ): String {
        val fieldName = sourceClassName.decapitalizeFirst()
        return """
            package $testPackage;
            
            import $sourcePackage.$sourceClassName;
            import lombok.extern.slf4j.Slf4j;
            import org.junit.jupiter.api.Test;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.boot.test.context.SpringBootTest;
            import org.springframework.test.annotation.Rollback;
            
            @Slf4j
            @Rollback
            @SpringBootTest
            public class $testClassName {
            
                @Autowired
                private $sourceClassName $fieldName;
            }
        """.trimIndent()
    }

    private fun String.capitalizeFirst(): String {
        if (isEmpty()) return this
        val first = this[0]
        return first.uppercaseChar() + substring(1)
    }

    private fun String.decapitalizeFirst(): String {
        if (isEmpty()) return this
        val first = this[0]
        return first.lowercaseChar() + substring(1)
    }

    private fun showInfo(project: Project, title: String, message: String) {
        Messages.showInfoMessage(project, message, title)
    }

    companion object {
        private const val SOURCE_PACKAGE_PREFIX = "com.xhsoft.base"
        private const val TARGET_PACKAGE_PREFIX = "com.xhsoft.main"
        private const val TARGET_ROOT_DIR = "xhsoft-main/src/test/java/com/xhsoft/main"
    }
}
