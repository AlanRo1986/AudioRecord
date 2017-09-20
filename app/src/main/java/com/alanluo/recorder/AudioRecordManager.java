package com.alanluo.recorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by alan.luo on 2017/9/19.
 */

public class AudioRecordManager implements Runnable {

    private static final String TAG = ">>>>>>>>>>";

    private static final int AudioFormatSampleRate = 16000;

    private Context mContext;

    /**
     * 发送消息到前端
     */
    private Handler mHandler;

    /**
     * AudioRecord.getMinBufferSize
     *
     */
    private int bufferSize;

    /**
     * 录音的状态
     */
    private boolean isRecord;

    /**
     * 输出的文件目录
     */
    private String directory = "/shangdaowenlu";

    /**
     * 输出的临时文件名
     */
    private String fileName = "/jasndfjhamsfnaoihjsdfasd.tmp";

    /**
     * 输出的录音文件路径
     */
    private String path;

    /**
     * 多次录音保存的路径对象
     */
    private List<String> paths;

    /**
     * 输出流对象
     */
    private DataOutputStream out;


    /**
     * 录音对象
     */
    private AudioRecord audioRecord;

    /**
     * 录音线程对象
     */
    private Thread mThread = null;

    /**
     * 单一的实例
     */
    private static AudioRecordManager $_static = null;

    public AudioRecordManager(Context ctx, Handler handler){

        this.mContext = ctx;
        this.mHandler = handler;

        this.bufferSize = AudioRecord.getMinBufferSize(AudioFormatSampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);

        /**
         * init audio record.
         */
        this.audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AudioFormatSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );


        /**
         * 子目录不存在则创建
         */
        directory = Environment.getExternalStorageDirectory().getPath() + directory;
        File file = new File(directory);
        if (!file.exists()){
            file.mkdir();
        }

    }

    /**
     * new Instance
     * @param ctx
     * @param handler
     * @return
     */
    public static AudioRecordManager newInstance(Context ctx, Handler handler){
        if ($_static == null){
            synchronized (AudioRecordManager.class){
                $_static = new AudioRecordManager(ctx,handler);
            }
        }
        return $_static;
    }


    /**
     * 录音record....
     * @param sessionId
     * @return 成功返回录音的sessionId
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public int record(int sessionId){

        if (mThread != null){
            Toast.makeText(mContext,"录音中...",Toast.LENGTH_LONG).show();
        }else {
            try {

                //初始化输出对象
                initOut(sessionId);

                //启动线程
                startThread();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.audioRecord.getAudioSessionId();
    }


    /**
     * 录音停止
     */
    public void stop(){

        if (out != null){
            Log.i(TAG,"停止录音");

            try {
                //关闭线程
                destroyThread();

                //关闭输出流
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 暂停方法
     * 暂停是把录音的路径add到一个list对象中进行保存。
     */
    public void pause(){
        Log.i(TAG,"暂停录音");

        if (paths.contains(path) == false){
            paths.add(path);
        }
        stop();
    }

    /**
     * 试听功能，试听就是把之前加到list对象路径的文件读进来然后播放
     */
    public void play(){

        if (paths.size() == 0){
            Toast.makeText(mContext,"请先录音",Toast.LENGTH_LONG);
            return;
        }

        Log.i(TAG,"开始播放");

        int offset = 0;
        InputStream in;
        AudioTrack track = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                AudioFormatSampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
        );



        try {
            for (String p : paths){
                Log.i(TAG,p);

                File file = new File(p);
                int sizeInBytes = Integer.valueOf(String.valueOf(file.length())) ;
                byte[] audioData = new byte[sizeInBytes];

                in = new FileInputStream(file);
                in.read(audioData);

                track.play();
                while (offset < audioData.length) {
                    offset += track.write(audioData,offset,sizeInBytes);
                }

                offset = 0;
                if (in != null){
                    in.close();
                }
            }

            track.stop();
            track.release();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 转码后播放MP3 *简单的例子
     * @param path
     */
    public void playMp3(String path){

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 录音完毕后合并文件并返回文件路径
     * @param fileName 保存的文件名
     */
    public void save(String fileName){

        if (paths == null || paths.size() == 0){
            Toast.makeText(mContext,"没有录音文件",Toast.LENGTH_LONG).show();
            return;
        }


        final String path = mergeFiles(fileName);

        if (path != null){

            /**
             * 实例化转换器
             */
            final AudioConvertManager convertManager = new AudioConvertManager(AudioFormatSampleRate);


            /**
             * 设置回调
             */
            convertManager.setCallBack(new ConvertCallBack() {
                @Override
                public void process(long size, int len, String path) {

                    int rate = (int) ((Integer.valueOf(len).floatValue() / Long.valueOf(size).floatValue()) * 100);

                    Log.i(TAG, "size: " + size+" len:"+len + " rate:" + rate);


//                    Message msg = mHandler.obtainMessage();
//                    msg.what = 4;
//                    Bundle bundle = new Bundle();
//                    bundle.putString("path",path);
//                    bundle.putInt("rate",rate);
//                    mHandler.sendMessage(msg);

                    if (rate == 100){
                        Log.i(TAG, "保存成功");
                    }

                }

            });


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {



                        convertManager.toMp3(path,null,true);

                        //路径初始化，并声称最新的路径
                        paths = new ArrayList<>();

                    } catch (Exception e) {
                        e.printStackTrace();

                        Message msg = mHandler.obtainMessage();
                        msg.what = 0;
                        Bundle bundle = new Bundle();
                        bundle.putString("info",e.getMessage());

                        mHandler.sendMessage(msg);
                    }
                }
            }).start();

        }

    }

    /**
     * 录音的线程
     */
    @Override
    public void run() {

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.i(TAG,"audioRecord未初始化...");
            stop();
            return;
        }

        int bytesRecord;
        byte[] audioData = new byte[bufferSize / 4];

        audioRecord.startRecording();

        Log.i(TAG,"开始录音...");

        while (isRecord){
            bytesRecord = audioRecord.read(audioData,0,audioData.length);

            if (bytesRecord == AudioRecord.ERROR_INVALID_OPERATION || bytesRecord == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }

            if (bytesRecord != 0 && bytesRecord != -1) {
                //在此可以对录制音频的数据进行二次处理 比如变声，压缩，降噪，增益等操作
                //我们这里直接将pcm音频原数据写入文件 这里可以直接发送至服务器 对方采用AudioTrack进行播放原数据
                writeData(audioData, bytesRecord);
            } else {
                break;
            }
        }

        stop();

    }

    /**
     * 初始化输出对象
     * @param sessionId
     * @throws Exception
     */
    private void initOut(int sessionId) throws Exception {

        //第一次的录音
        if (sessionId == -1){
            Log.i(TAG,"第一次录音");
            paths = new ArrayList<>();
        }else{
            Log.i(TAG,"继续录音");
        }

        //保存路径
        path = directory + fileName + paths.size();

        Log.i(TAG,path + ":sessionId->"+sessionId);

        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();

        //out对象
        out = new DataOutputStream(new FileOutputStream(file, true));
    }


    /**
     * 录音数据写出到内存卡
     * @param audioData
     * @param len
     */
    protected void writeData(byte[] audioData,int len){
        try {
            //写出文件
            out.write(audioData,0,len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 合并音频文件，成功返回文件路径地址，失败返回null
     * @param targetPath
     * @return
     */
    protected String mergeFiles(String targetPath){

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            if (targetPath == null){
                targetPath = System.currentTimeMillis() + "_merge.pcm";
            }

            targetPath = directory + "/" + targetPath;

            Log.i(TAG,targetPath);

            //新建一个目标文件对象
            File target = new File(targetPath);
            if (target.exists()){
                target.delete();
            }
            target.createNewFile();

            //实例化一个文件流输出对象
            out = new FileOutputStream(target);

            //循环读取要合并的文件集合
            for(String path:paths) {

                //文件对象
                File file = new File(path);

                if (file.exists()) {

                    Log.i(TAG,"filePath:"+file.getAbsolutePath()+" file:"+file.length());

                    byte[] buf = new byte[1024];
                    int len = -1;
                    in = new FileInputStream(file);
                    while ((len = in.read(buf)) != -1) {
                        //写出数据
                        out.write(buf, 0, len);
                    }

                    if (in != null) {
                        in.close();

                        //合并后把源文件删除
                        file.delete();
                    }
                }
            }

            paths = new ArrayList<>();
            paths.add(targetPath);

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            targetPath = null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            targetPath = null;
        }finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        return targetPath;

    }


    /**
     * 开始线程
     */
    protected void startThread(){
        if (mThread == null){
            Log.i(TAG,"启动录音线程");

            //录音的状态
            isRecord = true;

            //启动线程
            mThread = new Thread(this);
            mThread.start();
        }
    }

    /**
     * 销毁线程
     */
    protected synchronized void destroyThread(){

        if (mThread != null){

            Log.i(TAG,"销毁录音线程");

            //先改变状态，让录音跳出循环
            isRecord = false;

            try {
                //延迟线程关闭
                Thread.sleep(500);
                mThread.interrupt();

            } catch (InterruptedException e) {
                e.printStackTrace();

            }finally {
                mThread = null;
            }

        }
    }

}
