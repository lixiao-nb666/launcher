package com.innocn.launcher.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.android.launcher3.SettingsActivity;
import com.innocn.launcher.util.ActivityUtil;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/10 0010 16:17
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUtil.toActivity(this, Launcher1Activity.class);
        finish();
    }
}
