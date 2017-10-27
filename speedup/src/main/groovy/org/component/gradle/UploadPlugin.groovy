package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.MavenPlugin

class UploadPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        def root = project.rootProject
        def uploadAll = root.tasks.getByName("uploadAll")
        def uploadForClean = root.tasks.getByName("uploadForClean")
        String[] excludes = root.excludes

        if (project == root
                || project.plugins.hasPlugin('com.android.application')) {
            return
        }

        project.plugins.apply(MavenPlugin)

        project.uploadArchives {
            repositories {
                mavenDeployer {
                    pom.groupId = "com.local.maven"
                    pom.version = "local"
                    pom.artifactId = project.path.replaceAll(':','-')
                    repository(url: project.uri(project.rootProject.localMaven))
                }
            }
        }

        // filter exclude modules
        if (excludes.contains(project.path)) {
            return
        }

        // create upload tasks
        String name = project.path.replaceAll(":", "")
        def upload = project.rootProject.tasks.create(name: "upload$name", group: 'speedup', dependsOn: "${project.path}:uploadArchives")
        upload.doLast {
            RootPlugin.log(LogLevel.LIFECYCLE, "upload ${project.path} to local maven successful!")
        }
        uploadForClean.dependsOn "${project.path}:clean"
        uploadAll.dependsOn upload
    }


}
