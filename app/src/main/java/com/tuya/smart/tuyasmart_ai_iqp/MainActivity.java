package com.tuya.smart.tuyasmart_ai_iqp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tuya.smart.ai.iot_qr_p2p.IQPManager;
import com.tuya.smartai.iot_sdk.DPEvent;
import com.tuya.smartai.iot_sdk.IoTSDKManager;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import org.json.JSONObject;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";

    private String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private final int PERMISSION_CODE = 123;

    private int connectHandle = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!EasyPermissions.hasPermissions(this, requiredPermissions)) {
            EasyPermissions.requestPermissions(this, "需要授予权限以使用设备", PERMISSION_CODE, requiredPermissions);
        } else {
            init();
        }
    }

    private void init() {

        ZXingLibrary.initDisplayOpinion(this);

        findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IQPManager.getInstance().getIoT().reset();
            }
        });

        findViewById(R.id.send_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectHandle > 0) {
                    int send = IQPManager.getInstance().getP2P().send(connectHandle, 0, "hello from server".getBytes());
                    Log.d(TAG, "send return: " + send);
                }
            }
        });

        int[] channels = new int[8];
        channels[0] = 1024;
        channels[1] = 1024 * 10;
        channels[2] = 1024 * 100;

        IQPManager.getInstance().init(this, "/sdcard/", "xqcwrcjnq6smfygq"
                , "tuyacbd1a0db4302bdfe", "cAlyuW2VmwNyRcv93xLVSIrBy5n5vuge", "1.0.0", new IoTSDKManager.IoTCallback() {
                    @Override
                    public void onDpEvent(DPEvent event) {
                        if (event != null) {
                            Log.w(TAG, "rev dp: " + event);
                            if (event.type == DPEvent.Type.PROP_RAW) {
                                Log.w(TAG, Base64.encodeToString((byte[]) event.value, Base64.DEFAULT));
                            }
                        }
                    }

                    @Override
                    public void onReset() {
                        Intent mStartActivity = getPackageManager().getLaunchIntentForPackage(getPackageName());
                        if (mStartActivity != null) {
                            int mPendingIntentId = 123456;
                            PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId
                                    , mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, mPendingIntent);
                            Runtime.getRuntime().exit(0);
                        }
                    }

                    @Override
                    public void onShorturl(String urlJson) {
                        Log.w(TAG, "shorturl: " + urlJson);

                        final String url = (String) com.alibaba.fastjson.JSONObject.parseObject(urlJson).get("shortUrl");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView qrcode = findViewById(R.id.qrcode);
                                qrcode.setVisibility(View.VISIBLE);
                                qrcode.setImageBitmap(CodeUtils.createImage(url, 400, 400, null));
                            }
                        });
                    }

                    @Override
                    public void onActive() {
                        Log.w(TAG, "onActive: " + IQPManager.getInstance().getIoT().getDeviceId());
                    }

                    @Override
                    public void onFirstActive() {
                        Log.w(TAG, "onFirstActive");
                    }

                    @Override
                    public void onMQTTStatusChanged(int status) {
                        Log.e(TAG, "Status: " + status);
                        switch (status) {
                            case IoTSDKManager.STATUS_OFFLINE:
                                // 设备网络离线
                                break;
                            case IoTSDKManager.STATUS_MQTT_OFFLINE:
                                // 网络在线MQTT离线
                                break;
                            case IoTSDKManager.STATUS_MQTT_ONLINE:
                                // 网络在线MQTT在线
                                break;
                        }
                    }

                    @Override
                    public void onMqttMsg(int protocol, JSONObject msgObj) {

                    }


                }, new IQPManager.P2PStatusCallback() {

                    @Override
                    public void onConnectSuccess(int handle) {
                        connectHandle = handle;
                    }

                    @Override
                    public void onConnectFail(int err) {

                    }

                    @Override
                    public void onDisconnect(int handle, int channel) {

                    }

                    @Override
                    public void onRecvData(int handle, int channel, byte[] data) {

                    }
                }, channels);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        init();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
