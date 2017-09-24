package cn.fudannhpcc.www.alarm.commonclass;

import java.io.IOException;

import cn.fudannhpcc.www.alarm.BuildConfig;

public class Log {
    public static void d(String tag, String text){
        if(BuildConfig.DEBUG){
            android.util.Log.d(tag, text);
        }
    }
    public static void w(String tag, String text, IOException e){
        if(BuildConfig.DEBUG){
            android.util.Log.w(tag, text, e);
        }
    }
}
