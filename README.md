English | [简体中文](./README_cn.md)

# Tuya Device IPC SDK Sample for Android

## Demo usage

The demo provides device activation, P2P channel connection, sending and receiving data via P2P, and other functions for IP camera development.

## Preparation

Add the following configuration to your `local.properties` file:

```groovy
UUID=your uuid  
AUTHKEY=your key  
PID=your pid
```

The demo interface will show the activation QR code, just use the Tuya Smart app to scan the code.

## Access

```groovy
implementation 'com.tuya.smart:tuyasmart-iot_qr_p2p:0.0.8'
implementation 'com.tencent.mars:mars-xlog:1.2.3'
```

Add the repository address to the project root `build.gradle` file.

```groovy
maven { url 'https://dl.bintray.com/tuyasmartai/sdk' }
jcenter()
```

If obfuscation is enabled, add in the `proguard-rules.pro` file.

```groovy
-keep class com.tuya.smartai.** {*;}
-keep class com.tuya.smart.** {*;}
-keep class com.tencent.mars.** {*;}
```

## Permission requirements

```java
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

## Initialization

```java
/**
     * initialize SDK (Note! 1. one UUID can't be activated on more than one device at the same time; 2. the same process can only be initialized once, and you need to kill the process where initialization is done when you exit)
     * @param context
     * @param basePath: storage path Example: "/sdcard/tuya_iqp/"
     * @param productId: product ID
     * @param UUID: user ID
     * @param authKey: Authentication key
     * @param version: firmware version number (for OTA)
     * @param callback0: IoT related callback method
     * @param callback1: P2P related callback method
     * @return
     */
IQPManager.getInstance().init(Context context, String basePath, String productId, String uuid, String authorKey, String version, IoTCallback callback0, P2PStatusCallback callback1);

public interface IoTCallback {

        /**
                 * dp event reception
                 * @param event
                 * 
                 * event value(event.value)
                 * event id(event.dpid)
                 * event type(event.type)
                 * DPEvent.Type.PROP_BOOL
                 * DPEvent.Type.PROP_VALUE
                 * Type.PROP_STR
                 * PROP_ENUM
                 * Type.PROP_BITMAP
                 * Type.PROP_RAW
                 */
        void onDpEvent(DPEvent event);

        // unbind the device callback (please restart the APP process here, otherwise it will affect the secondary network allocation)
        void onReset();

        //receive the short link of the QR code of the wiring network (null in case of failure to get it)
        void onShorturl(String url);
        
        /**
         * MQTT status change
         * @param status IoTSDKManager.STATUS_OFFLINE Network offline; 
         * IoTSDKManager.STATUS_MQTT_OFFLINE network online MQTT offline; 
         * IoTSDKManager.STATUS_MQTT_ONLINE network online MQTT online
         */
        void onMQTTStatusChanged(int status);
        
        // Device activation
        void onActive();
        
        // First activation of the device
        void onFirstActive();
        
    }

public interface P2PStatusCallback {

        /**
         * p2p connection established successfully
         * @param handle connection handle
         */
        void onConnectSuccess(int handle);

        /**
         * p2p connection failed to be established
         * @param err error code
         */
        void onConnectFail(int err);

        /**
         * p2p connection disconnected
         * @param handle connection handle
         * @param channel channel
         */
        void onDisconnect(int handle, int channel);

        /**
         * p2p receive data callback
         * @param handle connection handle
         * @param channel channel
         * @param data data
         */
        void onRecvData(int handle, int channel, byte[] data);

    }
```

## Function

### IoT related

``` java
// Get the IoT interface object
IoTSDKManager ioTSDKManager = IQPManager.getInstance().getIoT();

// local unbinding (asynchronous operation, successful unbinding will enter onReset callback)
ioTSDKManager.reset();

/**
     * Send dp event
     * @param id dp id
     * @param type type DPEvent.
     * Type.PROP_BOOL boolean
     * Type.PROP_VALUE int
     * Type.PROP_STR string
     * PROP_ENUM int
     PROP_RAW byte[] * DPEvent.
     * @param val value
     * @return
     */
ioTSDKManager.sendDP(int id, int type, Object val)

/**
     * Send multiple dp events
     *
     * @param events Multiple dp types
     * @return
     */
ioTSDKManager.sendDP(DPEvent... events)

/**
     * send dp event with timestamp
     *
     * @param id dp id
     * @param type type DPEvent.
     * @param val value
     * @param timestamp timestamp in seconds
     * @return
     */
ioTSDKManager.sendDPWithTimeStamp(int id, int type, Object val, int timestamp)


/**
     * Send multiple dp events with timestamp (timestamp needs to be assigned in DPEvent.timestamp)
     *
     * @param events Multiple dp types
     * @return
     */
ioTSDKManager.sendDPWithTimeStamp(DPEvent... events)

/**
     * Send http request
     * @param apiName request api
     * @param apiVersion version number
     * @param jsonMsg parameter json
     * @return
     */
ioTSDKManager.httpRequest(String apiName, String apiVersion, String jsonMsg)


// Get the device id
ioTSDKManager.getDeviceId()

//Get the server time
ioTSDKManager.getUniTime()

// The SDK already provides a default implementation, no need to extend this method if you don't need it.
// TODO

```

### P2P related

```java
//Get the P2P interface object
P2PSDKManager p2pSDKManager = IQPManager.getInstance().getP2P();

/**
 * Send data
 * @param handle handle
 * @param channel channel
 * @param data data
 * @return send success returns the length of the data sent (return > 0); send failure returns the error code (return < 0)
 */
p2pSDKManager.send(int handle, int channel, byte[] data)
```

## Technical support

You can get support from Tuya Smart with the following methods:

* [Tuya Smart document center](https://developer.tuya.com/en/docs/iot)
* [Submit a ticket](https://service.console.tuya.com/)

## License

This project is licensed under the MIT License.
