package com.alanluo.recorder;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mTextMessage;
    private ImageView imageView;
    private Button play,stop,recorder,save;

    private Context mContext;

    private AudioRecordManager recordManager;

    private int recTime = 0;
    private int recSessionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        recordManager = AudioRecordManager.newInstance(mContext,mHandler);

        findView();

    }

    private void findView() {
        mTextMessage = (TextView) findViewById(R.id.message);

        play = (Button) findViewById(R.id.play);
        stop = (Button) findViewById(R.id.stop);
        save = (Button) findViewById(R.id.save);
        recorder = (Button) findViewById(R.id.recorder);
        imageView = (ImageView) findViewById(R.id.imageView);

        play.setOnClickListener(this);
        stop.setOnClickListener(this);
        save.setOnClickListener(this);
        recorder.setOnClickListener(this);

        save.setEnabled(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View v) {

        switch (v.getId()){

            /**
             * 音频播放
             */
            case R.id.play:

                recordManager.play();

                break;

            /**
             * 停止
             */
            case R.id.stop:
                recordManager.pause();

                imageView.setVisibility(View.GONE);
                mHandler.sendEmptyMessage(3);
                recorder.setText(R.string.title_recorder);
                recSessionId = -1;

                play.setEnabled(true);
                save.setEnabled(true);
                break;

            /**
             * 录音
             */
            case R.id.recorder:


                if (((Button)v).getText().equals(getString(R.string.title_recorder))){

                    recSessionId = recordManager.record(recSessionId);

                    ((Button)v).setText(R.string.title_pause);

                    imageView.setImageResource(R.drawable.voice_record);
                    mHandler.sendEmptyMessage(1);
                }else{

                    recordManager.pause();

                    ((Button)v).setText(R.string.title_recorder);
                    imageView.setImageResource(R.drawable.voice_pause);
                    mHandler.sendEmptyMessage(2);
                }

                play.setEnabled(true);
                save.setEnabled(true);
                imageView.setVisibility(View.VISIBLE);

                break;

            /**
             * 保存数据
             */
            case R.id.save:

                recordManager.save(null);

                break;
        }

    }


    private Bundle bundle = null;
    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    recTime++;
                    mTextMessage.setText(parserTime(recTime));
                    mHandler.sendEmptyMessageDelayed(1,1000);
                    break;
                case 2:
                    mHandler.removeMessages(1);
                    break;
                case 3:
                    recTime = 0;
                    mHandler.removeMessages(1);
                    break;
                case 4:
                    bundle = msg.getData();
                    mTextMessage.setText("保存中...."+bundle.getInt("rate")+"%");

                    break;
                default:
                    bundle = msg.getData();
                    mTextMessage.setText(bundle.getString("info"));

                    break;
            }

        }
    };

    /**
     * 格式化时间
     * @param time
     * @return
     */
    private CharSequence parserTime(int time) {

        int h = 0,m = 0,s = 0;

        if (time >= 3600) {
            int a = time % 3600;
            h = time / 3600;
            m = a / 60;
            s = a % 60;
        }else if (time >= 60) {
            s = time % 60;
            m = time / 60;
        }else {
            s = time;
        }

        return String.format("%02d:%02d:%02d", h,m,s);
    }



}
