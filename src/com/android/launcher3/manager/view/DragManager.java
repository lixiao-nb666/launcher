package com.android.launcher3.manager.view;

import com.android.launcher3.DropTargetBar;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherConfig;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.util.Thunk;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/16 0016 14:23
 */
public class DragManager {
    @Thunk
    DragLayer mDragLayer;
    private DragController mDragController;

    public DragManager(Launcher launcher){
        mDragController = new DragController(launcher);
    }

    public void setDragLayer(DragLayer dragLayer){
        this.mDragLayer=dragLayer;
    }



    public void initSet(){
        mDragLayer.getAlphaProperty(DragLayer.ALPHA_INDEX_LAUNCHER_LOAD).setValue(0);
    }


    public MultiValueAlpha.AlphaProperty getProperty(){
        return mDragLayer.getAlphaProperty(DragLayer.ALPHA_INDEX_LAUNCHER_LOAD);
    }

    public  boolean nowIsDragging(){
      return    mDragController.isDragging();
    }

    public void setUp(Workspace workspace){
        mDragLayer.setup(mDragController,workspace);
    }

    public DragController getmDragController() {
        return mDragController;
    }

    public void addListen(Workspace workspace){
        mDragController.addDragListener(workspace);
    }



    public DragLayer getmDragLayer() {
        return mDragLayer;
    }



    public void pause(){
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
    }
}
