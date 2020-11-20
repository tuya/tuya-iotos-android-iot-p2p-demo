# Android IoT & P2P SDK

## demo使用
demo 提供了设备云端激活、P2P通道建联、通过P2P数据收发等功能。

> 运行前的准备工作  
> 在你的local.properties文件中增加如下配置:

```groovy
UUID=你的uuid  
AUTHKEY=你的key  
PID=你的pid
```

demo界面会展示激活二维码，使用涂鸦智能APP扫码即可。


## 接入

```groovy
implementation 'com.tuya.smart:tuyasmart-iot_qr_p2p:0.0.1'
implementation 'com.tencent.mars:mars-xlog:1.2.3'
```

> 在项目根目录build.gradle中添加仓库地址

```groovy
maven { url 'https://maven-other.tuya.com/repository/maven-releases/'}
maven { url 'https://maven-other.tuya.com/repository/maven-snapshots/'}

```

> 如果开启了混淆，在proguard-rules.pro文件中添加

```groovy
-keep class com.tuya.smartai.** {*;}
-keep class com.tuya.smart.** {*;}
-keep class com.tencent.mars.** {*;}

```

> 权限要求

```java
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 初始化

```java
/**
     * 初始化SDK (注意！ 1.一个uuid不能同时在多个设备上激活；2.同一个进程只能初始化一次，退出时需要杀掉初始化所在进程)
     * @param context
     * @param basePath  存储路径 示例："/sdcard/tuya_iqp/"
     * @param productId 产品id
     * @param uuid  用户id
     * @param authKey 认证key
     * @param version 固件版本号（OTA用）
     * @param callback0 IoT相关回调方法
     * @param callback1 P2P相关回调方法
     * @return
     */
IQPManager.getInstance().init(Context context, String basePath, String productId, String uuid, String authorKey, String version, IoTCallback callback0, P2PStatusCallback callback1);

public interface IoTCallback {

        /**
                 * dp事件接收
                 * @param event
                 * 
                 * 事件值(event.value)
                 * 事件id(event.dpid)
                 * 事件类型(event.type)
                 * DPEvent.Type.PROP_BOOL
                 * DPEvent.Type.PROP_VALUE
                 * DPEvent.Type.PROP_STR
                 * DPEvent.Type.PROP_ENUM
                 * DPEvent.Type.PROP_BITMAP
                 * DPEvent.Type.PROP_RAW
                 */
        void onDpEvent(DPEvent event);

        //解绑设备回调 (请在此处重启APP进程，否则会影响二次配网)
        void onReset();

        //收到配网二维码短链（获取失败时为null）
        void onShorturl(String url);
        
        /**
         * MQTT状态变化
         * @param status IoTSDKManager.STATUS_OFFLINE 网络离线; 
         *               IoTSDKManager.STATUS_MQTT_OFFLINE 网络在线MQTT离线; 
         *               IoTSDKManager.STATUS_MQTT_ONLINE 网络在线MQTT在线
         */
        void onMQTTStatusChanged(int status);
        
        //设备激活
        void onActive();
        
        //设备初次激活
        void onFirstActive();
        
    }


public interface P2PStatusCallback {

        /**
         * p2p连接建立成功
         * @param handle 连接句柄
         */
        void onConnectSuccess(int handle);

        /**
         * p2p连接建立失败
         * @param err 错误码
         */
        void onConnectFail(int err);

        /**
         * p2p连接断开
         * @param handle 连接句柄
         * @param channel 通道
         */
        void onDisconnect(int handle, int channel);

        /**
         * p2p接收数据回调
         * @param handle 连接句柄
         * @param channel 通道
         * @param data 数据
         */
        void onRecvData(int handle, int channel, byte[] data);

    }

```

## 功能
### IoT相关

```java
//获取IoT接口对象
IoTSDKManager ioTSDKManager = IQPManager.getInstance().getIoT();

//本地解绑 (异步操作，解绑成功会进入onReset回调)
ioTSDKManager.reset();

/**
     * 发送dp事件
     * @param id dp id
     * @param type 类型 DPEvent.Type
     * DPEvent.Type.PROP_BOOL   boolean
     * DPEvent.Type.PROP_VALUE  int
     * DPEvent.Type.PROP_STR    string
     * DPEvent.Type.PROP_ENUM   int
     * DPEvent.Type.PROP_RAW    byte[]
     * @param val 值
     * @return
     */
ioTSDKManager.sendDP(int id, int type, Object val)

/**
     * 发送多个dp事件
     *
     * @param events 多个dp类型
     * @return
     */
ioTSDKManager.sendDP(DPEvent... events)

/**
     * 发送dp事件带时间戳
     *
     * @param id   dp id
     * @param type 类型 DPEvent.Type
     * @param val  值
     * @param timestamp 时间戳 单位秒
     * @return
     */
ioTSDKManager.sendDPWithTimeStamp(int id, int type, Object val, int timestamp)


/**
     * 发送多个dp事件带时间戳（时间戳需要赋值在DPEvent.timestamp）
     *
     * @param events 多个dp类型
     * @return
     */
ioTSDKManager.sendDPWithTimeStamp(DPEvent... events)

/**
     * 发送http请求
     * @param apiName 请求api
     * @param apiVersion 版本号
     * @param jsonMsg   参数json
     * @return
     */
ioTSDKManager.httpRequest(String apiName, String apiVersion, String jsonMsg)


//获取设备id
ioTSDKManager.getDeviceId()

//获取服务器时间
ioTSDKManager.getUniTime()

//自定义实现网络状态监测，返回值为网络是否离线。SDK已提供默认实现，如无需要不必扩展此方法。
//TODO

```

### P2P相关

```java
//获取P2P接口对象
P2PSDKManager p2pSDKManager = IQPManager.getInstance().getP2P();

/**
 * 发送数据
 * @param handle 句柄
 * @param channel 通道
 * @param data 数据
 * @return 发送成功返回发送的数据长度（return > 0）;发送失败返回错误码(return < 0)
 */
p2pSDKManager.send(int handle, int channel, byte[] data)

```
