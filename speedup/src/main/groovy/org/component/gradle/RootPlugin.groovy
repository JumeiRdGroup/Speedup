package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

public class RootPlugin implements Plugin<Project> {

    Project root;
    static Logger logger
    @Override
    void apply(Project project) {
        if (project != project.rootProject) {
            throw new RuntimeException("You must apply the plugin : 'speedup' on root build.gradle")
        }
        root = project.rootProject
        logger = project.logger
        // read local.properties
        Properties local = new Properties()
        local.load(project.rootProject.file('local.properties').newInputStream())
        boolean enable
        try {
            enable = Boolean.parseBoolean(local.get("speedup.enable") as String)
            root.ext {
                excludes = (local.get("excludeModules", "") as String).replaceAll(' ', '').split(',')
            }
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

        // create uploadAll task
        root.tasks.create(name:'uploadAll', group: 'speedup', dependsOn: 'clean')

        project.subprojects {
            it.plugins.apply(UploadPlugin)
        }
    }

    static void log(LogLevel level = LogLevel.DEBUG, String message) {
        logger.log(level, "[Speedup] $message")
    }

}
