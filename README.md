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

1. 在项目根目录下添加依赖并添加应用插件

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
excludeModules=modulename1,modulename2
```

1. speedup.enable
    
    此属性用于指定是否激活加速插件。默认为false。主要用于针对比如使用Jenkins打包发布时，不使用加速插件。

2. excludeModules

    此属性用于指定不需要被加速的组件模块名称。比如说组件化中的各自的业务线组件。
    这类组件因为是各自开发业务线的主module。需要随时变更改动。所以将其排除
    可被排除的模块名称，需要为最顶层业务组件。即直接只被application module所依赖的组件。
    application module被默认排除的。不需要再进行单独配置
    
    此属性的作用：
    - 当打包发布时：此处所指定的模块将会不进行打包发布。
    - 当执行启动任务时：不对compile project为此module的依赖进行动态替换。
    
### 用法

1. 通过执行uploadArchives任务。将需要发布的module进行打包发布。

```
./gradlew uploadArchives
```

成功后将在项目根目录下生成一个.repo文件夹, 其中存放打包发布的aar。请注意将此文件夹加入版本控制忽略列表中。

请注意在每次有切换分支之后。先进行clean，再进行打包发布。避免版本不一致问题出现

对于非被排除的组件有修改时，请注意对该组件进行再次打包发布。替换本地仓库中的aar。保证使用正确。
如模块名为login, 可使用以下方式进行重新打包：

```
./gradlew :login:uploadArchives
```
    
2. 然后直接通过![launch](./pics/launch.png)进行启动。和平时开发时一样

#### 示例

****可参考[一个简单的组件化demo](https://github.com/yjfnypeu/AndroidComponent)****
    
## License
[apache 2.0](./LICENCE)