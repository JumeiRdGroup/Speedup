package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.MavenPlugin

public class UploadPlugin implements Plugin<Project>{
    @Override
    void apply(Project project) {
        def root = project.rootProject
        def uploadAll = root.tasks.getByName("uploadAll")

        project.plugins.apply(MavenPlugin)
        project.afterEvaluate {
            String[] excludes = root.excludes
            project.plugins.apply(ReplacePlugin)
            if (project == root
                    || excludes.contains(project.path)
                    || project.plugins.hasPlugin('com.android.application')) {
                RootPlugin.log("Filter module $project.path")
                return
            }

            // create upload tasks
            String name = project.path.replaceAll(":", "")
            def upload = project.rootProject.tasks.create(name: "upload$name", group: 'speedup', dependsOn: "${project.path}:uploadArchives")
            upload.doLast {
                RootPlugin.log(LogLevel.LIFECYCLE, "upload ${project.path} to local maven successful!")
            }
            uploadAll.dependsOn upload

            project.uploadArchives {
                repositories {
                    mavenDeployer {
                        pom.groupId = "com.local.maven"
                        pom.artifactId = project.path.replaceAll(':','-')
                        pom.version = "local"
                        repository(url: project.uri(project.rootDir.absolutePath + "/.repo"))

                        pom.whenConfigured { pom ->
                            pom.dependencies.forEach { dep ->
                                if (dep.getVersion() == "unspecified") {
                                    dep.setGroupId("com.local.maven")
                                    dep.setVersion("local")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
