package com.android.launcher3.manager.view;

import com.android.launcher3.DropTargetBar;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragController;

/**
 * @author lixiaogege!
 * @description: one day day ,no zuo no die !
 * @description: every body hai hai hai !
 * @date :2022/5/16 0016 15:32
 */
public class DropBarManager {


    private DropTargetBar mDropTargetBar;
    public DropBarManager(DropTargetBar dropTargetBar){
        this.mDropTargetBar=dropTargetBar;
    }
    public void init(Workspace workspace, DragController mDragController){
        mDragController.setMoveTarget(workspace);
        mDropTargetBar.setup(mDragController);
    }

    public DropTargetBar getmDropTargetBar() {
        return mDropTargetBar;
    }
}
