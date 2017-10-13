package com.ihs.inputmethod.feature.apkupdate;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ihs.app.analytics.HSAnalytics;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.utils.HSMarketUtils;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.utils.HSLog;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.inputmethod.api.utils.HSFileUtils;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.utils.RippleDrawableUtils;
import com.ihs.inputmethod.utils.CommonUtils;
import com.ihs.keyboardutils.alerts.HSAlertDialog;

import java.io.File;
import java.util.Locale;

public class ApkUtils {

    private static final String PREF_KEY_UPDATE_APK_VERSION_CODE = "update_apk_version_code";
    private static final String PREF_KEY_UPDATE_ALERT_LAST_SHOWN_TIME = "update_alert_last_shown_time";
    private static final String PREF_KEY_RATE_BUTTON_CLICKED = "perf_rate_button_clicked";
    private static final String PREF_KEY_SHARED_KEYBOARD_ON_INSTAGRAM = "perf_shared_keyboard_on_instagram";
    private static final long UPDATE_ALERT_SHOW_INTERVAL_IN_MILLIS = 24 * 60 * 60 * 1000;
    private static final String PREF_APKUTILS_FILE_NAME = "pref_file_apkutils";

    public static void startInstall(Context context, Uri uri) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(install);
    }

    public static PackageInfo getApkInfo(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        return pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
    }

    public static File getDefaultLocalFile(Context context) {
        try {
            File file = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            return new File(file, UpdateConfig.getDefault().getLocalFileName());
        } catch (Exception e) {
            // noting
        }
        return new File(context.getFilesDir(), UpdateConfig.getDefault().getLocalFileName());
    }

    public static boolean isLocalApkReady(String apkPath, Context context) {
        if (apkPath == null)
            return false;
        PackageInfo apkInfo = getApkInfo(context, apkPath);
        if (apkInfo == null) {
            return false;
        }
        String localPackage = context.getPackageName();
        if (apkInfo.packageName.equals(localPackage)) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(localPackage, 0);
                // 1 Apk file is newer than current.
                // 2 Apk file has same version code with config load from server.
                if (apkInfo.versionCode > packageInfo.versionCode && apkInfo.versionCode >= getLatestVersionCode()) {
                    return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean checkAndShowUpdateAlert() {
        return checkAndShowUpdateAlert(false);
    }

    public static boolean checkAndShowUpdateAlert(final boolean force) {
        if (shouldUpdate() && isUpdateEnabled()) {
            if (force) {
                showUpdateAlert();
            } else if (checkTimeout()) {
                showUpdateAlert();
            }

            return true;
        }

        return false;
    }

    private static boolean checkTimeout() {
        final long lastShowTime = getUpdateAlertLastShownTime();
        if (System.currentTimeMillis() - lastShowTime >= UPDATE_ALERT_SHOW_INTERVAL_IN_MILLIS) {
            return true;
        }

        return false;
    }

    /**
     * If current App not from Market, download directly.
     */
    public static void doUpdate() {
        if (HSMarketUtils.isMarketInstalled("Google")) {
            HSMarketUtils.browseAPP("Google", HSApplication.getContext().getPackageName());
        } else {
            long downloadId;

            if ((downloadId = ApkDownloadManager.getInstance().checkDownload(UpdateConfig.getDefault())) > 0) {
                HSLog.d("Start to download update apk with downloadId: " + downloadId);
            } else {
                HSLog.e("Can't to download update apk with error code: " + downloadId);
            }
        }
    }

    /**
     * Indicates whether DownloadManager is enabled.
     */
    static boolean isDownloadManagerReady(Context context) {
        try {
            int state = context.getPackageManager().getApplicationEnabledSetting("com.android.providers.downloads");

            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Get download file status by id.
     *
     * @param downloadId an ID for the download, unique across the system.
     *                   This ID is used to make future calls related to this download.
     * @return int
     * @see DownloadManager#STATUS_PENDING
     * @see DownloadManager#STATUS_PAUSED
     * @see DownloadManager#STATUS_RUNNING
     * @see DownloadManager#STATUS_SUCCESSFUL
     * @see DownloadManager#STATUS_FAILED
     */
    public static int getDownloadStatus(DownloadManager manager, long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = manager.query(query);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));

                }
            } finally {
                c.close();
            }
        }
        return -1;
    }

    public static boolean shouldUpdate() {
        return shouldCheckUpdateNow() && isNewVersionAvailable();
    }

    private static boolean shouldCheckUpdateNow() {
        return CommonUtils.isNetworkAvailable(-1) // 有网
                && (HSMarketUtils.isMarketInstalled("Google") //安装了Google Play
                || !TextUtils.isEmpty(UpdateConfig.getDefault().getDownLoadUrl()));
    }

    public static boolean isGooglePlayInstalled() {
        return HSMarketUtils.isMarketInstalled("Google");
    }

    public static boolean isUpdateEnabled() {
        return HSApplication.getContext().getResources().getBoolean(R.bool.apk_update_enabled);
    }

    public static void startUpdate(final Context context) {
        File file = ApkUtils.getDefaultLocalFile(context);
        if (ApkUtils.isLocalApkReady(file.getPath(), context)) {
            startInstall(context, Uri.fromFile(file));
            return;
        }
    }

    public static boolean isNewVersionAvailable() {
        String localPackage = HSApplication.getContext().getPackageName();
        int currentVersionCode = 0;
        int latestVersionCode = 0;

        try {
            PackageInfo packageInfo = HSApplication.getContext().getPackageManager().getPackageInfo(localPackage, 0);
            currentVersionCode = packageInfo.versionCode;
            latestVersionCode = getLatestVersionCode();

            if (currentVersionCode < latestVersionCode) {
                HSLog.d("Has update, current version code: " + currentVersionCode + ", latestVersionCode: " + latestVersionCode);
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        HSLog.d("No update, current version code: " + currentVersionCode + ", latestVersionCode: " + latestVersionCode);
        return false;
    }

    public static int getLatestVersionCode() {
        final int latestVersionCode = HSConfig.optInteger(0, "Application", "Update", "LatestVersionCode");
        HSLog.d("latestVersionCode: " + latestVersionCode);
        return latestVersionCode;
    }

    public static int getUpdateApkVersionCode() {
        return HSPreferenceHelper.getDefault().getInt(PREF_KEY_UPDATE_APK_VERSION_CODE, 0);
    }

    public static void saveUpdateApkVersionCode() {
        HSPreferenceHelper.getDefault().putInt(PREF_KEY_UPDATE_APK_VERSION_CODE, getLatestVersionCode());
    }

    private static long getUpdateAlertLastShownTime() {
        return HSPreferenceHelper.getDefault().getLong(PREF_KEY_UPDATE_ALERT_LAST_SHOWN_TIME, 0);
    }

    public static void saveUpdateAlertLastShownTime() {
        HSPreferenceHelper.getDefault().putLong(PREF_KEY_UPDATE_ALERT_LAST_SHOWN_TIME, System.currentTimeMillis());
    }

    private static void setRateButtonClicked() {
        HSPreferenceHelper.create(HSApplication.getContext(), PREF_APKUTILS_FILE_NAME).putBoolean(PREF_KEY_RATE_BUTTON_CLICKED, true);
    }

    public static boolean isRateButtonClicked() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_APKUTILS_FILE_NAME).getBoolean(PREF_KEY_RATE_BUTTON_CLICKED, false);
    }

    public static boolean isSharedKeyboardOnInstagramBefore() {
        return HSPreferenceHelper.create(HSApplication.getContext(), PREF_APKUTILS_FILE_NAME).getBoolean(PREF_KEY_SHARED_KEYBOARD_ON_INSTAGRAM, false);
    }

    public static boolean isInstagramInstalled() {
        return isInstalled("com.instagram.android");
    }

    public static boolean isInstalled(String packageName) {
        try {
            PackageInfo packageInfo = HSApplication.getContext().getPackageManager().getPackageInfo(packageName.trim(),
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            if (packageInfo != null) {
                // 说明某个应用使用了该包名
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private static String getShareFileDir() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory() + File.separator + HSApplication.getContext().getPackageName() + File.separator + "shareApp";
        }
        Toast.makeText(HSApplication.getContext(), HSApplication.getContext().getString(R.string.sd_card_unavailable_tip), Toast.LENGTH_SHORT).show();
        return null;
    }

    private static String getInstagramShareFile() {
        String shareFileDirPath = getShareFileDir();
        if (shareFileDirPath == null) {
            return null;
        }
        File file = new File(shareFileDirPath, "share_keyboard_to_instagram.jpg");
        if (!file.exists() || file.length() == 0) {
            file.getParentFile().mkdirs();
            HSFileUtils.copyFile(HSApplication.getContext().getResources().openRawResource(R.raw.share_keyboard_to_instagram), file);
        }
        return file.getAbsolutePath();
    }

    @SuppressWarnings("WeakerAccess")
    public static void shareKeyboardToInstagram() {
        Intent intent = HSApplication.getContext().getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (intent != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setPackage("com.instagram.android");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            // Create the URI from the media
            String mediaPath = getInstagramShareFile();
            if (mediaPath == null) {
                return;
            }
            File media = new File(mediaPath);
            Uri uri = Uri.fromFile(media);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("image/*");
            HSApplication.getContext().startActivity(shareIntent);
            HSPreferenceHelper.create(HSApplication.getContext(), PREF_APKUTILS_FILE_NAME).putBoolean(PREF_KEY_SHARED_KEYBOARD_ON_INSTAGRAM, true);
        }
    }

    @SuppressLint("InflateParams")
    public static void showCustomShareAlert(final View.OnClickListener shareButtonClickListener) {
        String preferredLanguageString = Locale.getDefault().getLanguage();
        HSLog.d("showCustomShareAlert preferredLanguageString: " + preferredLanguageString);

        LayoutInflater inflater = (LayoutInflater) HSApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.apk_custom_share_alert, null);
        final AlertDialog alertDialog = HSAlertDialog.build().setView(view).setCancelable(false).create();
        TextView message = (TextView) view.findViewById(R.id.tv_share_message);
        message.setText(HSConfig.optString(HSApplication.getContext().getString(R.string.custom_share_alert_message), "Application", "Update", "ShareAlert", "Message", preferredLanguageString));
        Button positiveBtn = (Button) view.findViewById(R.id.btn_share);
        positiveBtn.setBackgroundDrawable(RippleDrawableUtils.getContainDisableStatusCompatRippleDrawable(
                HSApplication.getContext().getResources().getColor(R.color.custom_share_alert_button_bg),
                HSApplication.getContext().getResources().getColor(R.color.guide_bg_disable_color),
                HSApplication.getContext().getResources().getDimension(R.dimen.apk_update_alert_button_radius)));
        positiveBtn.setText(HSConfig.optString(HSApplication.getContext().getString(R.string.custom_share_alert_button_text), "Application", "Update", "ShareAlert", "ButtonText", preferredLanguageString));
        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CommonUtils.isNetworkAvailable(-1)) {
                    Toast.makeText(HSApplication.getContext(), HSApplication.getContext().getString(R.string.no_network_connection), Toast.LENGTH_SHORT).show();
                    return;
                }

                HSAnalytics.logEvent("customizeTheme_shareToUnlock_clicked");
                if (shareButtonClickListener != null) {
                    shareButtonClickListener.onClick(v);
                }
                shareKeyboardToInstagram();
                alertDialog.dismiss();
            }
        });
        ImageView closeIcon = (ImageView) view.findViewById(R.id.iv_close_image);
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
        HSAnalytics.logEvent("customizeTheme_shareToUnlock_show");
    }

    @SuppressLint("InflateParams")
    public static void showCustomRateAlert(final View.OnClickListener rateButtonClickListener) {
        String preferredLanguageString = Locale.getDefault().getLanguage();
        HSLog.d("showCustomRateAlert preferredLanguageString: " + preferredLanguageString);

        LayoutInflater inflater = (LayoutInflater) HSApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.apk_custom_rate_alert, null, false);
        final AlertDialog alertDialog = HSAlertDialog.build().setView(view).setCancelable(false).create();
        TextView message = (TextView) view.findViewById(R.id.tv_rate_message);
        message.setText(HSConfig.optString(HSApplication.getContext().getString(R.string.custom_rate_alert_message), "Application", "Update", "RateAlert", "Message", preferredLanguageString));
        Button positiveBtn = (Button) view.findViewById(R.id.btn_rate);
        positiveBtn.setBackgroundDrawable(RippleDrawableUtils.getContainDisableStatusCompatRippleDrawable(
                HSApplication.getContext().getResources().getColor(R.color.custom_rate_alert_button_bg),
                HSApplication.getContext().getResources().getColor(R.color.guide_bg_disable_color),
                HSApplication.getContext().getResources().getDimension(R.dimen.apk_update_alert_button_radius)));
        positiveBtn.setText(HSConfig.optString(HSApplication.getContext().getString(R.string.custom_rate_alert_button_text), "Application", "Update", "RateAlert", "ButtonText", preferredLanguageString));
        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CommonUtils.isNetworkAvailable(-1)) {
                    Toast.makeText(HSApplication.getContext(), HSApplication.getContext().getString(R.string.no_network_connection), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (HSMarketUtils.isMarketInstalled("Google")) {
                    HSMarketUtils.browseAPP("Google", HSApplication.getContext().getPackageName());
                    setRateButtonClicked();
                } else {
                    Toast.makeText(HSApplication.getContext(), HSApplication.getContext().getString(R.string.custom_rate_alert_toast_text), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (rateButtonClickListener != null) {
                    rateButtonClickListener.onClick(v);
                }
                alertDialog.dismiss();
            }
        });
        ImageView closeIcon = (ImageView) view.findViewById(R.id.iv_close_image);
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    @SuppressWarnings({"deprecation"})
    public static void showUpdateAlert() {
        saveUpdateAlertLastShownTime();

        LayoutInflater inflater = (LayoutInflater) HSApplication.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.apk_update_alert, null, false);
        final AlertDialog dialog = HSAlertDialog.build(R.style.AppCompactTransparentDialogStyle).setView(view).create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        String preferredLanguageString = Locale.getDefault().getLanguage();
        HSLog.d("preferredLanguageString: " + preferredLanguageString);
        // Set title
        TextView titleView = (TextView) view.findViewById(R.id.txt_dialog_title);
        titleView.setText(HSConfig.optString(HSApplication.getContext().getString(R.string.apk_update_alert_title), "Application", "Update", "UpdateAlert", "Title", preferredLanguageString));
        // Set message
        TextView message = (TextView) view.findViewById(R.id.txt_dialog_message);
        message.setText(HSConfig.optString(HSApplication.getContext().getString(R.string.apk_update_alert_message), "Application", "Update", "UpdateAlert", "Message", preferredLanguageString));

        Button positiveBtn = (Button) view.findViewById(R.id.update_button);
        positiveBtn.setBackgroundDrawable(RippleDrawableUtils.getButtonRippleBackground(R.color.theme_button_text_color));
        positiveBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!CommonUtils.isNetworkAvailable(-1)) {
                            Toast.makeText(HSApplication.getContext(), HSApplication.getContext().getString(R.string.no_network_connection), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        doUpdate();
                        dialog.dismiss();
                    }
                });

        Button negativeBtn = (Button) view.findViewById(R.id.cancel_button);
        negativeBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }
}
