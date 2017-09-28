package com.example.android.uamp;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import java.security.KeyStoreException;

/**
 * Created on 9/28/17.
 */
public class MyApp extends Application {

    //private Customer customer;

    private static MyApp instance;

    public void onCreate() {
        super.onCreate();

        instance = this;


    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }

    public static MyApp getInstance() {
        return instance;
    }

}
