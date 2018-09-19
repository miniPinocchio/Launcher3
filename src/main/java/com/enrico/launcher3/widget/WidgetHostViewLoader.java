package com.enrico.launcher3.widget;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.enrico.launcher3.AppWidgetResizeFrame;
import com.enrico.launcher3.DropTarget;
import com.enrico.launcher3.Launcher;
import com.enrico.launcher3.LauncherAppWidgetProviderInfo;
import com.enrico.launcher3.compat.AppWidgetManagerCompat;
import com.enrico.launcher3.dragndrop.DragController;
import com.enrico.launcher3.dragndrop.DragLayer;
import com.enrico.launcher3.dragndrop.DragOptions;
import com.enrico.launcher3.util.Thunk;

public class WidgetHostViewLoader implements DragController.DragListener {

    /* Runnables to handle inflation and binding. */
    @Thunk private Runnable mInflateWidgetRunnable = null;
    private Runnable mBindWidgetRunnable = null;

    // TODO: technically, this class should not have to know the existence of the launcher.
    @Thunk private Launcher mLauncher;
    @Thunk private Handler mHandler;
    @Thunk private final View mView;
    @Thunk private final PendingAddWidgetInfo mInfo;

    // Widget id generated for binding a widget host view or -1 for invalid id. The id is
    // not is use as long as it is stored here and can be deleted safely. Once its used, this value
    // to be set back to -1.
    @Thunk private int mWidgetLoadingId = -1;

    WidgetHostViewLoader(Launcher launcher, View view) {
        mLauncher = launcher;
        mHandler = new Handler();
        mView = view;
        mInfo = (PendingAddWidgetInfo) view.getTag();
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        preloadWidget();
    }

    @Override
    public void onDragEnd() {

        // Cleanup up preloading state.
        mLauncher.getDragController().removeDragListener(this);

        mHandler.removeCallbacks(mBindWidgetRunnable);
        mHandler.removeCallbacks(mInflateWidgetRunnable);

        // Cleanup widget id
        if (mWidgetLoadingId != -1) {
            mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
            mWidgetLoadingId = -1;
        }

        // The widget was inflated and added to the DragLayer -- remove it.
        if (mInfo.boundWidget != null) {
            mLauncher.getDragLayer().removeView(mInfo.boundWidget);
            mLauncher.getAppWidgetHost().deleteAppWidgetId(mInfo.boundWidget.getAppWidgetId());
            mInfo.boundWidget = null;
        }
    }

    /**
     * Start preloading the widget.
     */
    private boolean preloadWidget() {
        final LauncherAppWidgetProviderInfo pInfo = mInfo.info;

        if (pInfo.isCustomWidget) {
            return false;
        }
        final Bundle options = getDefaultOptionsForWidget(mLauncher, mInfo);

        // If there is a configuration activity, do not follow thru bound and inflate.
        if (mInfo.getHandler().needsConfigure()) {
            mInfo.bindOptions = options;
            return false;
        }

        mBindWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                mWidgetLoadingId = mLauncher.getAppWidgetHost().allocateAppWidgetId();
                if(AppWidgetManagerCompat.getInstance(mLauncher).bindAppWidgetIdIfAllowed(
                        mWidgetLoadingId, pInfo, options)) {

                    // Widget id bound. Inflate the widget.
                    mHandler.post(mInflateWidgetRunnable);
                }
            }
        };

        mInflateWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                if (mWidgetLoadingId == -1) {
                    return;
                }
                AppWidgetHostView hostView = mLauncher.getAppWidgetHost().createView(
                        (Context) mLauncher, mWidgetLoadingId, pInfo);
                mInfo.boundWidget = hostView;

                // We used up the widget Id in binding the above view.
                mWidgetLoadingId = -1;

                hostView.setVisibility(View.INVISIBLE);
                int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(mInfo, false, true);
                // We want the first widget layout to be the correct size. This will be important
                // for width size reporting to the AppWidgetManager.
                DragLayer.LayoutParams lp = new DragLayer.LayoutParams(unScaledSize[0],
                        unScaledSize[1]);
                lp.x = lp.y = 0;
                lp.customPosition = true;
                hostView.setLayoutParams(lp);
                mLauncher.getDragLayer().addView(hostView);
                mView.setTag(mInfo);
            }
        };

        mHandler.post(mBindWidgetRunnable);
        return true;
    }

    public static Bundle getDefaultOptionsForWidget(Context context, PendingAddWidgetInfo info) {
        Rect rect = new Rect();
        AppWidgetResizeFrame.getWidgetSizeRanges(context, info.spanX, info.spanY, rect);
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context,
                info.componentName, null);

        float density = context.getResources().getDisplayMetrics().density;
        int xPaddingDips = (int) ((padding.left + padding.right) / density);
        int yPaddingDips = (int) ((padding.top + padding.bottom) / density);

        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                rect.left - xPaddingDips);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                rect.top - yPaddingDips);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                rect.right - xPaddingDips);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                rect.bottom - yPaddingDips);
        return options;
    }
}
