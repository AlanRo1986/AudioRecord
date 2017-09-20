package com.alanluo.recorder;

import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Created by alan.luo on 2017/9/20.
 */

public class AudioConvertManager {

    private static final String TAG = "+++++>>>>>>>>>>";

    /**
     * 文件输出的路径目录
     */
    private String directory = "/shangdaowenlu";

    /**
     * 需要转码的数据
     */
    private byte[] mp3Buffer;

    /**
     * 转码回调接口
     */
    private ConvertCallBack callBack;

    /**
     * 录音的采样率
     */
    private int samplingRate;

    //输出MP3的码率
    private static final int BIT_RATE = 128;

    /**
     * 加载lib库
     */
    static {
        System.loadLibrary("lamemp3");
    }

    public AudioConvertManager(int rate) {

        /**
         * 子目录不存在则创建
         */
        directory = Environment.getExternalStorageDirectory().getPath() + directory;
        File file = new File(directory);
        if (!file.exists()) {
            file.mkdir();
        }

        /**
         * 采样率
         */
        this.samplingRate = rate;

        /**
         * 初始化转码器
         */
        SimpleLame.init(samplingRate, 1, samplingRate, BIT_RATE);

    }

    /**
     * 把pcm音频转码成mp3音频
     *
     * @param targetPath      源文件路径
     * @param savePath        mp3输出的文件路径
     * @param isDeleteOldFile 是否删除旧文件
     * @return
     * @throws Exception
     */
    public String toMp3(String targetPath, String savePath, boolean isDeleteOldFile) throws Exception {

        if (targetPath == null || targetPath.trim().equals("")) {
            throw new Exception("The param targetPath must be require.");
        }

        if (savePath == null) {
            savePath = System.currentTimeMillis() + ".mp3";
        }
        savePath = directory + "/" + savePath;
        Log.i(TAG, savePath);


        File file = new File(targetPath);
        if (!file.exists()) {
            throw new FileNotFoundException("The file " + targetPath + " is invalid.");
        }

        FileInputStream in = new FileInputStream(targetPath);
        FileOutputStream out = new FileOutputStream(savePath);

        int len = -1;
        int lens = 0;
        byte[] buf = new byte[1024];
        while ((len = in.read(buf)) != -1) {

            /**
             * 转码后的容器
             */
            mp3Buffer = new byte[len];

            short[] shorts = BytesToShorts(buf);

            int encodedSize = SimpleLame.encode(shorts, shorts, shorts.length, mp3Buffer);

            lens += len;

            if (encodedSize >= 0) {
                out.write(mp3Buffer, 0, encodedSize);
            }

            if (this.callBack != null) {
                this.callBack.process(file.length(), lens, savePath);
            }


        }

        /**
         * 释放资源
         */
        int flushResult = SimpleLame.flush(mp3Buffer);
        Log.i(TAG, "flushResult:" + flushResult);

        if (isDeleteOldFile) {
            file.delete();
        }

        return savePath;
    }

    /**
     * 设置回调
     *
     * @param callBack
     */
    public void setCallBack(ConvertCallBack callBack) {
        this.callBack = callBack;
    }

    /**
     * bytes to shorts
     *
     * @param buf
     * @return
     */
    public short[] BytesToShorts(byte[] buf) {
        byte bLength = 2;
        short[] s = new short[buf.length / bLength];
        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];
            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[iLoop * bLength + jLoop];
            }
            s[iLoop] = getShort(temp);
        }
        return s;
    }

    /**
     * @param buf
     * @param bBigEnding
     * @return
     */
    public short getShort(byte[] buf, boolean bBigEnding) {

        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 2) {
            throw new IllegalArgumentException("byte array size > 2 !");
        }

        short r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        }

        return r;
    }

    /**
     *
     * @param buf
     * @return
     */
    public short getShort(byte[] buf) {
        return getShort(buf, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
    }


}
