package com.tuya.smart.tuyasmart_ai_iqp;

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

    //标记是否处理了第一个报文状态
    private boolean isHandleFirst;

    //记录当前读取长度
    private int currentReadLength;

    //当前数据
    private byte[] mData;

    //图片路径
    private String videoFileDir;

    //音频路径
    private String audioFileDir;

    //视频路径
    private String imageFileDir;

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
            //此时是第一个报文
            byte[] header = new byte[8];
            //copy头部报文数组
            System.arraycopy(data, 0, header, 0, 8);
            //转为总长度
            long fileLength = Utils.bytes2Long(header);
            //全局声明数据长度
            mData = new byte[(int) fileLength];
            //把此次数据剩下的数据写进
            System.arraycopy(data, 8, mData, 0, data.length - 8);
            //记录已读长度
            currentReadLength = data.length - 8;
            //修改状态
            isHandleFirst = true;
        } else {
            if (currentReadLength + data.length < mData.length) {
                //直接写入即可
                System.arraycopy(data, 0, mData, currentReadLength, data.length);
                //累加已读长度
                currentReadLength += data.length;
            } else {
                //只需要读取尾部长度即可
                System.arraycopy(data, 0, mData, currentReadLength, mData.length - currentReadLength);
                //此时读取完毕
                mergeFile(channel);
                //修改状态
                isHandleFirst = false;
            }
        }
    }


    private void mergeFile(int channel) {
        switch (channel) {
            case 1:
                Utils.getFile(mData, videoFileDir, "VIDEO_" + System.currentTimeMillis() + ".mp4");
                break;
            case 2:
//                Utils.getFile(mData, audioFileDir, "AUDIO_" + System.currentTimeMillis() + "." + fileSuffix);
                Utils.getFile(mData, audioFileDir, "AUDIO_" + System.currentTimeMillis() + ".mp3");
                break;
            case 3:
                Utils.getFile(mData, imageFileDir, "IMAGE_" + System.currentTimeMillis() + ".jpg");
                break;
        }
    }


}
