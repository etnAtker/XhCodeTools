package com.xhsoft.plugins.utils

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiJavaFile

class GenerateServiceAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val psiClass = (psiFile as? PsiJavaFile)?.classes?.firstOrNull() ?: return

        if (psiClass.name?.endsWith("Bean") != true) return

        val beanPackage = psiFile.packageName
        val beanName = psiClass.name ?: return
        val baseName = psiClass.name?.removeSuffix("Bean") ?: return
        var subfolder: String

        val subfolderDialog = SubfolderDialog()
        if (subfolderDialog.showAndGet()) {
            subfolder = subfolderDialog.subfolder
        } else {
            return
        }

        // 目标包路径
        val servicePackage = "com.xhsoft.base.service"  + if (subfolder.isNotEmpty()) ".$subfolder" else ""
        val serviceDir = "xhsoft-base/src/main/java/com/xhsoft/base/service" + if (subfolder.isNotEmpty()) "/$subfolder" else ""
        val implPackage = "$servicePackage.impl"
        val implDir = "$serviceDir/impl"
        val daoPackage = "com.xhsoft.base.dao" + if (subfolder.isNotEmpty()) ".$subfolder" else ""
        val daoDir = "xhsoft-base/src/main/java/com/xhsoft/base/dao" + if (subfolder.isNotEmpty()) "/$subfolder" else ""

        FileUtils.createFile(
            project = project,
            dir = serviceDir,
            className = "I${baseName}Service",
            content = """
                package $servicePackage;
                
                import com.xhsoft.lib.ext.mvc.ICrudService;
                import $beanPackage.$beanName;
                
                public interface I${baseName}Service extends ICrudService<${beanName}> {
                }
            """
        )

        FileUtils.createFile(
            project = project,
            dir = implDir,
            className = "${baseName}Service",
            content = """
                package $implPackage;
                
                import com.xhsoft.lib.ext.mvc.AbstractCrudService;
                import $servicePackage.I${baseName}Service;
                import $beanPackage.$beanName;
                import lombok.extern.slf4j.Slf4j;
                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;
                
                @Slf4j
                @Service
                @RequiredArgsConstructor
                @Transactional(readOnly = true)
                public class ${baseName}Service extends AbstractCrudService<${beanName}> 
                    implements I${baseName}Service
                {
                }
            """
        )

        FileUtils.createFile(
            project = project,
            dir = daoDir,
            className = "${baseName}Dao",
            content = """
                package $daoPackage;
                
                import com.xhsoft.lib.ext.mvc.AbstractCrudDao;
                import $beanPackage.$beanName;
                import org.springframework.stereotype.Repository;
                
                @Repository
                public class ${baseName}Dao extends AbstractCrudDao<${beanName}> {
                }
            """
        )
    }
}
