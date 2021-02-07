package com.tuya.smart.tuyasmart_ai_iqp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tuya.smart.ai.iot_qr_p2p.IQPManager;
import com.tuya.smartai.iot_sdk.DPEvent;
import com.tuya.smartai.iot_sdk.IoTSDKManager;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";

    private String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private final int PERMISSION_CODE = 123;

    private int connectHandle = -1;

    TextView console;

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

    private void output(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                console.append(text + "\n");
                com.tuya.smartai.iot_sdk.Log.d(TAG, text);
            }
        });
    }

    private void clear() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                console.setText("接收日志在这里输出(长按清除): \n");
            }
        });
    }

    private void init() {
        HandlerManager.getInstance().setFileDir(
                getExternalFilesDir("video").getPath(),
                getExternalFilesDir("audio").getPath(),
                getExternalFilesDir("image").getPath());

        ZXingLibrary.initDisplayOpinion(this);

        console = findViewById(R.id.console);
        console.setOnLongClickListener(new View.OnLongClickListener() {
                                           @Override
                                           public boolean onLongClick(View v) {
                                               clear();
                                               return false;
                                           }
                                       }
        );

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
                    output("send return: " + send);
                }
            }
        });

        int[] channels = new int[8];
        channels[0] = 1024 * 100;
        channels[1] = 1024 * 200;
        channels[2] = 1024 * 200;
        channels[3] = 1024 * 200;

        output("固件版本：" + BuildConfig.VERSION_NAME);

        output("init sdk：" + BuildConfig.PID + "/" + BuildConfig.UUID + "/" + BuildConfig.AUTHOR_KEY);

        IQPManager.getInstance().init(this, "/sdcard/", BuildConfig.PID
                , BuildConfig.UUID, BuildConfig.AUTHOR_KEY, "1.0.0", new IoTSDKManager.IoTCallback() {
                    @Override
                    public void onDpEvent(DPEvent event) {
                        if (event != null) {
                            output("收到 dp: " + event);
                            if (event.type == DPEvent.Type.PROP_RAW) {
                                output(Base64.encodeToString((byte[]) event.value, Base64.DEFAULT));
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
                        output("shorturl: " + urlJson);

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
                        output("onActive: " + IQPManager.getInstance().getIoT().getDeviceId());
                    }

                    @Override
                    public void onFirstActive() {
                        output("onFirstActive");
                    }

                    @Override
                    public void onMQTTStatusChanged(int status) {
                        output("Status: " + status);
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
                        output("onConnectSuccess handle: " + handle);
                    }

                    @Override
                    public void onConnectFail(int err) {
                        output("onConnectFail err: " + err);
                    }

                    @Override
                    public void onDisconnect(int handle, int channel) {
                        output("onDisconnect handle: " + handle + ",channel：" + channel);
                    }

                    @Override
                    public void onRecvData(int handle, int channel, byte[] data) {
                        if (connectHandle == handle) {
                            output("onRecvData handle: " + handle + ",channel：" + channel + ",data：" + data.length);
                            HandlerManager.getInstance().handlerData(channel, data);
                        }
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
