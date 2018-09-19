package com.enrico.launcher3.popup;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import com.enrico.launcher3.AbstractFloatingView;
import com.enrico.launcher3.ItemInfo;
import com.enrico.launcher3.Launcher;
import com.enrico.launcher3.R;
import com.enrico.launcher3.model.WidgetItem;
import com.enrico.launcher3.theme.ThemeUtils;
import com.enrico.launcher3.util.PackageUserKey;
import com.enrico.launcher3.widget.WidgetsBottomSheet;

import java.util.List;

/**
 * Represents a system shortcut for a given app. The shortcut should have a static label and
 * icon, and an onClickListener that depends on the item that the shortcut services.
 *
 * Example system shortcuts, defined as inner classes, include Widgets and AppInfo.
 */
public abstract class SystemShortcut {
    private final int mIconResId;
    private final int mLabelResId;

    private SystemShortcut(int iconResId, int labelResId) {
        mIconResId = iconResId;
        mLabelResId = labelResId;
    }

    public Drawable getIcon(Context context, int colorAttr) {
        Drawable icon = context.getResources().getDrawable(mIconResId, context.getTheme()).mutate();
        icon.setTint(ThemeUtils.getAttrColor(context, colorAttr));
        return icon;
    }

    public String getLabel(Context context) {
        return context.getString(mLabelResId);
    }

    public abstract View.OnClickListener getOnClickListener(final Launcher launcher,
            final ItemInfo itemInfo);

    public static class Widgets extends SystemShortcut {

        public Widgets() {
            super(R.drawable.ic_widget, R.string.widget_button_text);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                final ItemInfo itemInfo) {
            final List<WidgetItem> widgets = launcher.getWidgetsForPackageUser(new PackageUserKey(
                    itemInfo.getTargetComponent().getPackageName(), itemInfo.user));
            if (widgets == null) {
                return null;
            }
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AbstractFloatingView.closeAllOpenViews(launcher);
                    WidgetsBottomSheet widgetsBottomSheet =
                            (WidgetsBottomSheet) launcher.getLayoutInflater().inflate(
                                    R.layout.widgets_bottom_sheet, launcher.getDragLayer(), false);
                    widgetsBottomSheet.populateAndShow(itemInfo);
                }
            };
        }
    }

    public static class AppInfo extends SystemShortcut {
        public AppInfo() {
            super(R.drawable.ic_info_no_shadow, R.string.app_info_drop_target_label);
        }

        @Override
        public View.OnClickListener getOnClickListener(final Launcher launcher,
                final ItemInfo itemInfo) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startInfoDetails(launcher, itemInfo);
                }
            };
        }

        private static void startInfoDetails(Launcher launcher, ItemInfo itemInfo) {
            try {
                //Open the specific App Info page:
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + itemInfo.getTargetComponent().getPackageName()));
                launcher.startActivity(intent);

            } catch ( ActivityNotFoundException e ) {
                e.printStackTrace();
            }
        }
    }
}
