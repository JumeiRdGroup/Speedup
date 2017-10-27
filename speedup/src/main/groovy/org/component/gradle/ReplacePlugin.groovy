package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.logging.LogLevel

class ReplacePlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        RootPlugin.log(LogLevel.LIFECYCLE, "Apply Replace Plugin for ${project.path}")
        project.configurations.all { Configuration configuration ->
            if (configuration.dependencies.size() == 0) {
                return
            }

            String[] excludes = project.rootProject.excludes
            // compare and replace from project to remote url
            configuration.dependencies.all { dependency ->
                if (dependency instanceof DefaultProjectDependency) {

                    String name = dependency.dependencyProject.path

                    if (excludes.contains(name)) {
                        return
                    }

                    name = name.replaceAll(':','-')
                    File repoDir = new File(project.rootProject.localMaven, "com/local/maven/$name")
                    if (!repoDir.exists()) {
                        RootPlugin.log(LogLevel.ERROR, "compile project with name [$name] wasn't be upload to local")
                        return
                    }
                    configuration.dependencies.remove(dependency)
                    Dependency projectModule = new DefaultExternalModuleDependency("com.local.maven", name, "local")
                    def subs = flatDependencies(new File(project.rootProject.localMaven, "com/local/maven").absolutePath, name)
                    excludes.each {
                        if (subs.contains(it)) {
                            projectModule.exclude group : 'com.local.maven', module: it.replaceAll(':', '-')
                            project.dependencies.add(configuration.name, project.project(it))
                        }
                    }
                    configuration.dependencies.add(projectModule)
                    RootPlugin.log("$project.name Replace dependency $dependency.name with local name [com.local.maven:$name:local] successful!")
                }
            }
        }
    }

    static List<String> flatDependencies(String localRepo, String name) {
        List<String> list = new ArrayList<>()
        File repoDir = new File(localRepo, "$name")
        if (!repoDir.exists()) {
            return list
        }

        try {
            File file = new File(localRepo, "${name}/local/${name}-local.pom")
            Node project = new XmlParser().parse(file)
            project.dependencies.dependency.each {
                if ('com.local.maven' == it.groupId[0].text()
                        && 'local' == it.version[0].text()) {
                    String artifactId = it.artifactId[0].text()
                    list.add(artifactId.replaceAll('-', ':'))
                    list.addAll(flatDependencies(localRepo, artifactId))
                }
            }
        } catch (Exception e) {
            // ignore
            RootPlugin.log(LogLevel.ERROR, e.getMessage())
            e.printStackTrace()
        }

        return list
    }
}
