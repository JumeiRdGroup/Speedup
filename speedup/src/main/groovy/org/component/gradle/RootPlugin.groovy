package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

public class RootPlugin implements Plugin<Project> {

    Project root
    boolean isAssemble
    boolean isUpload
    static Logger logger

    @Override
    void apply(Project project) {
        if (project != project.rootProject) {
            throw new RuntimeException("You must apply the plugin : 'speedup' on root build.gradle")
        }
        root = project.rootProject
        logger = project.logger
        parseTasks(project)
        boolean enable
        try {
            enable = parseLocal()
        } catch (Exception e) {
            enable = false
        }
        if (!enable) {
            log(LogLevel.ERROR, "plugin 'speedup' is disabled, you can add 'speedup.enable=true' on local.properties to enable Speedup")
            return
        }

        root.excludes.each {
            log("exclude module name $it")
        }

        // create upload tasks
        def uploadAll = root.tasks.create(name:'uploadAll', group: 'speedup')
        def uploadForClean = root.tasks.create(name:'uploadForClean', group: 'speedup')
        uploadAll.dependsOn uploadForClean

        project.subprojects {
            it.afterEvaluate {
                it.plugins.apply(UploadPlugin)
                if (isAssemble || isUpload) {
                    it.plugins.apply(ReplacePlugin)
                }
            }
        }
    }

    private boolean parseLocal() {
        // read local.properties
        Properties local = new Properties()
        local.load(root.file('local.properties').newInputStream())
        File file = new File(local.get("localRepo", '_repo') as String)
        String excludeModules = local.get("excludeModules", "") as String
        excludeModules = excludeModules.replaceAll(' ','')
        root.ext {
            excludes = excludeModules.length() == 0 ? [] as String[] : excludeModules.split(',')
            localMaven = file.absolutePath
        }

        root.subprojects {
            repositories {
                maven { url file.absolutePath}
            }
        }

        return Boolean.parseBoolean(local.get("speedup.enable") as String)
    }

    static void log(LogLevel level = LogLevel.DEBUG, String message) {
        logger.log(level, "[Speedup] $message")
    }

    private void parseTasks(Project project){
        project.gradle.startParameter.taskNames.each {
            if (it.contains("assemble")) {
                isAssemble = true
            } else if (it.contains('upload')) {
                isUpload = true
            }
        }
    }
}
