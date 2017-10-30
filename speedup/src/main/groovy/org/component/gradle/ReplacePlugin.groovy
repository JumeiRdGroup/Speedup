package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.logging.LogLevel

class ReplacePlugin implements Plugin<Project>{

    // cache the sub dependencies with pom.xml
    private static Map<String, List<String>> cache = new HashMap<>()

    @Override
    void apply(Project project) {
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
                        RootPlugin.log(LogLevel.LIFECYCLE, "compile project with name [$name] wasn't be upload to local")
                        return
                    }
                    configuration.dependencies.remove(dependency)
                    Dependency projectModule = new DefaultExternalModuleDependency("com.local.maven", name, "local")
                    def subs = findSubDependencies(project.rootProject.localMaven, name)
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

    private static List<String> findSubDependencies(String localRepo, String name) {
        List<String> subs;
        if ((subs = cache.get(name)) != null) {
            return subs
        }

        subs = flatDependencies(localRepo, name)
        cache.put(name, subs)
        return subs
    }

    private static List<String> flatDependencies(String localRepo, String name) {
        List<String> list = new ArrayList<>()

        try {
            File file = new File(localRepo, "com/local/maven/${name}/local/${name}-local.pom")
            Node project = new XmlParser().parse(file)
            project.dependencies.dependency.each {
                if ('com.local.maven' == it.groupId[0].text()
                        && 'local' == it.version[0].text()) {
                    String artifactId = it.artifactId[0].text()
                    list.add(artifactId.replaceAll('-', ':'))
                    list.addAll(findSubDependencies(localRepo, artifactId))
                }
            }
        } catch (Exception e) {
            // ignore
            RootPlugin.log("parse pom for artifact name $name failed: cause by ${e.message}")
        }

        return list
    }
}
