package com.xhsoft.plugins.utils

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager

class FileUtils {
    companion object {
        fun createFile(
            project: Project,
            dir: String,
            className: String,
            content: String
        ) {
            val psiDirectory = createPackageDirectory(project, dir) ?: return

            if (psiDirectory.findFile("$className.java") != null) {
                return
            }

            val fileText = content.trimIndent()
            val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText("$className.java", JavaFileType.INSTANCE, fileText)
            WriteCommandAction.runWriteCommandAction(project) {
                psiDirectory.add(psiFile)
            }
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