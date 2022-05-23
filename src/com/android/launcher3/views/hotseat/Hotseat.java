/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.launcher3.views.hotseat;

import static com.android.launcher3.LauncherState.ALL_APPS;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.appliction.MyApplication;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider;
import com.android.launcher3.userevent.nano.LauncherLogProto.Action;
import com.android.launcher3.userevent.nano.LauncherLogProto.ContainerType;
import com.android.launcher3.userevent.nano.LauncherLogProto.ControlType;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;

public class Hotseat extends LinearLayout implements LogContainerProvider, Insettable {

    private final Launcher mLauncher;
    private CellLayout mContent;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mHasVerticalHotseat;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = Launcher.getLauncher(context);
    }

    private HotseatListen hotseatListen;

    public void setHotseatListen(HotseatListen hotseatListen){
        this.hotseatListen=hotseatListen;
    }

    public CellLayout getLayout() {
        return mContent;
    }


    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    public int getOrderInHotseat(int x, int y) {
        return mHasVerticalHotseat ? (mContent.getCountY() - y - 1) : x;
    }

    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    public int getCellXFromOrder(int rank) {
        return mHasVerticalHotseat ? 0 : rank;
    }

    public int getCellYFromOrder(int rank) {
        return mHasVerticalHotseat ? (mContent.getCountY() - (rank + 1)) : 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.layout);
        findViewById(R.id.iv_show_all_app).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null!=hotseatListen){
                    hotseatListen.nowNeedShowAllAppView();
                }
            }
        });
        initCellLayoutLP();

    }

    private void initCellLayoutLP(){
        mContent .setBackgroundResource(R.drawable.hotseat_bg);



    }

   public void resetLayout(boolean hasVerticalHotseat) {

        mContent.removeAllViewsInLayout();
        mHasVerticalHotseat = hasVerticalHotseat;
        InvariantDeviceProfile idp = mLauncher.getDeviceProfile().inv;

        switch (showType){
            case DEF:
                if (hasVerticalHotseat) {
                    mContent.setGridSize(1, idp.numHotseatIcons);
                } else {
                    mContent.setGridSize(idp.numHotseatIcons, 1);
                }
                break;
            case TOP:
            case BOTTOM:
                mContent.setGridSize(HotseatConfig.ShowNumb, 1);
                break;
            case LEFT:
            case RIGHT:
                mContent.setGridSize(1, idp.numHotseatIcons);
                break;
        }
       Log.i("lixiao","kankancell多大:"+idp.numHotseatIcons);
        if (!FeatureFlags.NO_ALL_APPS_ICON) {
            Log.i("lixiao","kankancell多大:111");
            // Add the Apps button
            Context context = getContext();
            DeviceProfile grid = mLauncher.getDeviceProfile();
            int allAppsButtonRank = grid.inv.getAllAppsButtonRank();

            LayoutInflater inflater = LayoutInflater.from(context);
            TextView allAppsButton = (TextView)
                    inflater.inflate(R.layout.all_apps_button, mContent, false);
            Drawable d = context.getResources().getDrawable(R.drawable.all_apps_button_icon);
            d.setBounds(0, 0, grid.iconSizePx, grid.iconSizePx);

            int scaleDownPx = getResources().getDimensionPixelSize(R.dimen.all_apps_button_scale_down);
            Rect bounds = d.getBounds();
            d.setBounds(bounds.left, bounds.top + scaleDownPx / 2, bounds.right - scaleDownPx,
                    bounds.bottom - scaleDownPx / 2);
            allAppsButton.setCompoundDrawables(null, d, null, null);

            allAppsButton.setContentDescription(context.getString(R.string.all_apps_button_label));
            if (mLauncher != null) {
                allAppsButton.setOnClickListener((v) -> {
                    if (!mLauncher.isInState(ALL_APPS)) {
                        mLauncher.getUserEventDispatcher().logActionOnControl(Action.Touch.TAP,
                                ControlType.ALL_APPS_BUTTON);
                        mLauncher.getStateManager().goToState(ALL_APPS);
                    }
                });
                allAppsButton.setOnFocusChangeListener(mLauncher.mFocusHandler);
            }

            // Note: We do this to ensure that the hotseat is always laid out in the orientation of
            // the hotseat in order regardless of which orientation they were added
            int x = getCellXFromOrder(allAppsButtonRank);
            int y = getCellYFromOrder(allAppsButtonRank);
            CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x, y, 1, 1);
            lp.canReorder = false;
            allAppsButton.setBackgroundColor(Color.RED);
            mContent.addViewToCellLayout(allAppsButton, -1, allAppsButton.getId(), lp, true);
            Log.i("lixiao","kankancell多大:333");
        }
    }


    /**
     * 手势判断是否生效
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the hotseat unless the workspace is in
        // the normal state or an accessible drag is in progress.
        Log.i("lixiao","kankancell多大:22");
        return !mLauncher.getWorkspace().workspaceIconsCanBeDragged() &&
                !mLauncher.getAccessibilityDelegate().isInAccessibleDrag();
    }

    @Override
    public void fillInLogContainerData(View v, ItemInfo info, Target target, Target targetParent) {
        Log.i("lixiao","kankancell多大:33");
        target.gridX = info.cellX;
        target.gridY = info.cellY;
        targetParent.containerType = ContainerType.HOTSEAT;
    }

    @Override
    public void setInsets(Rect insets) {
        Log.i("lixiao","kankancell多大:44"+insets.toString());
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile grid = mLauncher.getDeviceProfile();
//        switch (showType){
//            case DEF:
//                if (grid.isVerticalBarLayout()) {
//                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                    if (grid.isSeascape()) {
//                        lp.gravity = Gravity.LEFT;
//                        lp.width = grid.hotseatBarSizePx + insets.left;
//                    } else {
//                        lp.gravity = Gravity.RIGHT;
//                        lp.width = grid.hotseatBarSizePx + insets.right;
//                    }
//                } else {
//                    lp.gravity = Gravity.BOTTOM;
//                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//                    lp.height = grid.hotseatBarSizePx + insets.bottom;
//                }
//                break;
//            case LEFT:
//                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                lp.gravity = Gravity.LEFT;
//                lp.width = grid.hotseatBarSizePx + insets.left;
//                break;
//            case RIGHT:
//                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
//                lp.gravity = Gravity.RIGHT;
//                lp.width = grid.hotseatBarSizePx + insets.right;
//                break;
//            case TOP:
//                lp.gravity = Gravity.TOP;
//                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//                lp.height = grid.hotseatBarSizePx + insets.top;
//                break;
//            case BOTTOM:
////                lp.gravity = Gravity.BOTTOM;
//                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//                lp.height = grid.hotseatBarSizePx + insets.bottom;
//                break;
//        }
//        Rect padding = grid.getHotseatLayoutPadding();
//        getLayout().setPadding(padding.left, padding.top, padding.right, padding.bottom);
//        getLayout().setFixedSize(744,128);

//        lp.gravity = Gravity.BOTTOM;
////
////        lp.height= grid.hotseatBarSizePx ;
//                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
//                        lp.height = 112;
//        lp.bottomMargin=24;
//        setLayoutParams(lp);

//        setBackgroundColor(Color.GREEN);
        InsettableFrameLayout.dispatchInsets(this, insets);

    }

    HotseatShowType showType=HotseatShowType.BOTTOM;

    public void setShowType(HotseatShowType setShowType){
        if(setShowType==null){
            return;
        }
        showType=setShowType;
        resetLayout(mHasVerticalHotseat);
    }


}
