package com.android.launcher3.manager.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.graphics.Color;
import android.net.wifi.aware.AttachCallback;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseDraggingActivity;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherConfig;
import com.android.launcher3.PagedViewChangePageListen;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.listen.LauncherOverlay;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

import java.util.Collection;
import java.util.Set;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/12 0012 11:15
 */
public class WorkSpaceManager {
    @Thunk Workspace mWorkspace;
    @Thunk boolean mWorkspaceLoading = true;
    public WorkSpaceManager(DragLayer mDragLayer){
        mWorkspace = mDragLayer.findViewById(R.id.workspace);
        mWorkspace.initParentViews(mDragLayer);
        mWorkspace.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                Log.i("lixiao","kankan:Attached");
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                Log.i("lixiao","kankan:Detache");
            }
        });


        mWorkspace.setOnPageChangeListen(new PagedViewChangePageListen() {
            @Override
            public void nowPage(int pageNumb, int now) {
                Log.i("lixiao","kankan222:"+pageNumb+"-"+now);
            }
        });

    }

    public Workspace getWorkspace(){
        return mWorkspace;
    }


    public void setPageNumb(int page){
        // Pages bound synchronously.
        mWorkspace.setCurrentPage(page);
        setWorkspaceLoading(true);

    }

    public void addDemo(){
        CellLayout demoView=new CellLayout(mWorkspace.getContext());
        demoView.setLayoutParams(new CellLayout.LayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)));
        demoView.setBackgroundResource(R.drawable.innocn);
        mWorkspace.addView(demoView);
    }

    private void setWorkspaceLoading(boolean value) {
        mWorkspaceLoading = value;
    }

    public int getNowPage(){
        return mWorkspace.getCurrentPage();
    }

    public int getNextPage(){
        return mWorkspace.getNextPage();
    }

    public int getChildCountNumb(){
        return mWorkspace.getChildCount();
    }

    public int getPageCount(){
        return mWorkspace.getPageCount();
    }

    public ViewGroup getCellLayoutByPageIndex(int index) {
        return ((CellLayout) mWorkspace.getPageAt(index)).getShortcutsAndWidgets();
    }

    public void setmWorkspaceLoading(boolean setWorkspaceLoading){
        mWorkspaceLoading=setWorkspaceLoading;
    }

    public boolean isSwitchingState(){
        return mWorkspace.isSwitchingState();
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    public void setLauncherOverlay(LauncherOverlay overlay){
        mWorkspace.setLauncherOverlay(overlay);
    }


    public void removeExtraEmptyScreenDelayed(Runnable onComplete,int delay){
        mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                delay, false);
    }

    public CellLayout getCellLayoutById(long id){

       return mWorkspace.getScreenWithId(id);
    }

    /**
     * Check to see if a given screen id exists. If not, create it at the end, return the new id.
     *
     * @param screenId the screen id to check
     * @return the new screen, or screenId if it exists
     */
    public long ensurePendingDropLayoutExists(long screenId) {
        CellLayout dropLayout = getCellLayoutById(screenId);
        if (dropLayout == null) {
            // it's possible that the add screen was removed because it was
            // empty and a re-bind occurred
            mWorkspace.addExtraEmptyScreen();
            return mWorkspace.commitExtraEmptyScreen();
        } else {
            return screenId;
        }
    }

    public void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, final DragView dragView,
                                  final Runnable onCompleteRunnable, int animationType, final View finalView
                                  ){
        mWorkspace.animateWidgetDrop(info, cellLayout,
                dragView, onCompleteRunnable,
                animationType, finalView, true);
    }

    public void onOverlayScrollChanged(float scroll){
        if (mWorkspace != null) {
            mWorkspace.onOverlayScrollChanged(scroll);
        }
    }

    public ViewGroup getViewGroup(){
        return  (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
    }


    public  boolean createUserFolderIfNecessary(View newView, long container, CellLayout target,
                                                int[] targetCell, float distance, boolean external, DragView dragView){
       return mWorkspace.createUserFolderIfNecessary(newView, container,target, targetCell, 0,
                true, null);
    }

    public boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell,
                      float distance, DropTarget.DragObject d, boolean external){
      return   mWorkspace.addToExistingFolderIfNecessary(newView, target, targetCell, 0, d,
                true);
    }

    public void onNoCellFound(View view){
        mWorkspace.onNoCellFound(view);
    }


    public void addInScreen(View child, ItemInfo info){
        mWorkspace.addInScreen(child, info);
    }


    public void requestLayout(){
        mWorkspace.requestLayout();
    }



    public FolderIcon getFolderIcon(long folderIconId){
        return (FolderIcon) mWorkspace.getFirstMatch(new Workspace.ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View view) {
                return info != null && info.id == folderIconId;
            }
        });
    }

    public void updateIconBadges(final Set<PackageUserKey> updatedBadges){
        mWorkspace.updateIconBadges(updatedBadges);
    }


    public void checkMoveToDefaultScreen(){
        if (!mWorkspace.isTouchActive()) {
            mWorkspace.post(mWorkspace::moveToDefaultScreen);
        }
    }

    public void restoreInstanceStateForChild(int child){
        mWorkspace.restoreInstanceStateForChild(child);
    }

   public void removeListen(){
       mWorkspace.removeFolderListeners();
    }

    public CellLayout getParentCellLayoutForView(View view){
       return mWorkspace.getParentCellLayoutForView(view);
    }

    public View getHomescreenIconByItemId(long id){
        return mWorkspace.getHomescreenIconByItemId(id);
    }

    public void removeWorkspaceItem(View view){
      mWorkspace.removeWorkspaceItem(view);
    }

    public void showOutlinesTemporarily(){
        mWorkspace.showOutlinesTemporarily();
    }

    public CellLayout getScreenWithId(long screenId){
      return mWorkspace.getScreenWithId(screenId);
    }

    public void clearScreens(){
        mWorkspace.clearDropTargets();
        mWorkspace.removeAllWorkspaceScreens();
    }


    public void addEmpScreen(){
        mWorkspace.addExtraEmptyScreen();
    }

    public void removeEmpScreen(){
        mWorkspace.removeExtraEmptyScreen(false, false);
    }

    public void unlockWallpaperFromDefaultPageOnNextLayout(){
        mWorkspace.unlockWallpaperFromDefaultPageOnNextLayout();
    }

    public void insertNewWorkspaceScreenBeforeEmptyScreen(long screenId){
        mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
    }


    private long getScreenIdForPageIndex(int page){
      return   mWorkspace.getScreenIdForPageIndex(page);
    }

   private long getScreenIdForNextPageIndex(){
        return getScreenIdForPageIndex(getNextPage());
    }

    private int getPageIndexForScreenId(long screenId){
        return mWorkspace.getPageIndexForScreenId(screenId);
    }

    public void bindItem(long newItemsScreenId, AnimatorSet anim, Collection<Animator> bounceAnims,  BaseDraggingActivity activity){
        if(newItemsScreenId<=-1){
            return;
        }
        long currentScreenId =getScreenIdForNextPageIndex();
        final int newScreenIndex =getPageIndexForScreenId(newItemsScreenId);
        final Runnable startBounceAnimRunnable = new Runnable() {
            public void run() {
                anim.playTogether(bounceAnims);
                anim.start();
            }
        };
        if (newItemsScreenId != currentScreenId) {
            // We post the animation slightly delayed to prevent slowdowns
            // when we are loading right after we return to launcher.
            mWorkspace.postDelayed(new Runnable() {
                public void run() {
                    if (mWorkspace != null) {
                        AbstractFloatingView.closeAllOpenViews(activity, false);
                        mWorkspace.snapToPage(newScreenIndex);
                        mWorkspace.postDelayed(startBounceAnimRunnable,
                                LauncherConfig.NEW_APPS_ANIMATION_DELAY);
                    }
                }
            }, LauncherConfig.NEW_APPS_PAGE_MOVE_DELAY);
        } else {
            mWorkspace.postDelayed(startBounceAnimRunnable, LauncherConfig.NEW_APPS_ANIMATION_DELAY);
        }
    }

    public LauncherAppWidgetHostView  getLauncherAppWidgetHostView(int appWidgetId){
        return mWorkspace.getWidgetForAppWidgetId(appWidgetId);
    }

    public void restoreInstanceStateForRemainingPages(){
        mWorkspace.restoreInstanceStateForRemainingPages();
    }



}
