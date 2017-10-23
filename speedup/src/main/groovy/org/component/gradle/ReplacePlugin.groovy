package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.logging.LogLevel

public class ReplacePlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        project.configurations.all { configuration ->
            if (configuration.dependencies.size() == 0) {
                return
            }
            configuration.dependencies.all { dependency ->
                if (dependency instanceof DefaultProjectDependency) {
                    // compare and replace from project to remote url
                    String name = dependency.dependencyProject.path

                    if (project.rootProject.excludes.contains(name)) {
                        return
                    }

                    name = name.replaceAll(':','-')
                    File repoDir = new File(project.rootProject.localMaven, "com/local/maven/$name")
                    if (!repoDir.exists()) {
                        RootPlugin.log(LogLevel.ERROR, "compile project with name [$name] wasn't be upload to local")
                        return
                    }
                    configuration.dependencies.remove(dependency)
                    configuration.dependencies.add(new DefaultExternalModuleDependency("com.local.maven", name, "local"))
                    RootPlugin.log("$project.name Replace dependency $dependency.name with local name [com.local.maven:$name:local] successful!")
                }
            }
        }

    }
}
