package com.tos.floatwindowedittext.ui.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.tos.floatwindowedittext.R;
import com.tos.floatwindowedittext.service.WindowService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View pView){
        //启动服务
        WindowService.startService(this);
    }
}
