# Speedup

Speedup是一款针对于多组件构建加速的插件。特别是针对组件化开发，作用尤为明显。

### 什么人需要Speedup

随着项目不停迭代升级，项目越来越大，特别是最近两年开始流行了组件化之后。可能拆分的组件越来越多。
越来越细。导致项目下开启了一堆的module.每次运行的时候都会因此耗费大量的时间。

Speedup即是专用于解决此类由于堆积了一堆module之后的编译卡顿问题

### 原理

1. 将library打包发布到指定的本地maven仓库中
2. 启动运行任务时，自动查找本地build.gradle中配置的compile project依赖。替换为本地maven仓库中的aar地址

### 配置

在项目根目录下添加依赖并添加应用插件

latest = [![](https://jitpack.io/v/yjfnypeu/Speedup.svg)](https://jitpack.io/#yjfnypeu/Speedup)

```groovy
buildscript {
    repositories {
        // 添加jitpack仓库地址
        maven { url "https://jitpack.io" }
    }
    dependencies {
        // 添加插件依赖
        classpath "com.github.yjfnypeu:Speedup:$latest"
    }
}
// 应用插件
apply plugin: 'speedup'
```

插件将配置属性放入local.properties文件中进行配置。更方便个人单独使用

- local.properties

```
speedup.enable=true
excludeModules=modulepath1,modulepath2
```

1. speedup.enable
    
    此属性用于指定是否激活加速插件。默认为false。主要用于针对比如使用Jenkins打包发布时，不使用加速插件。

2. excludeModules
    
    此属性的值为module的path值。即在settings.gradle中使用include所指定的路径地址：
    如 excludeModules=:app,:lib
    
    此属性用于指定不需要被加速的组件模块, 比如说组件化中的各自的业务线组件。
    这类组件因为是各自开发业务线的主module。需要随时变更改动。所以将其排除
    
    可被排除的模块名称，需要为最顶层业务组件。即为 **只被application module所直接依赖** 的组件。
    application module被默认排除的。不需要再进行单独配置
    
    此属性的作用：
    - 当打包发布时：此处所指定的模块将会不进行打包发布。
    - 当执行启动任务时：不对compile project为此module的依赖进行动态替换。
    
### 用法

主要用法是将module进行打包发布到本地maven库中去，发布方式分为两种：

- 使用全量打包发布：

使用提供的uploadAll任务进行全量打包发布。这种发布方式一般在切换分支后进行使用。

```
./gradlew uploadAll
```

- 对于部分module打包发布

不同的library含有各自单独的upload方法，如模块名为:loginLib：

```
./gradlew uploadloginLib
```

或者可使用AS提供的gradle任务视图窗口直接进行使用：

![upload](./pics/upload.png)

    
    为什么不直接使用uploadArchives命令？是因为在AS的gradle任务视图窗口中使用uploadArchives时。
    会触发使用root project的uploadArchives命令。导致各个不需要进行编译打包的模块：如application
    module，也都会被编译执行一次。严重影响了发布速度。
    
    而使用命令行的方式执行单个的module的uploadArchives任务可以避免以上情况，但是略显复杂，容易使用不顺

成功后将在项目根目录下生成一个.repo文件夹, 其中存放打包发布的aar。请注意将此文件夹加入版本控制忽略列表中。

请注意在每次有切换分支之后。先进行clean，再进行打包发布。避免版本不一致问题出现
    
然后直接通过![launch](./pics/launch.png)进行启动。和平时开发时一样

#### 示例

****可参考[一个简单的组件化demo](https://github.com/yjfnypeu/AndroidComponent)****
    
## License
[apache 2.0](./LICENCE)