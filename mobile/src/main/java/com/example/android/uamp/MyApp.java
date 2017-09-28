package com.example.android.uamp;

import android.app.Application;
import android.content.Context;

/**
 * Created on 9/28/17.
 */
public class MyApp extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }
    public static Context getContext() {
        return mContext;
    }

}
