

package com.android.launcher3;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
import static com.android.launcher3.LauncherAnimUtils.SPRING_LOADED_EXIT_DELAY;
import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.NORMAL;

import static com.android.launcher3.logging.LoggerUtils.newContainerTarget;
import static com.android.launcher3.logging.LoggerUtils.newTarget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompatVO;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.FolderIconPreviewVerifier;
import com.android.launcher3.keyboard.CustomActionsPopup;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.listen.LauncherOverlay;
import com.android.launcher3.listen.LauncherOverlayCallbacks;
import com.android.launcher3.listen.OnResumeCallback;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.logging.UserEventDispatcher.UserEventDelegate;
import com.android.launcher3.manager.view.AllAppManager;
import com.android.launcher3.manager.view.DragManager;
import com.android.launcher3.manager.view.DropBarManager;
import com.android.launcher3.manager.view.HotseatManager;
import com.android.launcher3.manager.view.WorkSpaceManager;
import com.android.launcher3.model.ModelWriter;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.popup.PopupDataProvider;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.states.InternalStateHandler;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import com.android.launcher3.userevent.nano.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;
import com.android.launcher3.util.ActivityResultInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.util.MultiValueAlpha.AlphaProperty;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.PendingRequestArgs;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.util.TraceHelper;
import com.android.launcher3.util.UiThreadHelper;
import com.android.launcher3.util.ViewOnDrawExecutor;
import com.android.launcher3.views.OptionsPopupView;
import com.android.launcher3.views.hotseat.Hotseat;
import com.android.launcher3.views.hotseat.HotseatListen;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PendingAppWidgetHostView;
import com.android.launcher3.widget.WidgetAddFlowHandler;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.WidgetListRowEntry;
import com.android.launcher3.widget.WidgetsFullSheet;
import com.android.launcher3.widget.custom.CustomWidgetParser;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default launcher application.
 */
public class Launcher extends BaseDraggingActivity implements LauncherExterns,
        LauncherModel.Callbacks, LauncherProviderChangeListener, UserEventDelegate{
    public static final String TAG = "Launcher";
    private LauncherStateManager mStateManager;
    private LauncherAppTransitionManager mAppTransitionManager;
    private Configuration mOldConfig;
    public AllAppManager allAppManager;
    public HotseatManager hotseatManager;
    public HotseatListen hotseatListen=new HotseatListen() {
        @Override
        public void nowNeedShowAllAppView() {
            mStateManager.goToState(ALL_APPS,true);
        }
    };

    private WorkSpaceManager workSpaceManager;
    private DragManager dragManager;
    private DropBarManager dropBarManager;

    private View mLauncherView;

    private AppWidgetManagerCompat mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;


    private final int[] mTmpAddItemCellCoordinates = new int[2];





    // UI and state for the overview panel
    private View mOverviewPanel;


    private OnResumeCallback mOnResumeCallback;

    private ViewOnDrawExecutor mPendingExecutor;

    private LauncherModel mModel;
    private ModelWriter mModelWriter;
    private IconCache mIconCache;
    private LauncherAccessibilityDelegate mAccessibilityDelegate;

    private PopupDataProvider mPopupDataProvider;

    private int mSynchronouslyBoundPage = PagedView.INVALID_PAGE;

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Activity result which needs to be processed after workspace has loaded.
    private ActivityResultInfo mPendingActivityResult;
    /**
     * Holds extra information required to handle a result from an external call, like
     * {@link #startActivityForResult(Intent, int)} or {@link #requestPermissions(String[], int)}
     */
    private PendingRequestArgs mPendingRequestArgs;

    public ViewGroupFocusHelper mFocusHandler;

    private RotationHelper mRotationHelper;

    private final Handler mHandler = new Handler();
    private final Runnable mLogOnDelayedResume = this::logOnDelayedResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (LauncherConfig.DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        TraceHelper.beginSection("Launcher-onCreate");
        super.onCreate(savedInstanceState);
        TraceHelper.partitionSection("Launcher-onCreate", "super call");
        LauncherAppState app = LauncherAppState.getInstance(this);
        mOldConfig = new Configuration(getResources().getConfiguration());
        mModel = app.setLauncher(this,this);
        initDeviceProfile(app.getInvariantDeviceProfile());
        mSharedPrefs = Utilities.getPrefs(this);
        mIconCache = app.getIconCache();
        mAccessibilityDelegate = new LauncherAccessibilityDelegate(this);
        dragManager=new DragManager(this);
        allAppManager=new AllAppManager(this);
        mStateManager = new LauncherStateManager(this);

        UiFactory.onCreate(this);
        mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);
        mAppWidgetHost = new LauncherAppWidgetHost(this);
        mAppWidgetHost.startListening();
        mLauncherView = LayoutInflater.from(this).inflate(R.layout.launcher, null);
        findViewById(R.id.tv_set_hotseat_show_type).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("lixiao","fskjflksnowWoyaojiafksjdflk1111");
//                workSpaceManager.addDemo();
//                mStateManager.goToState(ALL_APPS);
//                hotseatManager.getHotseat().getLayout()
            }
        });

        setupViews();
        mPopupDataProvider = new PopupDataProvider(this);

        mRotationHelper = new RotationHelper(this);
        mAppTransitionManager = LauncherAppTransitionManager.newInstance(this);

        boolean internalStateHandled = InternalStateHandler.handleCreate(this, getIntent());
        if (internalStateHandled) {
            if (savedInstanceState != null) {
                // InternalStateHandler has already set the appropriate state.
                // We dont need to do anything.
                savedInstanceState.remove(LauncherConfig.RUNTIME_STATE);
            }
        }
        restoreState(savedInstanceState);
        // We only load the page synchronously if the user rotates (or triggers a
        // configuration change) while launcher is in the foreground
        int currentScreen = PagedView.INVALID_RESTORE_PAGE;
        if (savedInstanceState != null) {
            currentScreen = savedInstanceState.getInt(LauncherConfig.RUNTIME_STATE_CURRENT_SCREEN, currentScreen);
        }
        if (!mModel.startLoader(currentScreen)) {
            if (!internalStateHandled) {
                // If we are not binding synchronously, show a fade in animation when
                // the first page bind completes.
                dragManager.initSet();
            }
        } else {
            // Pages bound synchronously.
            workSpaceManager.setPageNumb(currentScreen);

        }

        // For handling default keys
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        setContentView(mLauncherView);
        getRootView().dispatchInsets();

        // Listen for broadcasts
        registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
                Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onCreate(savedInstanceState);
        }
        mRotationHelper.initialize();
        TraceHelper.endSection("Launcher-onCreate");
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        UiFactory.onEnterAnimationComplete(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int diff = newConfig.diff(mOldConfig);
        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
            mUserEventDispatcher = null;
            initDeviceProfile(mDeviceProfile.inv);
            dispatchDeviceProfileChanged();
            reapplyUi();
            dragManager.getmDragLayer().recreateControllers();

            // TODO: We can probably avoid rebind when only screen size changed.
            rebindModel();
        }

        mOldConfig.setTo(newConfig);
        UiFactory.onLauncherStateOrResumeChanged(this);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void reapplyUi() {
        getRootView().dispatchInsets();
        getStateManager().reapplyState(true /* cancelCurrentAnimation */);
    }

    @Override
    public void rebindModel() {
        int currentPage =workSpaceManager.getNextPage();
        if (mModel.startLoader(currentPage)) {
            workSpaceManager.setPageNumb(currentPage);

        }
    }

    private void initDeviceProfile(InvariantDeviceProfile idp) {
        // Load configuration-specific DeviceProfile
        mDeviceProfile = idp.getDeviceProfile(this);
        if (isInMultiWindowModeCompat()) {
            Display display = getWindowManager().getDefaultDisplay();
            Point mwSize = new Point();
            display.getSize(mwSize);
            mDeviceProfile = mDeviceProfile.getMultiWindowProfile(this, mwSize);
        }
        onDeviceProfileInitiated();
        mModelWriter = mModel.getWriter(mDeviceProfile.isVerticalBarLayout(), true);
    }

    public RotationHelper getRotationHelper() {
        return mRotationHelper;
    }

    public LauncherStateManager getStateManager() {
        return mStateManager;
    }

    @Override
    public <T extends View> T findViewById(int id) {
        return mLauncherView.findViewById(id);
    }

    @Override
    public void onAppWidgetHostReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    private LauncherCallbacks mLauncherCallbacks;

    /**
     * Call this after onCreate to set or clear overlay.
     */
    @Override
    public void setLauncherOverlay(LauncherOverlay overlay) {
        if (overlay != null) {
            overlay.setOverlayCallbacks(new LauncherOverlayCallbacksImpl());
        }
        workSpaceManager.setLauncherOverlay(overlay);
    }

    @Override
    public boolean setLauncherCallbacks(LauncherCallbacks callbacks) {
        mLauncherCallbacks = callbacks;
        return true;
    }

    @Override
    public void onLauncherProviderChanged() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onLauncherProviderChange();
        }
    }

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo info) {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return (int) info.id;
    }

    public PopupDataProvider getPopupDataProvider() {
        return mPopupDataProvider;
    }

    @Override
    public BadgeInfo getBadgeInfoForItem(ItemInfo info) {
        return mPopupDataProvider.getBadgeInfoForItem(info);
    }

    @Override
    public void invalidateParent(ItemInfo info) {
        FolderIconPreviewVerifier verifier = new FolderIconPreviewVerifier(getDeviceProfile().inv);
        if (verifier.isItemInPreview(info.rank) && (info.container >= 0)) {
            View folderIcon = getWorkspace().getHomescreenIconByItemId(info.container);
            if (folderIcon != null) {
                folderIcon.invalidate();
            }
        }
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private long completeAdd(
            int requestCode, Intent intent, int appWidgetId, PendingRequestArgs info) {
        long screenId = info.screenId;
        if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // When the screen id represents an actual screen (as opposed to a rank) we make sure
            // that the drop page actually exists.
            screenId = workSpaceManager.ensurePendingDropLayoutExists(info.screenId);
        }
        switch (requestCode) {
            case LauncherConfig.REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(intent, info.container, screenId, info.cellX, info.cellY, info);
                break;
            case LauncherConfig.REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(appWidgetId, info, null, null);
                break;
            case LauncherConfig.REQUEST_RECONFIGURE_APPWIDGET:
                completeRestoreAppWidget(appWidgetId, LauncherAppWidgetInfo.RESTORE_COMPLETED);
                break;
            case LauncherConfig.REQUEST_BIND_PENDING_APPWIDGET: {
                int widgetId = appWidgetId;
                LauncherAppWidgetInfo widgetInfo =
                        completeRestoreAppWidget(widgetId, LauncherAppWidgetInfo.FLAG_UI_NOT_READY);
                if (widgetInfo != null) {
                    // Since the view was just bound, also launch the configure activity if needed
                    LauncherAppWidgetProviderInfo provider = mAppWidgetManager
                            .getLauncherAppWidgetInfo(widgetId);
                    if (provider != null) {
                        new WidgetAddFlowHandler(provider)
                                .startConfigActivity(this, widgetInfo, LauncherConfig.REQUEST_RECONFIGURE_APPWIDGET);
                    }
                }
                break;
            }
        }
        return screenId;
    }

    private void handleActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        if (isWorkspaceLoading()) {
            // process the result once the workspace has loaded.
            mPendingActivityResult = new ActivityResultInfo(requestCode, resultCode, data);
            return;
        }
        mPendingActivityResult = null;
        // Reset the startActivity waiting flag
        final PendingRequestArgs requestArgs = mPendingRequestArgs;
        setWaitingForResult(null);
        if (requestArgs == null) {
            return;
        }
        final int pendingAddWidgetId = requestArgs.getWidgetId();
        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                mStateManager.goToState(NORMAL, SPRING_LOADED_EXIT_DELAY);
            }
        };
        if (requestCode == LauncherConfig.REQUEST_BIND_APPWIDGET) {
            // This is called only if the user did not previously have permissions to bind widgets
            final int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId, requestArgs);
                workSpaceManager.removeExtraEmptyScreenDelayed(exitSpringLoaded, LauncherConfig.ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(
                        appWidgetId, requestArgs, null,
                        requestArgs.getWidgetHandler(),
                        LauncherConfig.ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            }
            return;
        }

        boolean isWidgetDrop = (requestCode == LauncherConfig.REQUEST_PICK_APPWIDGET ||
                requestCode == LauncherConfig.REQUEST_CREATE_APPWIDGET);
        // We have special handling for widgets
        if (isWidgetDrop) {
            final int appWidgetId;
            int widgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }
            final int result;
            if (appWidgetId < 0 || resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not " +
                        "returned from the widget configuration activity.");
                result = RESULT_CANCELED;
                completeTwoStageWidgetDrop(result, appWidgetId, requestArgs);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        getStateManager().goToState(NORMAL);
                    }
                };
                workSpaceManager.removeExtraEmptyScreenDelayed(onComplete,LauncherConfig.ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            } else {
                if (requestArgs.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    // When the screen id represents an actual screen (as opposed to a rank)
                    // we make sure that the drop page actually exists.
                    requestArgs.screenId =
                            workSpaceManager.ensurePendingDropLayoutExists(requestArgs.screenId);
                }
                final CellLayout dropLayout =workSpaceManager.getCellLayoutById(requestArgs.screenId);
                dropLayout.setDropPending(true);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        completeTwoStageWidgetDrop(resultCode, appWidgetId, requestArgs);
                        dropLayout.setDropPending(false);
                    }
                };
                workSpaceManager.removeExtraEmptyScreenDelayed(onComplete,LauncherConfig.ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            }
            return;
        }

        if (requestCode == LauncherConfig.REQUEST_RECONFIGURE_APPWIDGET
                || requestCode == LauncherConfig.REQUEST_BIND_PENDING_APPWIDGET) {
            if (resultCode == RESULT_OK) {
                // Update the widget view.
                completeAdd(requestCode, data, pendingAddWidgetId, requestArgs);
            }
            // Leave the widget in the pending state if the user canceled the configure.
            return;
        }

        if (requestCode == LauncherConfig.REQUEST_CREATE_SHORTCUT) {
            // Handle custom shortcuts created using ACTION_CREATE_SHORTCUT.
            if (resultCode == RESULT_OK && requestArgs.container != ItemInfo.NO_ID) {
                completeAdd(requestCode, data, -1, requestArgs);
                workSpaceManager.removeExtraEmptyScreenDelayed(exitSpringLoaded,
                        LauncherConfig.ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            } else if (resultCode == RESULT_CANCELED) {
                workSpaceManager.removeExtraEmptyScreenDelayed(exitSpringLoaded,
                        LauncherConfig.ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            }
        }
        dragManager.getmDragLayer().clearAnimatedView();
    }

    @Override
    public void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        handleActivityResult(requestCode, resultCode, data);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        PendingRequestArgs pendingArgs = mPendingRequestArgs;
        if (requestCode == LauncherConfig.REQUEST_PERMISSION_CALL_PHONE && pendingArgs != null
                && pendingArgs.getRequestCode() == LauncherConfig.REQUEST_PERMISSION_CALL_PHONE) {
            setWaitingForResult(null);

            View v = null;
            CellLayout layout = getCellLayout(pendingArgs.container, pendingArgs.screenId);
            if (layout != null) {
                v = layout.getChildAt(pendingArgs.cellX, pendingArgs.cellY);
            }
            Intent intent = pendingArgs.getPendingIntent();

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivitySafely(v, intent, null);
            } else {
                // TODO: Show a snack bar with link to settings
                Toast.makeText(this, getString(R.string.msg_no_phone_permission,
                        getString(R.string.derived_app_name)), Toast.LENGTH_SHORT).show();
            }
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onRequestPermissionsResult(requestCode, permissions,
                    grantResults);
        }
    }



    @Thunk void completeTwoStageWidgetDrop(
            final int resultCode, final int appWidgetId, final PendingRequestArgs requestArgs) {
        CellLayout cellLayout = workSpaceManager.getCellLayoutById(requestArgs.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    requestArgs.getWidgetHandler().getProviderInfo(this));
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, requestArgs, layout, null);
                    mStateManager.goToState(NORMAL, SPRING_LOADED_EXIT_DELAY);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
        }
        if (getDragLayer().getAnimatedView() != null) {
            workSpaceManager.animateWidgetDrop(requestArgs, cellLayout,
                    (DragView) getDragLayer().getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget);
        } else if (onCompleteRunnable != null) {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStop();
        }
        getUserEventDispatcher().logActionCommand(Action.Command.STOP,
                mStateManager.getState().containerType, -1);

        mAppWidgetHost.setListenIfResumed(false);

        NotificationListener.removeNotificationsChangedListener();
        getStateManager().moveToRestState();

        UiFactory.onLauncherStateOrResumeChanged(this);

        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStart();
        }
        mAppWidgetHost.setListenIfResumed(true);
        NotificationListener.setNotificationsChangedListener(mPopupDataProvider);
        UiFactory.onStart(this);
    }

    private void logOnDelayedResume() {
        if (hasBeenResumed()) {
            getUserEventDispatcher().logActionCommand(Action.Command.RESUME,
                    mStateManager.getState().containerType, -1);
            getUserEventDispatcher().startSession();
        }
    }

    @Override
    protected void onResume() {
        TraceHelper.beginSection("ON_RESUME");
        super.onResume();
        TraceHelper.partitionSection("ON_RESUME", "superCall");
        mHandler.removeCallbacks(mLogOnDelayedResume);
        Utilities.postAsyncCallback(mHandler, mLogOnDelayedResume);
        setOnResumeCallback(null);
        // Process any items that were added while Launcher was away.
        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_ACTIVITY_PAUSED, this);
        // Refresh shortcuts if the permission changed.
        mModel.refreshShortcutsIfRequired();
        DiscoveryBounce.showForHomeIfNeeded(this);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onResume();
        }
        UiFactory.onLauncherStateOrResumeChanged(this);
        TraceHelper.endSection("ON_RESUME");
    }

    @Override
    protected void onPause() {
        // Ensure that items added to Launcher are queued until Launcher returns
        InstallShortcutReceiver.enableInstallQueue(InstallShortcutReceiver.FLAG_ACTIVITY_PAUSED);
        super.onPause();
        dragManager.pause();
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPause();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        UiFactory.onLauncherStateOrResumeChanged(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mStateManager.onWindowFocusChanged();
    }

    class LauncherOverlayCallbacksImpl implements LauncherOverlayCallbacks {

        public void onScrollChanged(float progress) {
            workSpaceManager.onOverlayScrollChanged(progress);
        }
    }

    public boolean hasSettings() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasSettings();
        } else {
            // On O and above we there is always some setting present settings (add icon to
            // home screen or icon badging). On earlier APIs we will have the allow rotation
            // setting, on devices with a locked orientation,
            return Utilities.ATLEAST_OREO || !getResources().getBoolean(R.bool.allow_rotation);
        }
    }

    public boolean isInState(LauncherState state) {
        return mStateManager.getState() == state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        int stateOrdinal = savedState.getInt(LauncherConfig.RUNTIME_STATE, NORMAL.ordinal);
        LauncherState[] stateValues = LauncherState.values();
        LauncherState state = stateValues[stateOrdinal];
        if (!state.disableRestore) {
            mStateManager.goToState(state, false /* animated */);
        }

        PendingRequestArgs requestArgs = savedState.getParcelable(LauncherConfig.RUNTIME_STATE_PENDING_REQUEST_ARGS);
        if (requestArgs != null) {
            setWaitingForResult(requestArgs);
        }

        mPendingActivityResult = savedState.getParcelable(LauncherConfig.RUNTIME_STATE_PENDING_ACTIVITY_RESULT);

        SparseArray<Parcelable> widgetsState =
                savedState.getSparseParcelableArray(LauncherConfig.RUNTIME_STATE_WIDGET_PANEL);
        if (widgetsState != null) {
            WidgetsFullSheet.show(this, false).restoreHierarchyState(widgetsState);
        }
    }

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {

        DragLayer mDragLayer = findViewById(R.id.drag_layer);
        dragManager.setDragLayer(mDragLayer);
        mFocusHandler = mDragLayer.getFocusIndicatorHelper();
        workSpaceManager=new WorkSpaceManager(mDragLayer);

        mOverviewPanel = findViewById(R.id.overview_panel);
        Hotseat mHotseat = findViewById(R.id.hotseat);
        mHotseat.setHotseatListen(hotseatListen);
        View mHotseatSearchBox = findViewById(R.id.search_container_hotseat);
        hotseatManager=new HotseatManager(mHotseat,mHotseatSearchBox);
        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // Setup the drag layer
        dragManager.setUp(workSpaceManager.getWorkspace());
        UiFactory.setOnTouchControllersChangedListener(this, mDragLayer::recreateControllers);

        workSpaceManager.getWorkspace().setup(dragManager.getmDragController());
        // Until the workspace is bound, ensure that we keep the wallpaper offset locked to the
        // default state, otherwise we will update to the wrong offsets in RTL
        workSpaceManager.getWorkspace().lockWallpaperToDefaultPage();
        workSpaceManager.getWorkspace().bindAndInitFirstWorkspaceScreen(null /* recycled qsb */);
        dragManager.addListen(workSpaceManager.getWorkspace());
        // Get the search/delete/uninstall bar
        DropTargetBar mDropTargetBar = mDragLayer.findViewById(R.id.drop_target_bar);
        dropBarManager=new DropBarManager(mDropTargetBar);

        // Setup Apps
        AllAppsContainerView mAppsView = findViewById(R.id.apps_view);
        allAppManager.setDefView(mAppsView);
        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dropBarManager.init(workSpaceManager.getWorkspace(),getDragController());


    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     */
    View createShortcut(ShortcutInfo info) {
        Log.i("lixiao","create------shouyeView:"+info);
        return createShortcut(workSpaceManager.getViewGroup(), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param parent The group the shortcut belongs to.
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_icon, parent, false);
        favorite.applyFromShortcutInfo(info);
        favorite.setOnClickListener(ItemClickHandler.INSTANCE);
        favorite.setOnFocusChangeListener(mFocusHandler);

        Log.i("lixiao","chuangjianshitu:"+info);
        return favorite;
    }

    /**
     * Add a shortcut to the workspace or to a Folder.
     *
     * @param data The intent describing the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, long screenId, int cellX,
            int cellY, PendingRequestArgs args) {
        if (args.getRequestCode() != LauncherConfig.REQUEST_CREATE_SHORTCUT
                || args.getPendingIntent().getComponent() == null) {
            return;
        }

        int[] cellXY = mTmpAddItemCellCoordinates;
        CellLayout layout = getCellLayout(container, screenId);

        ShortcutInfo info = null;
        if (Utilities.ATLEAST_OREO) {
            info = LauncherAppsCompatVO.createShortcutInfoFromPinItemRequest(
                    this, LauncherAppsCompatVO.getPinItemRequest(data), 0);
        }

        if (info == null) {
            // Legacy shortcuts are only supported for primary profile.
            info = Process.myUserHandle().equals(args.user)
                    ? InstallShortcutReceiver.fromShortcutIntent(this, data) : null;

            if (info == null) {
                Log.e(TAG, "Unable to parse a valid custom shortcut result");
                return;
            } else if (!new PackageManagerHelper(this).hasPermissionForActivity(
                    info.intent, args.getPendingIntent().getComponent().getPackageName())) {
                // The app is trying to add a shortcut without sufficient permissions
                Log.e(TAG, "Ignoring malicious intent " + info.intent.toUri(0));
                return;
            }
        }

        if (container < 0) {
            // Adding a shortcut to the Workspace.
            final View view = createShortcut(info);
            boolean foundCellSpan = false;
            // First we check if we already know the exact location where we want to add this item.
            if (cellX >= 0 && cellY >= 0) {
                cellXY[0] = cellX;
                cellXY[1] = cellY;
                foundCellSpan = true;

                // If appropriate, either create a folder or add to an existing folder
                if (workSpaceManager.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                        true, null)) {
                    return;
                }
                DragObject dragObject = new DragObject();
                dragObject.dragInfo = info;
                if (workSpaceManager.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                        true)) {
                    return;
                }
            } else {
                foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
            }

            if (!foundCellSpan) {
                workSpaceManager.onNoCellFound(layout);
                return;
            }
            getModelWriter().addItemToDatabase(info, container, screenId, cellXY[0], cellXY[1]);
            workSpaceManager.addInScreen(view, info);
        } else {
            // Adding a shortcut to a Folder.
            FolderIcon folderIcon = findFolderIcon(container);
            if (folderIcon != null) {
                FolderInfo folderInfo = (FolderInfo) folderIcon.getTag();
                folderInfo.add(info, args.rank, false);
            } else {
                Log.e(TAG, "Could not find folder with id " + container + " to add shortcut.");
            }
        }
    }

    public FolderIcon findFolderIcon(final long folderIconId) {
        return workSpaceManager.getFolderIcon(folderIconId);
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     */
    @Thunk void completeAddAppWidget(int appWidgetId, ItemInfo itemInfo,
            AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {

        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(appWidgetId);
        }

        LauncherAppWidgetInfo launcherInfo;
        launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = itemInfo.spanX;
        launcherInfo.spanY = itemInfo.spanY;
        launcherInfo.minSpanX = itemInfo.minSpanX;
        launcherInfo.minSpanY = itemInfo.minSpanY;
        launcherInfo.user = appWidgetInfo.getProfile();
        getModelWriter().addItemToDatabase(launcherInfo,
                itemInfo.container, itemInfo.screenId, itemInfo.cellX, itemInfo.cellY);

        if (hostView == null) {
            // Perform actual inflation because we're live
            hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        }
        hostView.setVisibility(View.VISIBLE);
        prepareAppWidget(hostView, launcherInfo);
        workSpaceManager.addInScreen(hostView, launcherInfo);
    }

    private void prepareAppWidget(AppWidgetHostView hostView, LauncherAppWidgetInfo item) {
        hostView.setTag(item);
        item.onBindAppWidget(this, hostView);
        hostView.setFocusable(true);
        hostView.setOnFocusChangeListener(mFocusHandler);
    }

    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Reset AllApps to its initial state only if we are not in the middle of
            // processing a multi-step drop
            if (mPendingRequestArgs == null) {
                mStateManager.goToState(NORMAL);
            }
        }
    };

    public void updateIconBadges(final Set<PackageUserKey> updatedBadges) {
        workSpaceManager.updateIconBadges(updatedBadges);
        allAppManager.updateIconBadges(updatedBadges);

        PopupContainerWithArrow popup = PopupContainerWithArrow.getOpen(Launcher.this);
        if (popup != null) {
            popup.updateNotificationHeader(updatedBadges);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDetachedFromWindow();
        }
    }

    public AllAppsTransitionController getAllAppsController() {
        return allAppManager.getController();
    }

    @Override
    public LauncherRootView getRootView() {
        return (LauncherRootView) mLauncherView;
    }

    @Override
    public DragLayer getDragLayer() {
        return dragManager.getmDragLayer();
    }

    public AllAppsContainerView getAppsView() {
        return allAppManager.getView();
    }

    public Workspace getWorkspace() {
        return workSpaceManager.getWorkspace();
    }


    public <T extends View> T getOverviewPanel() {
        return (T) mOverviewPanel;
    }

    public DropTargetBar getDropTargetBar() {
        return dropBarManager.getmDropTargetBar();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public ModelWriter getModelWriter() {
        return mModelWriter;
    }

    public SharedPreferences getSharedPrefs() {
        return mSharedPrefs;
    }

    public int getOrientation() { return mOldConfig.orientation; }

    @Override
    protected void onNewIntent(Intent intent) {
        TraceHelper.beginSection("NEW_INTENT");
        super.onNewIntent(intent);
        boolean alreadyOnHome = hasWindowFocus() && ((intent.getFlags() &
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        // Check this condition before handling isActionMain, as this will get reset.
        boolean shouldMoveToDefaultScreen = alreadyOnHome && isInState(NORMAL)
                && AbstractFloatingView.getTopOpenView(this) == null;
        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
        boolean internalStateHandled = InternalStateHandler
                .handleNewIntent(this, intent, isStarted());

        if (isActionMain) {
            if (!internalStateHandled) {
                // Note: There should be at most one log per method call. This is enforced
                // implicitly by using if-else statements.
                UserEventDispatcher ued = getUserEventDispatcher();
                AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this);
                if (topOpenView != null) {
                    topOpenView.logActionCommand(Action.Command.HOME_INTENT);
                } else if (alreadyOnHome) {
                    Target target = newContainerTarget(mStateManager.getState().containerType);
                    target.pageIndex = workSpaceManager.getNowPage();
                    ued.logActionCommand(Action.Command.HOME_INTENT, target,
                            newContainerTarget(ContainerType.WORKSPACE));
                }
                // In all these cases, only animate if we're already on home
                AbstractFloatingView.closeAllOpenViews(this, isStarted());
                if (!isInState(NORMAL)) {
                    // Only change state, if not already the same. This prevents cancelling any
                    // animations running as part of resume
                    mStateManager.goToState(NORMAL);
                }

                // Reset the apps view
                if (!alreadyOnHome) {
                    allAppManager.reSet( isStarted());
                }
                if(shouldMoveToDefaultScreen){
                    workSpaceManager.checkMoveToDefaultScreen();
                }
            }

            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                UiThreadHelper.hideKeyboardAsync(this, v.getWindowToken());
            }

            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onHomeIntent(internalStateHandled);
            }
        }

        TraceHelper.endSection("NEW_INTENT");
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        workSpaceManager.restoreInstanceStateForChild(mSynchronouslyBoundPage);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (workSpaceManager.getChildCountNumb() > 0) {
            outState.putInt(LauncherConfig.RUNTIME_STATE_CURRENT_SCREEN, workSpaceManager.getNextPage());

        }
        outState.putInt(LauncherConfig.RUNTIME_STATE, mStateManager.getState().ordinal);


        AbstractFloatingView widgets = AbstractFloatingView
                .getOpenView(this, AbstractFloatingView.TYPE_WIDGETS_FULL_SHEET);
        if (widgets != null) {
            SparseArray<Parcelable> widgetsState = new SparseArray<>();
            widgets.saveHierarchyState(widgetsState);
            outState.putSparseParcelableArray(LauncherConfig.RUNTIME_STATE_WIDGET_PANEL, widgetsState);
        } else {
            outState.remove(LauncherConfig.RUNTIME_STATE_WIDGET_PANEL);
        }

        // We close any open folders and shortcut containers since they will not be re-opened,
        // and we need to make sure this state is reflected.
        AbstractFloatingView.closeAllOpenViews(this, false);

        if (mPendingRequestArgs != null) {
            outState.putParcelable(LauncherConfig.RUNTIME_STATE_PENDING_REQUEST_ARGS, mPendingRequestArgs);
        }
        if (mPendingActivityResult != null) {
            outState.putParcelable(LauncherConfig.RUNTIME_STATE_PENDING_ACTIVITY_RESULT, mPendingActivityResult);
        }

        super.onSaveInstanceState(outState);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mScreenOffReceiver);
        workSpaceManager.removeListen();

        UiFactory.setOnTouchControllersChangedListener(this, null);

        // Stop callbacks from LauncherModel
        // It's possible to receive onDestroy after a new Launcher activity has
        // been created. In this case, don't interfere with the new Launcher.
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
            LauncherAppState.getInstance(this).setLauncher(null,null);
        }
        mRotationHelper.destroy();

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }

        TextKeyListener.getInstance().release();

        LauncherAnimUtils.onDestroyActivity();

        clearPendingBinds();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDestroy();
        }
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public DragController getDragController() {
        return dragManager.getmDragController();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startIntentSenderForResult (IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        try {
            super.startIntentSenderForResult(intent, requestCode,
                fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    /**
     * Indicates that we want global search for this activity by setting the globalSearch
     * argument for {@link #startSearch} to true.
     */
    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }

        if (mLauncherCallbacks == null ||
                !mLauncherCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData)) {
            // Starting search from the callbacks failed. Start the default global search.
            super.startSearch(initialQuery, selectInitialQuery, appSearchData, true);
        }

        // We need to show the workspace after starting the search
        mStateManager.goToState(NORMAL);
    }

    public boolean isWorkspaceLocked() {
        return workSpaceManager.isWorkspaceLoading()|| mPendingRequestArgs != null;
    }

    public boolean isWorkspaceLoading() {
        return workSpaceManager.isWorkspaceLoading();
    }

    private void setWorkspaceLoading(boolean value) {
       workSpaceManager.setmWorkspaceLoading(value);
    }

    public void setWaitingForResult(PendingRequestArgs args) {
        mPendingRequestArgs = args;
    }

    void addAppWidgetFromDropImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget,
            WidgetAddFlowHandler addFlowHandler) {
        if (LauncherConfig.LOGD) {
            Log.d(TAG, "Adding widget from drop");
        }
        addAppWidgetImpl(appWidgetId, info, boundWidget, addFlowHandler, 0);
    }

    void addAppWidgetImpl(int appWidgetId, ItemInfo info,
            AppWidgetHostView boundWidget, WidgetAddFlowHandler addFlowHandler, int delay) {
        if (!addFlowHandler.startConfigActivity(this, appWidgetId, info, LauncherConfig.REQUEST_CREATE_APPWIDGET)) {
            // If the configuration flow was not started, add the widget

            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    // Exit spring loaded mode if necessary after adding the widget
                    mStateManager.goToState(NORMAL, SPRING_LOADED_EXIT_DELAY);
                }
            };
            completeAddAppWidget(appWidgetId, info, boundWidget, addFlowHandler.getProviderInfo(this));
           workSpaceManager.removeExtraEmptyScreenDelayed( onComplete, delay);
        }
    }

    public void addPendingItem(PendingAddItemInfo info, long container, long screenId,
                               int[] cell, int spanX, int spanY) {
        info.container = container;
        info.screenId = screenId;
        if (cell != null) {
            info.cellX = cell[0];
            info.cellY = cell[1];
        }
        info.spanX = spanX;
        info.spanY = spanY;

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET:
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                addAppWidgetFromDrop((PendingAddWidgetInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                processShortcutFromDrop((PendingAddShortcutInfo) info);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
    }

    /**
     * Process a shortcut drop.
     */
    private void processShortcutFromDrop(PendingAddShortcutInfo info) {
        Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(info.componentName);
        setWaitingForResult(PendingRequestArgs.forIntent(LauncherConfig.REQUEST_CREATE_SHORTCUT, intent, info));
        if (!info.activityInfo.startConfigActivity(this, LauncherConfig.REQUEST_CREATE_SHORTCUT)) {
            handleActivityResult(LauncherConfig.REQUEST_CREATE_SHORTCUT, RESULT_CANCELED, null);
        }
    }

    /**
     * Process a widget drop.
     */
    private void addAppWidgetFromDrop(PendingAddWidgetInfo info) {
        AppWidgetHostView hostView = info.boundWidget;
        final int appWidgetId;
        WidgetAddFlowHandler addFlowHandler = info.getHandler();
        if (hostView != null) {
            // In the case where we've prebound the widget, we remove it from the DragLayer
            if (LauncherConfig.LOGD) {
                Log.d(TAG, "Removing widget view from drag layer and setting boundWidget to null");
            }
            getDragLayer().removeView(hostView);

            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetFromDropImpl(appWidgetId, info, hostView, addFlowHandler);

            // Clear the boundWidget so that it doesn't get destroyed.
            info.boundWidget = null;
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            if (FeatureFlags.ENABLE_CUSTOM_WIDGETS &&
                    info.itemType == LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET) {
                appWidgetId = CustomWidgetParser.getWidgetIdForCustomProvider(
                        this, info.componentName);
            } else {
                appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            }
            Bundle options = info.bindOptions;

            boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                    appWidgetId, info.info, options);
            if (success) {
                addAppWidgetFromDropImpl(appWidgetId, info, null, addFlowHandler);
            } else {
                addFlowHandler.startBindFlow(this, appWidgetId, info, LauncherConfig.REQUEST_BIND_APPWIDGET);
            }
        }
    }

    FolderIcon addFolder(CellLayout layout, long container, final long screenId, int cellX,
            int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        getModelWriter().addItemToDatabase(folderInfo, container, screenId, cellX, cellY);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo);
        workSpaceManager.addInScreen(newFolder, folderInfo);
        // Force measure the new folder icon
        CellLayout parent = workSpaceManager.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }

    /**
     * Unbinds the view for the specified item, and removes the item and all its children.
     *
     * @param v the view being removed.
     * @param itemInfo the {@link ItemInfo} for this view.
     * @param deleteFromDb whether or not to delete this item from the db.
     */
    public boolean removeItem(View v, final ItemInfo itemInfo, boolean deleteFromDb) {
        if (itemInfo instanceof ShortcutInfo) {
            // Remove the shortcut from the folder before removing it from launcher
            View folderIcon = workSpaceManager.getHomescreenIconByItemId(itemInfo.container);
            if (folderIcon instanceof FolderIcon) {
                ((FolderInfo) folderIcon.getTag()).remove((ShortcutInfo) itemInfo, true);
            } else {
                workSpaceManager.removeWorkspaceItem(v);
            }
            if (deleteFromDb) {
                getModelWriter().deleteItemFromDatabase(itemInfo);
            }
        } else if (itemInfo instanceof FolderInfo) {
            final FolderInfo folderInfo = (FolderInfo) itemInfo;
            if (v instanceof FolderIcon) {
                ((FolderIcon) v).removeListeners();
            }
            workSpaceManager.removeWorkspaceItem(v);
            if (deleteFromDb) {
                getModelWriter().deleteFolderAndContentsFromDatabase(folderInfo);
            }
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) itemInfo;
            workSpaceManager.removeWorkspaceItem(v);
            if (deleteFromDb) {
                deleteWidgetInfo(widgetInfo);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Deletes the widget info and the widget id.
     */
    private void deleteWidgetInfo(final LauncherAppWidgetInfo widgetInfo) {
        final LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
        if (appWidgetHost != null && !widgetInfo.isCustomWidget() && widgetInfo.isWidgetIdAllocated()) {
            // Deleting an app widget ID is a void call but writes to disk before returning
            // to the caller...
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void ... args) {
                    appWidgetHost.deleteAppWidgetId(widgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
        }
        getModelWriter().deleteItemFromDatabase(widgetInfo);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return (event.getKeyCode() == KeyEvent.KEYCODE_HOME) || super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (finishAutoCancelActionMode()) {
            return;
        }
        if (mLauncherCallbacks != null && mLauncherCallbacks.handleBackPressed()) {
            return;
        }

        if (dragManager.getmDragController().isDragging()) {
            dragManager.getmDragController().cancelDrag();
            return;
        }

        // Note: There should be at most one log per method call. This is enforced implicitly
        // by using if-else statements.
        UserEventDispatcher ued = getUserEventDispatcher();
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
        if (topView != null && topView.onBackPressed()) {
            // Handled by the floating view.
        } else if (!isInState(NORMAL)) {
            LauncherState lastState = mStateManager.getLastState();
            ued.logActionCommand(Action.Command.BACK, mStateManager.getState().containerType,
                    lastState.containerType);
            mStateManager.goToState(lastState);
        } else {
            // Back button is a no-op here, but give at least some feedback for the button press
            workSpaceManager.showOutlinesTemporarily();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public ActivityOptions getActivityLaunchOptions(View v) {
        return mAppTransitionManager.getActivityLaunchOptions(this, v);
    }

    public LauncherAppTransitionManager getAppTransitionManager() {
        return mAppTransitionManager;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected boolean onErrorStartingShortcut(Intent intent, ItemInfo info) {
        // Due to legacy reasons, direct call shortcuts require Launchers to have the
        // corresponding permission. Show the appropriate permission prompt if that
        // is the case.
        if (intent.getComponent() == null
                && Intent.ACTION_CALL.equals(intent.getAction())
                && checkSelfPermission(android.Manifest.permission.CALL_PHONE) !=
                PackageManager.PERMISSION_GRANTED) {

            setWaitingForResult(PendingRequestArgs
                    .forIntent(LauncherConfig.REQUEST_PERMISSION_CALL_PHONE, intent, info));
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE},
                    LauncherConfig.REQUEST_PERMISSION_CALL_PHONE);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void modifyUserEvent(LauncherLogProto.LauncherEvent event) {
        if (event.srcTarget != null && event.srcTarget.length > 0 &&
                event.srcTarget[1].containerType == ContainerType.PREDICTION) {
            Target[] targets = new Target[3];
            targets[0] = event.srcTarget[0];
            targets[1] = event.srcTarget[1];
            targets[2] = newTarget(Target.Type.CONTAINER);
            event.srcTarget = targets;
            LauncherState state = mStateManager.getState();
            if (state == LauncherState.ALL_APPS) {
                event.srcTarget[2].containerType = ContainerType.ALLAPPS;
            } else if (state == LauncherState.OVERVIEW) {
                event.srcTarget[2].containerType = ContainerType.TASKSWITCHER;
            }
        }
    }

    public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
        boolean success = super.startActivitySafely(v, intent, item);
        if (success && v instanceof BubbleTextView) {
            // This is set to the view that launched the activity that navigated the user away
            // from launcher. Since there is no callback for when the activity has finished
            // launching, enable the press state and keep this reference to reset the press
            // state when we return to launcher.
            BubbleTextView btv = (BubbleTextView) v;
            btv.setStayPressed(true);
            setOnResumeCallback(btv);
        }
        return success;
    }



    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    public CellLayout getCellLayout(long container, long screenId) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            return hotseatManager.getCellLayout();
        } else {
            return workSpaceManager.getScreenWithId(screenId);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();

            // This clears all widget bitmaps from the widget tray
            // TODO(hyunyoungs)
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onTrimMemory(level);
        }
        UiFactory.onTrimMemory(this, level);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        // TODO: When can workspace be null?
        text.add(checkWorkSpaceIsNull()
                ? getString(R.string.all_apps_home_button_label)
                : mStateManager.getState().getDescription(this));
        return result;
    }

    boolean checkWorkSpaceIsNull(){
        return workSpaceManager==null||workSpaceManager.getWorkspace()==null;
    }

    public void setOnResumeCallback(OnResumeCallback callback) {
        if (mOnResumeCallback != null) {
            mOnResumeCallback.onLauncherResume();
        }
        mOnResumeCallback = callback;
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public int getCurrentWorkspaceScreen() {
        if (!checkWorkSpaceIsNull()) {
            return workSpaceManager.getNowPage();
        } else {
            return 0;
        }
    }

    /**
     * Clear any pending bind callbacks. This is called when is loader is planning to
     * perform a full rebind from scratch.
     */
    @Override
    public void clearPendingBinds() {
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
            mPendingExecutor = null;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        TraceHelper.beginSection("startBinding");
        // Floating panels (except the full widget sheet) are associated with individual icons. If
        // we are starting a fresh bind, close all such panels as all the icons are about
        // to go away.
        AbstractFloatingView.closeOpenViews(this, true,
                AbstractFloatingView.TYPE_ALL & ~AbstractFloatingView.TYPE_REBIND_SAFE);

        setWorkspaceLoading(true);

        // Clear the workspace because it's going to be rebound
       workSpaceManager.clearScreens();
        mAppWidgetHost.clearViews();
        hotseatManager.resetLayout(mDeviceProfile.isVerticalBarLayout());
        TraceHelper.endSection("startBinding");
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        // Make sure the first screen is always at the start.
        if (FeatureFlags.QSB_ON_FIRST_SCREEN &&
                orderedScreenIds.indexOf(Workspace.FIRST_SCREEN_ID) != 0) {
            orderedScreenIds.remove(Workspace.FIRST_SCREEN_ID);
            orderedScreenIds.add(0, Workspace.FIRST_SCREEN_ID);
            LauncherModel.updateWorkspaceScreenOrder(this, orderedScreenIds);
        } else if (!FeatureFlags.QSB_ON_FIRST_SCREEN && orderedScreenIds.isEmpty()) {
            // If there are no screens, we need to have an empty screen
            workSpaceManager.addEmpScreen();
        }
        bindAddScreens(orderedScreenIds);

        // After we have added all the screens, if the wallpaper was locked to the default state,
        // then notify to indicate that it can be released and a proper wallpaper offset can be
        // computed before the next layout

        workSpaceManager.unlockWallpaperFromDefaultPageOnNextLayout();
    }

    private void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            long screenId = orderedScreenIds.get(i);
            if (!FeatureFlags.QSB_ON_FIRST_SCREEN || screenId != Workspace.FIRST_SCREEN_ID) {
                // No need to bind the first screen, as its always bound.
                workSpaceManager.insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
            }
        }
    }

    @Override
    public void bindAppsAdded(ArrayList<Long> newScreens, ArrayList<ItemInfo> addNotAnimated,
            ArrayList<ItemInfo> addAnimated) {
        // Add the new screens
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }

        // We add the items without animation on non-visible pages, and with
        // animations on the new page (which we will try and snap to).
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, true);
        }

        // Remove the extra empty screen
        workSpaceManager.removeEmpScreen();
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindItems(final List<ItemInfo> items, final boolean forceAnimateIcons) {
        // Get the list of added items and intersect them with the set of items here
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<>();
        final boolean animateIcons = forceAnimateIcons && canRunNewAppsAnimation();
        Workspace workspace = workSpaceManager.getWorkspace();
        long newItemsScreenId = -1;
        int end = items.size();
        for (int i = 0; i < end; i++) {
            final ItemInfo item = items.get(i);

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                   hotseatManager.checkViewIsNull()) {
                continue;
            }

            final View view;
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT: {
                    ShortcutInfo info = (ShortcutInfo) item;
                    view = createShortcut(info);
                    break;
                }
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER: {
                    view = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item);
                    break;
                }
                case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET: {
                    view = inflateAppWidget((LauncherAppWidgetInfo) item);
                    if (view == null) {
                        continue;
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Invalid Item Type");
            }

             /*
             * Remove colliding items.
             */
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                CellLayout cl = workSpaceManager.getCellLayoutById(item.screenId);
                if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
                    View v = cl.getChildAt(item.cellX, item.cellY);
                    Object tag = v.getTag();
                    String desc = "Collision while binding workspace item: " + item
                            + ". Collides with " + tag;
                    if (FeatureFlags.IS_DOGFOOD_BUILD) {
                        throw (new RuntimeException(desc));
                    } else {
                        Log.d(TAG, desc);
                        getModelWriter().deleteItemFromDatabase(item);
                        continue;
                    }
                }
            }

            workspace.addInScreenFromBind(view, item);
            if (animateIcons) {
                // Animate all the applications up now
                view.setAlpha(0f);
                view.setScaleX(0f);
                view.setScaleY(0f);
                bounceAnims.add(createNewAppBounceAnimation(view, i));
                newItemsScreenId = item.screenId;
            }
        }
        if (animateIcons) {
            // Animate to the correct page
            workSpaceManager.bindItem(newItemsScreenId,anim,bounceAnims,this);
        }
        workspace.requestLayout();
    }

    /**
     * Add the views for a widget to the workspace.
     */
    public void bindAppWidget(LauncherAppWidgetInfo item) {
        View view = inflateAppWidget(item);
        if (view != null) {
            workSpaceManager.addInScreen(view, item);
            workSpaceManager.requestLayout();
        }
    }

    private View inflateAppWidget(LauncherAppWidgetInfo item) {
        if (mIsSafeModeEnabled) {
            PendingAppWidgetHostView view =
                    new PendingAppWidgetHostView(this, item, mIconCache, true);
            prepareAppWidget(view, item);
            return view;
        }

        TraceHelper.beginSection("BIND_WIDGET");

        final LauncherAppWidgetProviderInfo appWidgetInfo;

        if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)) {
            // If the provider is not ready, bind as a pending widget.
            appWidgetInfo = null;
        } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
            // The widget id is not valid. Try to find the widget based on the provider info.
            appWidgetInfo = mAppWidgetManager.findProvider(item.providerName, item.user);
        } else {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(item.appWidgetId);
        }

        // If the provider is ready, but the width is not yet restored, try to restore it.
        if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY) &&
                (item.restoreStatus != LauncherAppWidgetInfo.RESTORE_COMPLETED)) {
            if (appWidgetInfo == null) {
                Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                        + " belongs to component " + item.providerName
                        + ", as the provider is null");
                getModelWriter().deleteItemFromDatabase(item);
                return null;
            }

            // If we do not have a valid id, try to bind an id.
            if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_ALLOCATED)) {
                    // Id has not been allocated yet. Allocate a new id.
                    item.appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                    item.restoreStatus |= LauncherAppWidgetInfo.FLAG_ID_ALLOCATED;

                    // Also try to bind the widget. If the bind fails, the user will be shown
                    // a click to setup UI, which will ask for the bind permission.
                    PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(appWidgetInfo);
                    pendingInfo.spanX = item.spanX;
                    pendingInfo.spanY = item.spanY;
                    pendingInfo.minSpanX = item.minSpanX;
                    pendingInfo.minSpanY = item.minSpanY;
                    Bundle options = WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingInfo);

                    boolean isDirectConfig =
                            item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG);
                    if (isDirectConfig && item.bindOptions != null) {
                        Bundle newOptions = item.bindOptions.getExtras();
                        if (options != null) {
                            newOptions.putAll(options);
                        }
                        options = newOptions;
                    }
                    boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                            item.appWidgetId, appWidgetInfo, options);

                    // We tried to bind once. If we were not able to bind, we would need to
                    // go through the permission dialog, which means we cannot skip the config
                    // activity.
                    item.bindOptions = null;
                    item.restoreStatus &= ~LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG;

                    // Bind succeeded
                    if (success) {
                        // If the widget has a configure activity, it is still needs to set it up,
                        // otherwise the widget is ready to go.
                        item.restoreStatus = (appWidgetInfo.configure == null) || isDirectConfig
                                ? LauncherAppWidgetInfo.RESTORE_COMPLETED
                                : LauncherAppWidgetInfo.FLAG_UI_NOT_READY;
                    }

                    getModelWriter().updateItemInDatabase(item);
                }
            } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_UI_NOT_READY)
                    && (appWidgetInfo.configure == null)) {
                // The widget was marked as UI not ready, but there is no configure activity to
                // update the UI.
                item.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;
                getModelWriter().updateItemInDatabase(item);
            }
        }

        final AppWidgetHostView view;
        if (item.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            // Verify that we own the widget
            if (appWidgetInfo == null) {
                FileLog.e(TAG, "Removing invalid widget: id=" + item.appWidgetId);
                deleteWidgetInfo(item);
                return null;
            }

            item.minSpanX = appWidgetInfo.minSpanX;
            item.minSpanY = appWidgetInfo.minSpanY;
            view = mAppWidgetHost.createView(this, item.appWidgetId, appWidgetInfo);
        } else {
            view = new PendingAppWidgetHostView(this, item, mIconCache, false);
        }
        prepareAppWidget(view, item);

        TraceHelper.endSection("BIND_WIDGET", "id=" + item.appWidgetId);
        return view;
    }

    /**
     * Restores a pending widget.
     *
     * @param appWidgetId The app widget id
     */
    private LauncherAppWidgetInfo completeRestoreAppWidget(int appWidgetId, int finalRestoreFlag) {
        LauncherAppWidgetHostView view = workSpaceManager.getLauncherAppWidgetHostView(appWidgetId);
        if ((view == null) || !(view instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return null;
        }
        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = finalRestoreFlag;
        if (info.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            info.pendingItemInfo = null;
        }

        if (((PendingAppWidgetHostView) view).isReinflateIfNeeded()) {
            view.reInflate();
        }

        getModelWriter().updateItemInDatabase(info);
        return info;
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPage = page;
    }

    @Override
    public void executeOnNextDraw(ViewOnDrawExecutor executor) {
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
        }
        mPendingExecutor = executor;
        if (!isInState(ALL_APPS)) {
            allAppManager.setDeferUpdates(true);

            mPendingExecutor.execute(() -> allAppManager.setDeferUpdates(false));
        }

        executor.attachTo(this);
    }

    public void clearPendingExecutor(ViewOnDrawExecutor executor) {
        if (mPendingExecutor == executor) {
            mPendingExecutor = null;
        }
    }

    @Override
    public void finishFirstPageBind(final ViewOnDrawExecutor executor) {
        AlphaProperty property =dragManager.getProperty();
        if (property.getValue() < 1) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(property, MultiValueAlpha.VALUE, 1);
            if (executor != null) {
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        executor.onLoadAnimationCompleted();
                    }
                });
            }
            anim.start();
        } else if (executor != null) {
            executor.onLoadAnimationCompleted();
        }
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        TraceHelper.beginSection("finishBindingItems");
        workSpaceManager.restoreInstanceStateForRemainingPages();

        setWorkspaceLoading(false);

        if (mPendingActivityResult != null) {
            handleActivityResult(mPendingActivityResult.requestCode,
                    mPendingActivityResult.resultCode, mPendingActivityResult.data);
            mPendingActivityResult = null;
        }

        InstallShortcutReceiver.disableAndFlushInstallQueue(
                InstallShortcutReceiver.FLAG_LOADER_RUNNING, this);

        TraceHelper.endSection("finishBindingItems");
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - dragManager.getmDragController().getLastGestureUpTime();
        return diff > (LauncherConfig.NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofViewAlphaAndScale(v, 1, 1, 1);
        bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(LauncherConfig.BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(ArrayList<AppInfo> apps) {
        allAppManager.setApps(apps);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.bindAllApplications(apps);
        }
    }

    /**
     * Copies LauncherModel's map of activities to shortcut ids to Launcher's. This is necessary
     * because LauncherModel's map is updated in the background, while Launcher runs on the UI.
     */
    @Override
    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        mPopupDataProvider.setDeepShortcutMap(deepShortcutMapCopy);
    }

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindAppsAddedOrUpdated(ArrayList<AppInfo> apps) {
        allAppManager.addOrUpdateApps(apps);

    }

    @Override
    public void bindPromiseAppProgressUpdated(PromiseAppInfo app) {
        allAppManager.updatePromiseAppProgress(app);
    }

    @Override
    public void bindWidgetsRestored(ArrayList<LauncherAppWidgetInfo> widgets) {
        workSpaceManager.getWorkspace().widgetsRestored(widgets);
    }

    /**
     * Some shortcuts were updated in the background.
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @param updated list of shortcuts which have changed.
     */
    @Override
    public void bindShortcutsChanged(ArrayList<ShortcutInfo> updated, final UserHandle user) {
        if (!updated.isEmpty()) {
           workSpaceManager.getWorkspace().updateShortcuts(updated);
        }
    }

    /**
     * Update the state of a package, typically related to install state.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindRestoreItemsChange(HashSet<ItemInfo> updates) {
        workSpaceManager.getWorkspace().updateRestoreItems(updates);
    }

    /**
     * A package was uninstalled/updated.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace and hotseat, where as
     * package-removal should clear all items by package name.
     */
    @Override
    public void bindWorkspaceComponentsRemoved(final ItemInfoMatcher matcher) {
        workSpaceManager.getWorkspace().removeItemsByMatcher(matcher);
       dragManager.getmDragController().onAppsRemoved(matcher);
    }

    @Override
    public void bindAppInfosRemoved(final ArrayList<AppInfo> appInfos) {
        allAppManager.removeApps(appInfos);
    }

    @Override
    public void bindAllWidgets(final ArrayList<WidgetListRowEntry> allWidgets) {
        mPopupDataProvider.setAllWidgets(allWidgets);
        AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(this);
        if (topView != null) {
            topView.onWidgetsBound();
        }
    }

    /**
     * @param packageUser if null, refreshes all widgets and shortcuts, otherwise only
     *                    refreshes the widgets and shortcuts associated with the given package/user
     */
    public void refreshAndBindWidgetsForPackageUser(@Nullable PackageUserKey packageUser) {
        mModel.refreshAndBindWidgetsAndShortcuts(packageUser);
    }

    /**
     * $ adb shell dumpsys activity com.android.launcher3.Launcher [--all]
     */
    @Override

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        if (args.length > 0 && TextUtils.equals(args[0], "--all")) {
            writer.println(prefix + "Workspace Items");
            for (int i = 0; i < workSpaceManager.getPageCount(); i++) {
                writer.println(prefix + "  Homescreen " + i);
                ViewGroup layout = workSpaceManager.getCellLayoutByPageIndex(i);
                for (int j = 0; j < layout.getChildCount(); j++) {
                    Object tag = layout.getChildAt(j).getTag();
                    if (tag != null) {
                        writer.println(prefix + "    " + tag.toString());
                    }
                }
            }

            writer.println(prefix + "  Hotseat");
            ViewGroup layout =hotseatManager.getHotseatViewGroup();
            for (int j = 0; j < layout.getChildCount(); j++) {
                Object tag = layout.getChildAt(j).getTag();
                if (tag != null) {
                    writer.println(prefix + "    " + tag.toString());
                }
            }
        }
        writer.println(prefix + "Misc:");
        writer.print(prefix + "\tmWorkspaceLoading=" + workSpaceManager.isWorkspaceLoading());
        writer.print(" mPendingRequestArgs=" + mPendingRequestArgs);
        writer.println(" mPendingActivityResult=" + mPendingActivityResult);
        writer.println(" mRotationHelper: " + mRotationHelper);
        dumpMisc(writer);
        try {
            FileLog.flushAll(writer);
        } catch (Exception e) {
            // Ignore
        }

        mModel.dumpState(prefix, fd, writer, args);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.dump(prefix, fd, writer, args);
        }
    }

    public boolean isHotseatLayout(View layout){
        return hotseatManager.isHotseatLayout(layout);
    }


    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public void onProvideKeyboardShortcuts(
            List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {

        ArrayList<KeyboardShortcutInfo> shortcutInfos = new ArrayList<>();
        if (isInState(NORMAL)) {
            shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.all_apps_button_label),
                    KeyEvent.KEYCODE_A, KeyEvent.META_CTRL_ON));
            shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.widget_button_text),
                    KeyEvent.KEYCODE_W, KeyEvent.META_CTRL_ON));
        }
        final View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            if (new CustomActionsPopup(this, currentFocus).canShow()) {
                shortcutInfos.add(new KeyboardShortcutInfo(getString(R.string.custom_actions),
                        KeyEvent.KEYCODE_O, KeyEvent.META_CTRL_ON));
            }
            if (currentFocus.getTag() instanceof ItemInfo
                    && DeepShortcutManager.supportsShortcuts((ItemInfo) currentFocus.getTag())) {
                shortcutInfos.add(new KeyboardShortcutInfo(
                        getString(R.string.shortcuts_menu_with_notifications_description),
                        KeyEvent.KEYCODE_S, KeyEvent.META_CTRL_ON));
            }
        }
        if (!shortcutInfos.isEmpty()) {
            data.add(new KeyboardShortcutGroup(getString(R.string.home_screen), shortcutInfos));
        }

        super.onProvideKeyboardShortcuts(data, menu, deviceId);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (event.hasModifiers(KeyEvent.META_CTRL_ON)) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    if (isInState(NORMAL)) {
                        getStateManager().goToState(ALL_APPS);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_S: {
                    View focusedView = getCurrentFocus();
                    if (focusedView instanceof BubbleTextView
                            && focusedView.getTag() instanceof ItemInfo
                            && mAccessibilityDelegate.performAction(focusedView,
                                    (ItemInfo) focusedView.getTag(),
                                    LauncherAccessibilityDelegate.DEEP_SHORTCUTS)) {
                        PopupContainerWithArrow.getOpen(this).requestFocus();
                        return true;
                    }
                    break;
                }
                case KeyEvent.KEYCODE_O:
                    if (new CustomActionsPopup(this, getCurrentFocus()).show()) {
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_W:
                    if (isInState(NORMAL)) {
                        OptionsPopupView.openWidgets(this);
                        return true;
                    }
                    break;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // KEYCODE_MENU is sent by some tests, for example
            // LauncherJankTests#testWidgetsContainerFling. Don't just remove its handling.
            if (!dragManager.nowIsDragging() && !workSpaceManager.isSwitchingState() &&
                    isInState(NORMAL)) {
                // Close any open floating views.
                AbstractFloatingView.closeAllOpenViews(this);

                // Setting the touch point to (-1, -1) will show the options popup in the center of
                // the screen.
                OptionsPopupView.showDefaultOptions(this, -1, -1);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public static Launcher getLauncher(Context context) {
        if (context instanceof Launcher) {
            return (Launcher) context;
        }
        return ((Launcher) ((ContextWrapper) context).getBaseContext());
    }


}
