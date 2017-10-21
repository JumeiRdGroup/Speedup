package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
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
            RootPlugin.log("Apply ReplacePlugin to $project.name")
            if (project == root
                    || excludes.contains(project.name)
                    || project.plugins.hasPlugin('com.android.application')) {
                RootPlugin.log("Filter module:$project.name")
                return
            }

            String name = project.path.replaceAll(":", "_")
            def upload = project.rootProject.tasks.create(name: "upload$name", group: 'speedup', dependsOn: "${project.path}:uploadArchives")
            uploadAll.dependsOn upload

            project.uploadArchives {
                repositories {
                    mavenDeployer {
                        pom.groupId = "com.local.maven"
                        pom.artifactId = project.name
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
