package com.android.launcher3.manager.view;

import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.CellLayout;
import com.android.launcher3.Hotseat;
import com.android.launcher3.util.Thunk;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/11 0011 11:38
 */
public class HotseatManager {
    @Thunk Hotseat mHotseat;
    @Nullable private View mHotseatSearchBox;
    public HotseatManager(Hotseat mHotseat,View mHotseatSearchBox){
        this.mHotseat=mHotseat;
        this.mHotseatSearchBox=mHotseatSearchBox;
    }


   public boolean isHotseatLayout(View layout) {
        // TODO: Remove this method
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }

    public  CellLayout getCellLayout(){
        if (mHotseat != null) {
            return mHotseat.getLayout();
        } else {
            return null;
        }
    }
    public void resetLayout(boolean isVerticalBarLayout){
        if (mHotseat != null) {
            mHotseat.resetLayout(isVerticalBarLayout);
        }
    }


    public  boolean checkViewIsNull(){
        return mHotseat == null;
    }

    public   ViewGroup getHotseatViewGroup(){
        return mHotseat.getLayout().getShortcutsAndWidgets();
    }

    public Hotseat getHotseat() {
        return mHotseat;
    }

    public View getHotseatSearchBox() {
        Log.i("lixiao","kankanviewshifoudengyunull"+(mHotseatSearchBox==null));
        return mHotseatSearchBox;
    }


}
