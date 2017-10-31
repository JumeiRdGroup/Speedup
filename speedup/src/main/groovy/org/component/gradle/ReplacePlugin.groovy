package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import org.gradle.api.logging.LogLevel

/**
 * 动态替换任务插件
 *
 * @author haoge
 */
class ReplacePlugin implements Plugin<Project>{

    private static Map<String, List<String>> cache = new HashMap<>()
    private String localRepo
    private String[] excludes

    @Override
    void apply(Project project) {
        RootPlugin.log(LogLevel.DEBUG, "Apply Replace plugin for ${project.path}")
        localRepo = project.rootProject.localMaven
        excludes = project.rootProject.excludes

        project.configurations.all { Configuration configuration ->
            if (configuration.dependencies.size() == 0) {
                return
            }

            configuration.dependencies.all { dependency ->
                if (dependency instanceof DefaultProjectDependency) {
                    // 过滤Project依赖，并针对excludes进行过滤。被过滤的不参与替换操作
                    String name = dependency.dependencyProject.path
                    if (excludes.contains(name)) {
                        return
                    }

                    // 过滤project所对应的本地依赖不在仓库中的情况。不进行替换
                    name = name.replaceAll(':','-')
                    if (!isLocalExist(name)) {
                        RootPlugin.log(LogLevel.DEBUG, "compile project with name [$name] wasn't be upload to local")
                        return
                    }

                    /*
                     * 进行动态替换。
                     * 1. 从依赖链中移除原来的project
                     * 2. 读取对应依赖的子依赖。遍历其子依赖，并找出需要被exclude排除(比如被excludes所指出的或者本地仓库中没有对应库的)的本地子依赖
                     * 3. exclude所有过滤出来的子依赖并替换回project依赖
                     */
                    configuration.dependencies.remove(dependency)
                    Dependency projectModule = new DefaultExternalModuleDependency("com.local.maven", name, "local")

                    def subs = findExcludes(name)
                    RootPlugin.log(LogLevel.DEBUG, "find sub excludes on [$name] = $subs")
                    subs.each {
                        RootPlugin.log(LogLevel.DEBUG, "exclude module $it and replace it with project ${it.replaceAll('-', ':')}")
                        projectModule.exclude group : 'com.local.maven', module: it
                        project.dependencies.add(configuration.name, project.project(it.replaceAll('-', ':')))
                    }

                    configuration.dependencies.add(projectModule)
                    RootPlugin.log("$project.name Replace dependency $dependency.name with local name [com.local.maven:$name:local] successful!")
                }
            }
        }
    }

    private List<String> findExcludes(String name) {
        def subExcludes = []

        if (!isLocalExist(name)) {
            subExcludes.add(name)
            return subExcludes
        }

        def subDependencies = flatDependencies(name)
        RootPlugin.log(LogLevel.DEBUG, "find sub dependencies for $name = $subDependencies")
        subDependencies.each {
            if (excludes.contains(it.replaceAll('-',':'))) {
                subExcludes.add(it)
            } else {
                subExcludes.addAll(findExcludes(it))
            }
        }
        return subExcludes
    }

    private List<String> flatDependencies(String name) {
        List<String> list = cache.get(name)
        if (list != null) {
            return list
        }

        list = new ArrayList<>()
        try {
            File file = new File(localRepo, "com/local/maven/${name}/local/${name}-local.pom")
            Node project = new XmlParser().parse(file)
            project.dependencies.dependency.each {
                if ('com.local.maven' == it.groupId[0].text()
                        && 'local' == it.version[0].text()) {
                    String artifactId = it.artifactId[0].text()
                    list.add(artifactId)
                }
            }
        } catch (Exception e) {
            // ignore
            RootPlugin.log("parse pom for artifact name $name failed: cause by ${e.message}")
        }
        cache.put(name, list)
        return list
    }

    private boolean isLocalExist(String name) {
        File repoDir = new File(localRepo, "com/local/maven/$name")
        return repoDir.exists()
    }
}
