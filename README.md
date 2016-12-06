# 用户识别并预处理

## 1. 概述

如果一个 APP 在使用 Sensors Analytics 之前已经有很多用户，现在开始使用 Sensors Analytics 并嵌入了客户端 SDK 如 Android 和 iOS SDK，可能不能准确识别新安装，原因如下：

嵌入 SDK 后 APP 启动时，发送 $AppStart 事件，其中一个属性 `$is_first_time` 用于标记是否第一次使用，这个值是根据 SDK 内的变量判断的，如果老用户的 APP 升级到嵌入 SDK 的版本，直接使用初始值会导致认为这个用户是新安装的用户。

解决这个问题的方法有多种，这里提供一种在服务端使用 `数据预处理模块` 预处理数据的方案。更多关于 `数据预处理模块` 可以参考：

[ExtProcessor 数据预处理模块](https://www.sensorsdata.cn/manual/ext_processor.html)

使用该方法需要所有老用户的 ID 列表。

## 2. 使用

### 2.1 下载代码

```bash
git clone https://github.com/sensorsdata/ext-processor-identify-old-users.git
cd ext-processor-identify-old-users
```

### 2.2 根据实际使用修改

如果客户端使用了 `trackInstallation` 来做渠道追踪，这时需要在本模块中配置 `trackInstallation` 的事件名，例如 SDK 代码是：

```
    // 获取当前时间
    NSDate *currentTime = [NSDate date];
    // 在 App 首次启动时，追踪 App 激活事件，并记录首次激活事件
    [[SensorsAnalyticsSDK sharedInstance] trackInstallation:@"XXXXXXAppInstall" 
                                             withProperties:@{
                                                 @"FirstUseTime" : currentTime}];
```

那么需要修改 `src/main/java/cn/sensorsdata/sample/ExtProcessorForIdentifyOldUsers.java` 中常量 `TRACK_INSTALLATION_EVENT_NAME` 的值为 `XXXXXXAppInstall`。

### 2.3 生成老用户 ID 列表

将老用户 ID 列表写入文本文件 `id.txt`，一行一个 ID 例如：

```
user1
user2
user3
user4
```

将 `id.txt` 覆盖 `ext-processor-identify-old-users` 里之前的文件。

### 2.4 生成部署包

运行脚本生成部署包：

```bash
bash re-build-db.sh
```

生成的 JAR 包位于：

```
target/ext-processor-identify-old-users-0.1.jar
```

### 2.5 测试

将 `ext-processor-identify-old-users-0.1.jar` 拷贝到部署 Sensors Analytics 的机器上，运行命令启动测试运行：

```
~/sa/extractor/bin/ext-processor-utils --jar ext-processor-identify-old-users-0.1.jar --class cn.sensorsdata.sample.ExtProcessorForIdentifyOldUsers --method run
```

输入一行输入（使用一个老用户 ID 替换 distinct_id）：

```
{"distinct_id":"user4","time":1434556935000,"type":"track","event":"$AppStart","properties":{"$is_first_time":true}}
```

若输出：

```
{"distinct_id":"user4","time":1434556935000,"type":"track","event":"$AppStart","properties":{"$is_first_time":false}}
```

则说明预处理模块有效。

### 2.6 部署

运行如下命令部署：

```
~/sa/extractor/bin/ext-processor-utils --jar ext-processor-identify-old-users-0.1.jar --class cn.sensorsdata.sample.ExtProcessorForIdentifyOldUsers --method install
```

如果部署后需要修改，比如修改了 ID 列表，可以重新打包，使用上面同样的命令测试部署，直接部署即可，不需要先卸载。