package com.example.android.uamp.ui;

import android.os.Bundle;

import com.example.android.uamp.R;

public class OfflineActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);
        initializeToolbar();
    }
}
