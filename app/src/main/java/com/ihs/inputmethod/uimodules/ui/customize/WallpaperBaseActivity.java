package com.ihs.inputmethod.uimodules.ui.customize;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.ihs.app.framework.activity.HSAppCompatActivity;
import com.ihs.commons.utils.HSLog;
import com.ihs.inputmethod.ads.fullscreen.KeyboardFullScreenAd;
import com.ihs.inputmethod.feature.common.ConcurrentUtils;
import com.ihs.inputmethod.feature.common.ViewUtils;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.customize.service.CustomizeService;
import com.ihs.inputmethod.uimodules.ui.customize.service.ICustomizeService;
import com.ihs.inputmethod.uimodules.ui.customize.util.WallpaperManagerProxy;
import com.ihs.inputmethod.uimodules.ui.customize.view.DrawView;
import com.ihs.inputmethod.uimodules.ui.customize.view.EditWallpaperHintDrawer;
import com.ihs.inputmethod.uimodules.ui.customize.view.ProgressDialog;
import com.ihs.keyboardutils.utils.ToastUtils;
import com.kc.commons.utils.KCCommonUtils;

import java.io.IOException;

import hugo.weaving.DebugLog;

public abstract class WallpaperBaseActivity extends HSAppCompatActivity implements ServiceConnection {

    private static final String TAG = WallpaperBaseActivity.class.getSimpleName();
    protected ICustomizeService mService = null;
    protected ProgressDialog mDialog;
    protected WallpaperInfo mCurrentWallpaper;
    private boolean mIsSettingWallpaper = false;
    // --Commented out by Inspection (18/1/11 下午2:41):private boolean mIsSettingKeyTheme = false;
    // --Commented out by Inspection (18/1/11 下午2:41):private static final String FROM_LOCAL_GALLERY = "Local";
    // --Commented out by Inspection (18/1/11 下午2:41):private static final String FROM_LOCAL_ONLINE = "Online";
    // --Commented out by Inspection (18/1/11 下午2:41):private static final String WALLPAPER_MODE_FIXED = "Fixed";
    // --Commented out by Inspection (18/1/11 下午2:41):private static final String WALLPAPER_MODE_SCROLLABLE = "Scrollable";

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ICustomizeService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mService == null) {
            Intent intent = new Intent(this, CustomizeService.class);
            intent.setAction(CustomizeService.class.getName());
            bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        if (mDialog != null) {
            KCCommonUtils.dismissDialog(mDialog);
        }
    }

// --Commented out by Inspection START (18/1/11 下午2:41):
//    protected void startGuideAnimation(Activity activity, int resId, final int width) {
//        final DrawView drawView = ViewUtils.findViewById(activity, resId);
//        if (drawView == null) {
//            return;
//        }
//        drawView.post(new Runnable() {
//            @Override
//            public void run() {
//                EditWallpaperHintDrawer drawer = new EditWallpaperHintDrawer(drawView, width);
//                drawView.setDrawer(drawer);
//                drawer.start();
//                drawView.setVisibility(View.VISIBLE);
//            }
//        });
//    }
// --Commented out by Inspection STOP (18/1/11 下午2:41)

    // --Commented out by Inspection (18/1/11 下午2:41):protected abstract void refreshButtonState();

    protected boolean isSettingWallpaper() {
        return mIsSettingWallpaper;
    }

// --Commented out by Inspection START (18/1/11 下午2:41):
//    protected void applyWallpaper(final boolean isScroll) {
//        applyWallpaper(isScroll, true);
//    }
// --Commented out by Inspection STOP (18/1/11 下午2:41)

    protected void applyWallpaper(final boolean isScroll, final boolean logEvent) {
        mIsSettingWallpaper = true;
        mCurrentWallpaper = getCurrentWallpaper();
        if (logEvent) {
            if (mCurrentWallpaper.getCategory() != null) {
                HSLog.i(mCurrentWallpaper.getCategory().categoryName + "");
            }
        }

        final Handler mHandler = new Handler();
        mDialog = ProgressDialog.createDialog(this, getString(R.string.wallpaper_setting_progress_dialog_text));
        mDialog.setCancelable(false);
        KCCommonUtils.showDialog(mDialog);
        final Bitmap wallpaper = tryGetWallpaperToSet();
        if (wallpaper != null) {
            ConcurrentUtils.postOnThreadPoolExecutor(new Runnable() {
                @Override
                public void run() {
                    final Close close = new Close();
                    final boolean success = setWallpaper(close, wallpaper, isScroll, logEvent);
                    int delays = wallpaper.getWidth() * wallpaper.getHeight() / 10000;
                    if (delays > 1000) {
                        delays = 1000;
                    }
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    applyWallPaperFinish(close);
                                }
                            });
                            if (success) {
                                KCCommonUtils.dismissDialog(mDialog);
                                ToastUtils.showToast(R.string.interstitial_ad_title_after_try_keyboard);
                                KeyboardFullScreenAd.showSessionOneTimeAd("afterApply");
                                finish();
                            } else {
                                KCCommonUtils.dismissDialog(mDialog);
                                ToastUtils.showToast(R.string.wallpaper_toast_set_failed);
                            }
                        }
                    }, delays);
                }
            });
        } else {
            KCCommonUtils.dismissDialog(mDialog);
            ToastUtils.showToast(R.string.wallpaper_toast_set_failed);
            finish();
        }
    }

    private void applyWallPaperFinish(Close close) {
        if (close.isReady()) {
            close.run();
        } else {
            close.setReady(true);
        }
    }

    @DebugLog
    private boolean setWallpaper(final Close close, Bitmap wallpaper, boolean isScroll, boolean logEvent) {
        if (wallpaper != null) {
            ICustomizeService service = mService;
            if (service != null) {
                try {
                    service.preChangeWallpaperFromLauncher();
//                    WallpaperMgr.getInstance().cleanCurrentWallpaper();
                    WallpaperManagerProxy.getInstance().setSystemBitmap(this, wallpaper);
                    if (logEvent) {
                    }
                } catch (IOException | RemoteException e) {
                    if (logEvent) {
                    }
                    e.printStackTrace();
                }
                mCurrentWallpaper = getCurrentWallpaper();
//                WallpaperMgr.getInstance().saveCurrentWallpaper(mCurrentWallpaper, close);
//                HSGlobalNotificationCenter.sendNotificationOnMainThread(WallpaperMgr.NOTIFICATION_WALLPAPER_SET);
                return true;
            }
        }
        return false;
    }

// --Commented out by Inspection START (18/1/11 下午2:41):
//    private void saveWallPaperToFile(Bitmap wallpaper) {
//        // store thumb for Gallery only
//
////        String thumbPath = mCurrentWallpaper.getPath() + WallpaperMgr.Files.LOCAL_WALLPAPER_THUMB_SUFFIX;
////        File file = new File(thumbPath);
////        if (file.exists()) {
////            boolean deleted = file.delete();
////            HSLog.d(TAG, "Delete file " + file + ": " + deleted);
////        }
////        Utils.saveBitmapToFile(wallpaper, file, 40);
////        HSGlobalNotificationCenter.sendNotificationOnMainThread(WallpaperMgr.NOTIFICATION_WALLPAPER_GALLERY_SAVED);
//    }
// --Commented out by Inspection STOP (18/1/11 下午2:41)

    protected abstract Bitmap tryGetWallpaperToSet();

    protected abstract WallpaperInfo getCurrentWallpaper();

    public class Close implements Runnable {
        private boolean mReady = false;

        @Override
        public void run() {
            ICustomizeService service = mService;
            if (service != null) {
                try {
                    service.notifyWallpaperSetEvent();
                    HSLog.w(TAG, "mService.notifyWallpaperSetEvent");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isReady() {
            return mReady;
        }

        public void setReady(boolean reached) {
            mReady = reached;
        }

    }
}
