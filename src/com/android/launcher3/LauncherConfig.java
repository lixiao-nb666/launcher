package com.android.launcher3;

import com.android.launcher3.util.Thunk;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/12 0012 11:33
 */
public class LauncherConfig {
   public static final boolean LOGD = false;

    public static final boolean DEBUG_STRICT_MODE = false;

    public static final int REQUEST_CREATE_SHORTCUT = 1;
    public static final int REQUEST_CREATE_APPWIDGET = 5;

    public static final int REQUEST_PICK_APPWIDGET = 9;

    public static final int REQUEST_BIND_APPWIDGET = 11;
    public static final int REQUEST_BIND_PENDING_APPWIDGET = 12;
    public static final int REQUEST_RECONFIGURE_APPWIDGET = 13;

    public static final int REQUEST_PERMISSION_CALL_PHONE = 14;

    public static final float BOUNCE_ANIMATION_TENSION = 1.3f;

    /**
     * IntentStarter uses request codes starting with this. This must be greater than all activity
     * request codes used internally.
     */
    public static final int REQUEST_LAST = 100;

    // Type: int
    public static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    public static final String RUNTIME_STATE = "launcher.state";
    // Type: PendingRequestArgs
    public static final String RUNTIME_STATE_PENDING_REQUEST_ARGS = "launcher.request_args";
    // Type: ActivityResultInfo
    public static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";
    // Type: SparseArray<Parcelable>
    public static final String RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel";
    public   static final int NEW_APPS_PAGE_MOVE_DELAY = 500;
    public static final int NEW_APPS_ANIMATION_DELAY = 500;


public static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;

 // How long to wait before the new-shortcut animation automatically pans the workspace

 public static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
}
