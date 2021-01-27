package com.tuya.smart.tuyasmart_ai_iqp;

import java.util.Arrays;

/**
 * author : wanlinruo
 * date : 2021/1/23 16:16
 * contact : ln.wan@tuya.com
 * description :
 */
public class DataBean {

    private long id;
    private long length;
    private byte[] fileSuffix;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public byte[] getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(byte[] fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    @Override
    public String toString() {
        return "DataBean{" +
                "id=" + id +
                ", length=" + length +
                ", fileSuffix=" + Arrays.toString(fileSuffix) +
                '}';
    }
}
