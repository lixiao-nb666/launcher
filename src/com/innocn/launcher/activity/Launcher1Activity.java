package com.innocn.launcher.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.android.launcher3.Launcher;
import java.util.List;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/9 0009 14:20
 */
public class Launcher1Activity extends Launcher {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pckName=getTopActivity(this);
        Log.i("lixiao","chakandangqianbaoming:"+pckName);
    }

    public static String getTopActivity(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            //获取正在运行的task列表，其中1表示最近运行的task，通过该数字限制列表中task数目，最近运行的靠前
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
            if (runningTaskInfos != null && runningTaskInfos.size() != 0) {
                return (runningTaskInfos.get(0).baseActivity).getPackageName();
            }
        } catch (Exception e) {
            Log.d("GsonUtils", "Exception=" + e.toString());
        }
        return "";
    }
}
