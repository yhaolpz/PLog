[![](https://jitpack.io/v/yhaolpz/PLog.svg)](https://jitpack.io/#yhaolpz/PLog)

[![](https://github.com/yhaolpz/PLog/blob/master/logo.png)](https://github.com/yhaolpz/PLog)

PLog 即 Persistence Log，可持久化日志于文件，便于还原用户使用场景，解决异常问题。

## 特性：

1.mmap 方式高效写入，规避 IO 操作带来的性能消耗

2.兼容多进程并发文件写入，日志文件保存在各自的进程目录下

3.通过 zip 压缩并加密，节省上报流量，保护日志私密性

4.内置收集 Activity/Fragment 生命周期、崩溃、网络状态等常用信息：

## 集成：

```java
     //配置工程 gradle
     maven { url 'https://jitpack.io' }

     // dependencies
     implementation 'com.github.yhaolpz:PLog:1.1'
```

## 使用

##### 1.初始化
```java
    PLog.Config config = new PLog.Config.Builder(this)
            .logDir(mLogDirPath) //日志存放目录，默认优先存储于SD卡
            .logcatDebugLevel(PLog.DebugLevel.DEBUG) //允许输出到Logcat的级别
            .recordDebugLevel(PLog.DebugLevel.DEBUG) //允许记录到日志文件的级别
            .fileSizeLimitDay(15) //单天日志文件存储上限
            .overdueDay(3) //日志文件过期天数
            .cipherKey("123456") //日志密钥
            .build();
    PLog.init(config);
```
##### 2.打印
```java
    //普通打印
    PLog.d("wyh", "This is a log that can be recorded in a file");

    //Format
    PLog.d("wyh", "This is a %s", "log");

    //数组类型
    PLog.d("wyh", new String[]{"a", "b", "c"});
    //output: [a,b,c]
```
##### 3.只记录到日志文件
```java
    PLog.record(PLog.DebugLevel.DEBUG,"wyh","This is a log that can only be recorded in files");
```
##### 4.只输出到logcat
```java
    PLog.print(PLog.DebugLevel.DEBUG,"wyh","This is a log");
```
##### 5.触发上传
```java
    PLog.upload(new UploadListener() {
        @Override
        public void upload(@NonNull List<File> files) {
            //上传到你的服务端
            //...

            //建议上传成功及时删除日志文件
            for (File file : files) {
                 if (file.exists()) {
                    file.delete();
                 }
            }
        }
    });
```


## 更多

[存储逻辑](https://github.com/yhaolpz/PLog/blob/master/MORE.md)

















