package com.tuya.smart.tuyasmart_ai_iqp;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * author : wanlinruo
 * date : 2021/1/28 10:08
 * contact : ln.wan@tuya.com
 * description :
 */
public class HandlerManager {

    private volatile static HandlerManager instance = null;

    // 私有化构造方法
    private HandlerManager() {

    }

    public static HandlerManager getInstance() {
        if (instance == null) {
            synchronized (HandlerManager.class) {
                if (instance == null) {
                    instance = new HandlerManager();
                }
            }

        }
        return instance;
    }

    private static final String TAG = "HandlerManager";

    //标记是否处理了第一个报文状态
    private boolean isHandleFirst;

    //记录当前读取长度
    private int currentReadLength;

    //文件总长度
    private long fileID;

    //文件总长度
    private long fileLength;

    //文件后缀
    private String fileSuffix;

    //视频路径
    private String videoFileDir;

    //音频路径
    private String audioFileDir;

    //图片路径
    private String imageFileDir;

    //最后文件输出路径
    private String finalFileDir;

    //最后文件输出名称
    private String finalFileName;

    /**
     * 设置目录路径
     *
     * @param videoFileDir
     * @param audioFileDir
     * @param imageFileDir
     */
    public void setFileDir(String videoFileDir, String audioFileDir, String imageFileDir) {
        this.videoFileDir = videoFileDir;
        this.audioFileDir = audioFileDir;
        this.imageFileDir = imageFileDir;
    }

    /**
     * 处理数据
     *
     * @param channel
     * @param data
     */
    public void handlerData(int channel, byte[] data) {
        if (!isHandleFirst) {
            Log.v(TAG, "新文件开始");
            //此时是第一个报文
            //拿头部报文第一个字段：id
            byte[] headerID = new byte[8];
            //copy头部报文数组
            System.arraycopy(data, 0, headerID, 0, 8);
            //提取文件长度
            fileID = Utils.bytes2Long(headerID);
            Log.v(TAG, "新文件开始,fileID:" + fileID);

            //拿头部报文第一个字段：length
            byte[] headerLength = new byte[8];
            //copy头部报文数组
            System.arraycopy(data, 8, headerLength, 0, 8);
            //提取文件长度
            fileLength = Utils.bytes2Long(headerLength);
            Log.v(TAG, "新文件开始,fileLength:" + fileLength);

            //拿头部报文第三个字段：Suffix
            byte[] headerSuffix = new byte[10];
            //copy头部报文数组
            System.arraycopy(data, 16, headerSuffix, 0, 10);
            //提取文件后缀
            fileSuffix = Utils.ByteToString(headerSuffix);
            Log.v(TAG, "新文件开始,fileSuffix:" + fileSuffix);

            //偏移量
            int off = headerID.length + headerLength.length + headerSuffix.length;

            //提取余下的数据数据
            byte[] innerData = new byte[data.length - off];
            System.arraycopy(data, off, innerData, 0, data.length - off);

            //写入文件
            writeToFile(false, channel, innerData, 0, innerData.length);

            //记录已读长度
            currentReadLength = innerData.length;
            Log.v(TAG, "头部数据文件-currentReadLength:" + currentReadLength);

            //修改状态
            isHandleFirst = true;
        } else {
            //直接写入即可
            writeToFile(true, channel, data, 0, data.length);
            //累加已读长度
            currentReadLength += data.length;
            Log.v(TAG, "数据文件currentReadLength:" + currentReadLength);
            if (currentReadLength >= fileLength) {
                //此时已到尾部
                //修改状态
                isHandleFirst = false;
                Log.v(TAG, "已到底部");
            }
        }
    }

    /**
     * 写入文件
     *
     * @param append
     * @param channel
     * @param innerData
     * @param off
     * @param len
     */
    private void writeToFile(boolean append, int channel, byte[] innerData, int off, int len) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            //首次
            if (!append)
                createFileDirAndName(channel);

            File dir = new File(finalFileDir);
            if (!dir.exists()) {//判断文件目录是否存在
                dir.mkdirs();
            }

            file = new File(finalFileDir, finalFileName);

            if (!file.exists()) {
                file.createNewFile();
            }

            Log.v(TAG, "writeToFile-file:" + file);

            fos = new FileOutputStream(file, append);
            bos = new BufferedOutputStream(fos);
            bos.write(innerData, off, len);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.flush();
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 根据Channel创建存储路径和文件名称
     *
     * @param channel
     */
    private void createFileDirAndName(int channel) {
        switch (channel) {
            case 1:
                finalFileDir = videoFileDir;
                finalFileName = "VIDEO_" + System.currentTimeMillis() + "." + fileSuffix;
                break;
            case 2:
                finalFileDir = audioFileDir;
                finalFileName = "AUDIO_" + System.currentTimeMillis() + "." + fileSuffix;
                break;
            case 3:
                finalFileDir = imageFileDir;
                finalFileName = "IMAGE_" + System.currentTimeMillis() + "." + fileSuffix;
                break;
        }
        Log.v(TAG, "createFileDirAndName-finalFileDir:" + finalFileDir + ",finalFileName:" + finalFileName);
    }
}
