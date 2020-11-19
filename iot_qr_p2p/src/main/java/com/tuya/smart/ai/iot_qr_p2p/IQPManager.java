package com.tuya.smart.ai.iot_qr_p2p;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;
import android.view.View;

import com.tuya.smart.p2p_sdk.P2PSDKManager;
import com.tuya.smart.p2p_sdk.interfaces.IP2PCallback;
import com.tuya.smart.p2p_sdk.interfaces.IP2PServerCallback;
import com.tuya.smartai.iot_sdk.DPEvent;
import com.tuya.smartai.iot_sdk.IoTSDKManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.reactivex.disposables.Disposable;

public class IQPManager {

    private final static String TAG = "IQPManager";

    private static volatile IQPManager INSTANCE = null;

    private int[] mChannels;

    private IQPManager() {
    }

    public static IQPManager getInstance() {
        if (INSTANCE == null) {
            synchronized (IQPManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new IQPManager();
                }
            }
        }
        return INSTANCE;
    }

    private IoTSDKManager ioTSDKManager;

    private P2PSDKManager p2PSDKManager;

    /**
     * 初始化SDK
     * @param context 上下文
     * @param basePath  存储路径
     * @param productId 产品id
     * @param uuid      用户id
     * @param authKey   认证key
     * @param callback  IoT回调方法
     * @param callbackP P2P回调方法
     * @param channels  数据通道(index为通道, value为通道缓存大小)
     */
    public void init(Context context, String basePath, String productId, String uuid, String authKey, String version
            , final IoTSDKManager.IoTCallback callback, P2PStatusCallback callbackP, int[] channels) {

        this.mCb = callbackP;
        this.mChannels = channels;

        ioTSDKManager = new IoTSDKManager(context);

        ioTSDKManager.setP2PCallback(new IoTSDKManager.IP2PCallback() {
            @Override
            public void onP2PSignal(String id, String msg) {
                Log.w(TAG, String.format("onP2PSignal: %s %s", id, msg));
                try {
                    JSONObject jsonObject = new JSONObject(msg);
                    String signaling = jsonObject.getJSONObject("data").getString("signaling");
                    p2PSDKManager.setSignaling(id, signaling);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        ioTSDKManager.initSDK(basePath, productId, uuid, authKey, version, new IoTSDKManager.IoTCallback() {
            @Override
            public void onDpEvent(DPEvent event) {
                callback.onDpEvent(event);
            }

            @Override
            public void onReset() {
                callback.onReset();
            }

            @Override
            public void onShorturl(String url) {
                callback.onShorturl(url);
            }

            @Override
            public void onActive() {
                callback.onActive();

                initP2P(ioTSDKManager.getDeviceId(), mChannels);
            }

            @Override
            public void onFirstActive() {
                callback.onFirstActive();
            }

            @Override
            public void onMQTTStatusChanged(int status) {
                callback.onMQTTStatusChanged(status);
            }

            @Override
            public void onMqttMsg(int protocol, JSONObject msgObj) {
                callback.onMqttMsg(protocol, msgObj);
            }
        });
    }

    public interface P2PStatusCallback {

        /**
         * p2p连接建立成功
         *
         * @param handle 连接句柄
         */
        void onConnectSuccess(int handle);

        /**
         * p2p连接建立失败
         *
         * @param err 错误码
         */
        void onConnectFail(int err);

        /**
         * p2p连接断开
         *
         * @param handle  连接句柄
         * @param channel 通道
         */
        void onDisconnect(int handle, int channel);

        /**
         * p2p接收数据回调
         *
         * @param handle  连接句柄
         * @param channel 通道
         * @param data    数据
         */
        void onRecvData(int handle, int channel, byte[] data);

    }

    private P2PStatusCallback mCb;

    /**
     * 初始化P2P
     *
     * @param id devId
     */
    private void initP2P(String id, int[] channels) {

        p2PSDKManager = P2PSDKManager.getInstance();

        int errCode = p2PSDKManager.init(id, channels);

        Log.e(TAG, String.format("p2p init err: %d", errCode));

        if (errCode != 0) {
            return;
        }

        p2PSDKManager.runServer(new IP2PServerCallback() {
            @Override
            public void onConnect(int handle) {
                Log.d(TAG, "onConnect: " + handle);

                if (mCb != null) {
                    mCb.onConnectSuccess(handle);
                }
            }

            @Override
            public void onFail(int err) {
                Log.e(TAG, "onFail: " + err);
                if (mCb != null) {
                    mCb.onConnectFail(err);
                }
            }
        });

        p2PSDKManager.setP2PCallback(new IP2PCallback() {
            @Override
            public void recvData(int handle, int channelId, byte[] data) {
                Log.d(TAG, String.format("recvData: %d %d %s", handle, channelId
                        , new String(data)));

                if (mCb != null) {
                    mCb.onRecvData(handle, channelId, data);
                }
            }

            @Override
            public void onSignaling(String id, String signaling) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("id", id);
                    jsonObject.put("signaling", signaling);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ioTSDKManager.sendMqtt(302, jsonObject.toString());
            }

            @Override
            public void onDisconnect(int handle, int channel) {
                Log.w(TAG, "onDisconnect: " + handle);

                if (mCb != null) {
                    mCb.onDisconnect(handle, channel);
                }
            }

        });

    }

    public IoTSDKManager getIoT() {
        return ioTSDKManager;
    }

    public P2PSDKManager getP2P() {
        return p2PSDKManager;
    }


}
