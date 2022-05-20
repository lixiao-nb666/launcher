package com.android.launcher3.manager.view;

import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.PromiseAppInfo;

import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.Thunk;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/11 0011 10:05
 */
public class AllAppManager {

    // Main container view for the all apps screen.

    @Thunk AllAppsContainerView mAppsView;
    AllAppsTransitionController mAllAppsController;
    public  AllAppManager(Launcher launcher){
        mAllAppsController = new AllAppsTransitionController(launcher);
    }

    public void setDefView(AllAppsContainerView mAppsView){
        this.mAppsView=mAppsView;
        mAllAppsController.setupViews(mAppsView);
    }


    public void ScrollToAppsView(){
//        mAllAppsController.setStateWithAnimation();
    }

    public void updateIconBadges(final Set<PackageUserKey> updatedBadges){
        mAppsView.getAppsStore().updateIconBadges(updatedBadges);
    }

   public AllAppsTransitionController getController(){
        return mAllAppsController;
    }

    public  AllAppsContainerView getView(){
        return mAppsView;
    }

    public void reSet(boolean canReset){
        mAppsView.reset(canReset /* animate */);
    }

    public void setDeferUpdates(boolean canUpdate){
        mAppsView.getAppsStore().setDeferUpdates(canUpdate);
    }

    public  void setApps(ArrayList<AppInfo> apps){
        mAppsView.getAppsStore().setApps(apps);
    }

    public void  addOrUpdateApps(ArrayList<AppInfo> apps){
        mAppsView.getAppsStore().addOrUpdateApps(apps);
    }

    public void updatePromiseAppProgress(PromiseAppInfo app){
        mAppsView.getAppsStore().updatePromiseAppProgress(app);
    }

    public void removeApps(final ArrayList<AppInfo> appInfos){
        mAppsView.getAppsStore().removeApps(appInfos);
    }



}
