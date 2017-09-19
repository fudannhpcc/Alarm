package cn.fudannhpcc.www.alarm.commonclass;

//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import cn.fudannhpcc.www.alarm.commonclass.Log;

import cn.fudannhpcc.www.alarm.R;
//import android.content.Context;
//import android.content.Intent;
//import android.database.Cursor;
//import android.database.sqlite.SQLiteDatabase;
//import android.graphics.Color;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.os.PowerManager;
//import android.support.annotation.Nullable;
//import android.support.v7.app.AppCompatActivity;
//import android.util.JsonReader;
//
//
//import org.fusesource.hawtbuf.Buffer;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.StringReader;
//import java.io.UnsupportedEncodingException;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Collections;
//import java.util.Date;
//import java.util.Enumeration;
//import java.util.GregorianCalendar;
//import java.util.HashMap;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//import java.util.zip.ZipOutputStream;


public class MQTTService extends Service {

    public class LocalBinder extends Binder {
        String stringToSend = "I'm the test String";
        MQTTService getService() {
            Log.d("TAG", "getService ---> " + MQTTService.this);
            return MQTTService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("TAG", "onBind~~~~~~~~~~~~");
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG", "onCreate~~~~~~~~~~");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "onDestroy~~~~~~~~~~~");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TAG", "onStartCommand~~~~~~~~~~~~");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("TAG", "onUnbind~~~~~~~~~~~~~~~~");
        return super.onUnbind(intent);
    }

//    private static MQTTService instance;
//
//    static public MQTTService getInstance() {
//        return instance;
//    }
//
//    private final String TAG = "MyService";
//
//    private MediaPlayer mediaPlayer;
//
//    private int startId;
//
//    public enum Control {
//        PLAY, PAUSE, STOP
//    }
//
//    public MQTTService() {
//    }
//
//    @Override
//    public void onCreate() {
//        if (mediaPlayer == null) {
//            mediaPlayer = MediaPlayer.create(this, R.raw.music);
//            mediaPlayer.setLooping(false);
//        }
//        Log.d(TAG, "onCreate");
//        super.onCreate();
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        this.startId = startId;
//        Log.d(TAG, "onStartCommand---startId: " + startId);
//        Bundle bundle = intent.getExtras();
//        if (bundle != null) {
//            Control control = (Control) bundle.getSerializable("Key");
//            if (control != null) {
//                switch (control) {
//                    case PLAY:
//                        play();
//                        break;
//                    case PAUSE:
//                        pause();
//                        break;
//                    case STOP:
//                        stop();
//                        break;
//                }
//            }
//        }
//        return super.onStartCommand(intent, flags, startId);
//    }
//
//    @Override
//    public void onDestroy() {
//        Log.d(TAG, "onDestroy");
//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//            mediaPlayer.release();
//        }
//        super.onDestroy();
//    }
//
//    private void play() {
//        if (!mediaPlayer.isPlaying()) {
//            mediaPlayer.start();
//        }
//    }
//
//    private void pause() {
//        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//            mediaPlayer.pause();
//        }
//    }
//
//    private void stop() {
//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//        }
//        stopSelf(startId);
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        Log.d(TAG, "onBind");
//        throw new UnsupportedOperationException("Not yet implemented");
//    }

}

