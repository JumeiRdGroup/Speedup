package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

public class RootPlugin implements Plugin<Project> {

    static def logger
    @Override
    void apply(Project project) {
        if (project != project.rootProject) {
            throw new RuntimeException("You must apply the plugin : 'speedup' on root build.gradle")
        }
        logger = project.logger
        Properties local = new Properties()
        def is = project.rootProject.file('local.properties').newInputStream()
        local.load(is)
        boolean enable = local.get("speedup.enable", false)
        if (!enable) {
            log(LogLevel.ERROR, "plugin 'speedup' is disabled")
            return
        }
        project.ext {
            excludes = (local.get("excludeModules", "") as String).split(',')
        }
        log("exclude modules === $project.excludes")
        project.subprojects {
            it.plugins.apply(UploadPlugin)
        }
    }

    static void log(LogLevel level = LogLevel.LIFECYCLE, String message) {
        logger.log(level, "[Speedup] $message")
    }

}
