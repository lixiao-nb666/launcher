/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static android.content.pm.ActivityInfo.CONFIG_ORIENTATION;
import static android.content.pm.ActivityInfo.CONFIG_SCREEN_SIZE;
import static com.android.launcher3.LauncherAnimUtils.SPRING_LOADED_EXIT_DELAY;
import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.dragndrop.DragLayer.ALPHA_INDEX_LAUNCHER_LOAD;
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
import com.android.launcher3.Workspace.ItemOperator;
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
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.logging.UserEventDispatcher.UserEventDelegate;
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
public class LiXiaoLauncher extends Launcher{

}
