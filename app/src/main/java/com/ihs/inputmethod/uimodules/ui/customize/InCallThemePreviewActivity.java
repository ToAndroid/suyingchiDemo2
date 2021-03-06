package com.ihs.inputmethod.uimodules.ui.customize;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.acb.call.CPSettings;
import com.acb.call.MediaDownloadManager;
import com.acb.call.activity.HSAppCompatActivity;
import com.acb.call.constant.CPConst;
import com.acb.call.receiver.IncomingCallReceiver;
import com.acb.call.themes.Type;
import com.acb.call.views.InCallActionView;
import com.acb.call.views.ThemePreviewWindow;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.kc.utils.KCAnalytics;
import com.ihs.chargingscreen.utils.DisplayUtils;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.inputmethod.ads.fullscreen.KeyboardFullScreenAd;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.keyboardutils.utils.ToastUtils;

import static com.ihs.inputmethod.uimodules.ui.settings.activities.SettingsActivity.CALL_ASSISTANT_HAS_SWITCHED_ON;

public class InCallThemePreviewActivity extends HSAppCompatActivity {


    private static final int OVERLAY_REQUEST_CODE = 999;

    private ThemePreviewWindow mPreviewView;
    private InCallActionView mCallView;
    private Toolbar mToolbar;
    private TextView mSetCallThemeButton;
    private MediaDownloadManager mGifDownloader = new MediaDownloadManager();
    private boolean mIsDestroyed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_activity_theme_preview);


        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        TextView titleView = new TextView(this);
        titleView.setText(R.string.acb_screen_flash_name);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(20);
        Toolbar.LayoutParams toolbarTitleParams = new Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.START);
        boolean showToolBarBack = HSConfig.optBoolean(true, "Application", "LibColorPhone", "ShowToolBarBack");
        if (showToolBarBack) {
            toolbarTitleParams.setMargins(DisplayUtils.dip2px(20), 0, 0, 0);
        } else {
            toolbarTitleParams.leftMargin = DisplayUtils.dip2px(30);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                toolbarTitleParams.setMarginStart(DisplayUtils.dip2px(30));
            }
        }

        titleView.setLayoutParams(toolbarTitleParams);
        mToolbar.addView(titleView);
        mToolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        mToolbar.setBackgroundResource(R.drawable.acb_phone_theme_tool_bar_bg);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(showToolBarBack);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mPreviewView = findViewById(R.id.flash_view);
        mPreviewView.setPreviewType(ThemePreviewWindow.PreviewType.PREVIEW);

        mCallView = findViewById(R.id.in_call_view);
        mCallView.enableFullScreen(false);
        mSetCallThemeButton = findViewById(R.id.set_incoming_call_theme);
        initThemesView();
        requestPermissionsIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPreviewView != null) {
            mPreviewView.startAnimations();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPreviewView != null) {
            mPreviewView.stopAnimations();
        }
        if (mCallView != null) {
            mCallView.stopAnimations();
        }
    }

    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        mGifDownloader.cancelAllDownload();
        super.onDestroy();
    }

    private void requestPermissionsIfNeeded() {
        if (!IncomingCallReceiver.IncomingCallListener.getInstance().getPermission().canDrawOverlays()) {
            IncomingCallReceiver.IncomingCallListener.getInstance().getPermission().requestDrawOverlays(this, OVERLAY_REQUEST_CODE);
        }
    }

    private void initThemesView() {
        final Type themeType = (Type) getIntent().getSerializableExtra("CallThemeType");


        initThemeAnimation(themeType);

        mSetCallThemeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KCAnalytics.logEvent("app_callflash_applied", "themeName", themeType.getName());
                enableCallFlash();
                HSPreferenceHelper.getDefault().putInt(CPConst.PREFS_SCREEN_FLASH_THEME_ID, themeType.getValue());
                ToastUtils.showToast(R.string.interstitial_ad_title_after_try_keyboard);
                KeyboardFullScreenAd.showSessionOneTimeAd("afterApply");
            }
        });
    }

    private void enableCallFlash() {
        HSPreferenceHelper.getDefault().putBoolean(CALL_ASSISTANT_HAS_SWITCHED_ON, true);
        CPSettings.setCallAssistantModuleEnabled(true);
        CPSettings.setScreenFlashModuleEnabled(true,true);
    }

    private void initThemeAnimation(final Type themeType) {
        if (themeType.isGif()) {
            prepareAndShowGif(themeType);
        } else {
            findViewById(R.id.theme_progress_bar).setVisibility(View.GONE);
            findViewById(R.id.theme_progress_txt_holder).setVisibility(View.GONE);
            mSetCallThemeButton.setVisibility(View.VISIBLE);
            mPreviewView.playAnimation(themeType);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareAndShowGif(final Type type) {
        if (mGifDownloader.isDownloaded(type.getFileName())) {
            mPreviewView.playAnimation(type);
            mSetCallThemeButton.setVisibility(View.VISIBLE);
            mCallView.setAutoRun(true);
        } else {
            mCallView.setAutoRun(false);
            mPreviewView.updateThemeLayout(type);
//            ImageLoader.getInstance().displayImage(type.getPreviewImage(), (ImageView) mPreviewView.findViewById(R.id.animation_view));
            RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA);
            Glide.with(this).asBitmap().apply(requestOptions)
                    .load(type.getPreviewImage())
                    .into((ImageView) mPreviewView.findViewById(R.id.animation_view));
            if (!mGifDownloader.isDownloading(type.getFileName())) {
                downloadMedia(type);
            } else {
                findViewById(R.id.theme_progress_bar).setVisibility(View.VISIBLE);
                findViewById(R.id.theme_progress_txt_holder).setVisibility(View.VISIBLE);
            }
        }
    }

    private void downloadMedia(final Type type) {
        final ProgressBar horPro = findViewById(R.id.theme_progress_bar);
        final LinearLayout proHolder = findViewById(R.id.theme_progress_txt_holder);

        horPro.setVisibility(View.VISIBLE);
        proHolder.setVisibility(View.VISIBLE);
        ((TextView) proHolder.findViewById(R.id.theme_progress_txt)).setText("0 %");
        mGifDownloader.downloadMedia(type.getSuggestMediaUrl(), type.getFileName(),
                new MediaDownloadManager.SimpleDownloadCallback() {
                    @Override
                    public void onUpdate(final long progress) {

                        horPro.setProgress((int) progress);
                        ((TextView) proHolder.findViewById(R.id.theme_progress_txt)).setText("" + progress + " %");
                    }

                    @Override
                    public void onSuccess(MediaDownloadManager.MediaDownLoadTask task) {
                        if (mIsDestroyed) {
                            return;
                        }

                        horPro.setVisibility(View.GONE);
                        proHolder.setVisibility(View.GONE);
                        horPro.setProgress(0);
                        ((TextView) proHolder.findViewById(R.id.theme_progress_txt)).setText("0 %");
                        mCallView.doAnimation();
                        mPreviewView.playAnimation(type);
                        mSetCallThemeButton.setVisibility(View.VISIBLE);
                    }



                    @Override
                    public void onFail(MediaDownloadManager.MediaDownLoadTask task, String msg) {
                        if (mIsDestroyed) {
                            return;
                        }

                        proHolder.setVisibility(View.GONE);
                        horPro.setVisibility(View.GONE);
                        Toast.makeText(InCallThemePreviewActivity.this, R.string.acb_phone_theme_gif_download_failed_toast, Toast.LENGTH_LONG).show();
                    }
                });
    }
}