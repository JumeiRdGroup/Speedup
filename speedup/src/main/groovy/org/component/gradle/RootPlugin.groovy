package org.component.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

/**
 * 加速插件入口.
 *
 * 加速逻辑主要分为两部分，分别由其余两个插件完成
 *
 *      1. {@link UploadPlugin}
 *          打包上传插件，用于对library project打包发布到本地maven仓库
 *      2. {@link ReplacePlugin}
 *          动态替换插件，用于进行动态依赖替换。
 *
 * @author haoge
 */
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

        def uploadAll = root.tasks.create(name:'uploadAll', group: 'speedup')
        def uploadForClean = root.tasks.create(name:'uploadForClean', group: 'speedup')
        uploadAll.dependsOn uploadForClean

        project.subprojects {
            it.afterEvaluate {
                // 对每个project都应用此UploadPlugin.
                it.plugins.apply(UploadPlugin)
                if (isAssemble || isUpload) {
                    // 只针对assemble及upload任务添加动态替换插件
                    // assemble任务及upload任务均为根据project依赖链来决定哪些project需要编译的
                    // 所以对此两种任务。使用动态替换插件。可以有效减少需被编译的module数量。达到打包提速的作用
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

    // 通知日志打印方法。格式化日志
    static void log(LogLevel level = LogLevel.DEBUG, String message) {
        logger.log(level, "[Speedup] $message")
    }

    // 解析启动任务表。判断是是否含有upload或者assemble任务
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
