/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.enrico.launcher3;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Looper;

import com.enrico.launcher3.compat.LauncherAppsCompat;
import com.enrico.launcher3.compat.PackageInstallerCompat;
import com.enrico.launcher3.compat.UserManagerCompat;
import com.enrico.launcher3.dynamicui.ExtractionUtils;
import com.enrico.launcher3.icons.IconCache;
import com.enrico.launcher3.util.ConfigMonitor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class LauncherAppState {

    // We do not need any synchronization for this variable as its only written on UI thread.
    private static LauncherAppState INSTANCE;

    private final Context mContext;
    private final LauncherModel mModel;
    private final IconCache mIconCache;
    private final WidgetPreviewLoader mWidgetCache;
    private final InvariantDeviceProfile mInvariantDeviceProfile;


    public static LauncherAppState getInstance(final Context context) {
        if (INSTANCE == null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                INSTANCE = new LauncherAppState(context.getApplicationContext());
            } else {
                try {
                    return new MainThreadExecutor().submit(new Callable<LauncherAppState>() {
                        @Override
                        public LauncherAppState call() throws Exception {
                            return LauncherAppState.getInstance(context);
                        }
                    }).get();
                } catch (InterruptedException|ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return INSTANCE;
    }

    public static LauncherAppState getInstanceNoCreate() {
        return INSTANCE;
    }

    public Context getContext() {
        return mContext;
    }

    private LauncherAppState(Context context) {
        if (getLocalProvider(context) == null) {
            throw new RuntimeException(
                    "Initializing LauncherAppState in the absence of LauncherProvider");
        }
        mContext = context;

        mInvariantDeviceProfile = new InvariantDeviceProfile(mContext);
        mIconCache = new IconCache(mContext, mInvariantDeviceProfile);
        mWidgetCache = new WidgetPreviewLoader(mContext, mIconCache);

        AppFilter appFilter = new StringSetAppFilter();

        mModel = new LauncherModel(this, mIconCache,appFilter);

        LauncherAppsCompat.getInstance(mContext).addOnAppsChangedCallback(mModel);

        // Register intent receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        // For handling managed profiles
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED);
        // For extracting colors from the wallpaper
        if (AndroidVersion.isAtLeastNougat) {
            // TODO: add a broadcast entry to the manifest for pre-N.
            filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        }

        mContext.registerReceiver(mModel, filter);
        UserManagerCompat.getInstance(mContext).enableAndResetCache();
        new ConfigMonitor(mContext).register();

        ExtractionUtils.startColorExtractionServiceIfNecessary(mContext);
    }

    /**
     * Call from Application.onTerminate(), which is not guaranteed to ever be called.
     */
    public void onTerminate() {
        mContext.unregisterReceiver(mModel);
        final LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(mContext);
        launcherApps.removeOnAppsChangedCallback(mModel);
        PackageInstallerCompat.getInstance(mContext).onStop();
    }

    LauncherModel setLauncher(Launcher launcher) {
        getLocalProvider(mContext).setLauncherProviderChangeListener(launcher);
        mModel.initialize(launcher);
        return mModel;
    }

    public IconCache getIconCache() {
        return mIconCache;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public WidgetPreviewLoader getWidgetCache() {
        return mWidgetCache;
    }

    public InvariantDeviceProfile getInvariantDeviceProfile() {
        return mInvariantDeviceProfile;
    }

    /**
     * Shorthand for {@link #getInvariantDeviceProfile()}
     */
    public static InvariantDeviceProfile getIDP(Context context) {
        return LauncherAppState.getInstance(context).getInvariantDeviceProfile();
    }

    private static LauncherProvider getLocalProvider(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try (ContentProviderClient cl = context.getContentResolver()
                    .acquireContentProviderClient(LauncherProvider.AUTHORITY)) {
                return (LauncherProvider) cl.getLocalContentProvider();
            }
        } else {
            ContentProviderClient cl = context.getContentResolver()
                    .acquireContentProviderClient(LauncherProvider.AUTHORITY);
            LauncherProvider ret = null;
            if (cl != null) {
                ret = (LauncherProvider) cl.getLocalContentProvider();
                cl.release();
            }
            return ret;
        }
    }
}
