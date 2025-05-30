package com.xhsoft.plugins.utils

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager

class FileUtils {
    companion object {
        /**
         * Creates a Java file with the given content and returns the created PsiFile.
         * The file is also formatted according to the project's code style.
         */
        fun createFile(
            project: Project,
            dir: String,
            className: String,
            content: String
        ): PsiFile? {
            val psiDirectory = createPackageDirectory(project, dir) ?: return null

            // Check if file already exists
            val existingFile = psiDirectory.findFile("$className.java")
            if (existingFile != null) {
                return existingFile
            }

            val fileText = content.trimIndent()
            val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText("$className.java", JavaFileType.INSTANCE, fileText)

            // Format the file
            CodeStyleManager.getInstance(project).reformat(psiFile)

            var createdFile: PsiFile? = null
            WriteCommandAction.runWriteCommandAction(project) {
                createdFile = psiDirectory.add(psiFile) as PsiFile
            }

            return createdFile
        }

        /**
         * Navigates to the specified PsiFile in the editor.
         */
        fun navigateToFile(project: Project, psiFile: PsiFile) {
            val virtualFile = psiFile.virtualFile ?: return
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }

        private fun createPackageDirectory(project: Project, dir: String): PsiDirectory? {
            val dirSeq = dir.split("/")

            if (dirSeq.isEmpty()) {
                throw Exception("Target directory is unspecified.")
            }

            val root = ProjectRootManager.getInstance(project)
                .contentRoots
                .find { it.name == dirSeq[0] }

            if (root == null) {
                throw Exception("Cannot find root: $dir")
            }

            var currentDir = PsiManager.getInstance(project).findDirectory(root) ?: return null

            for (i in 1..dirSeq.lastIndex) {
                val d = dirSeq[i]
                var subDir = currentDir.findSubdirectory(d)
                if (subDir == null) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        subDir = currentDir.createSubdirectory(d)
                    }
                }
                if (subDir != null) {
                    currentDir = subDir
                } else {
                    throw Exception("Create package directory failed.")
                }
            }
            return currentDir
        }
    }
}
