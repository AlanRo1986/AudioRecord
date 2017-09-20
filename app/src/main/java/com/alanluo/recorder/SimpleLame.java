package com.alanluo.recorder;

/**
 * Created by clam314 on 2017/3/26
 */

public class SimpleLame {

    public native static void close();

    public native static int encode(short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf);

    public native static int flush(byte[] mp3buf);

    public native static void init(int inSampleRate, int outChannel, int outSampleRate, int outBitrate, int quality);

    /**
     * 初始化转码器
     * @param inSampleRate 录音的采样率
     * @param outChannel 声道
     * @param outSampleRate 输出的采样率
     * @param outBitrate 输出MP3的码率
     *
     */
    public static void init(int inSampleRate, int outChannel, int outSampleRate, int outBitrate) {
        init(inSampleRate, outChannel, outSampleRate, outBitrate, 7);
    }
}
