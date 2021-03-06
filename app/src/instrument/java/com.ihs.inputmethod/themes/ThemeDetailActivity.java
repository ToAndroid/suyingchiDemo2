package com.ihs.inputmethod.themes;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.artw.lockscreen.LockerEnableDialog;
import com.artw.lockscreen.LockerSettings;
import com.artw.lockscreen.lockerappguide.LockerAppGuideManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.activity.HSAppCompatActivity;
import com.ihs.app.utils.HSInstallationUtils;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.inputmethod.ads.fullscreen.KeyboardFullScreenAd;
import com.ihs.inputmethod.api.keyboard.HSKeyboardTheme;
import com.ihs.inputmethod.api.theme.HSKeyboardThemeManager;
import com.ihs.inputmethod.api.utils.HSDisplayUtils;
import com.ihs.inputmethod.api.utils.HSResourceUtils;
import com.ihs.inputmethod.api.utils.HSToastUtils;
import com.ihs.inputmethod.constants.AdPlacements;
import com.ihs.inputmethod.home.model.HomeModel;
import com.ihs.inputmethod.theme.ThemeLockerBgUtil;
import com.ihs.inputmethod.theme.download.ApkUtils;
import com.ihs.inputmethod.theme.download.ThemeDownloadManager;
import com.ihs.inputmethod.themes.adapter.ThemeAdapter;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.theme.analytics.ThemeAnalyticsReporter;
import com.ihs.inputmethod.uimodules.ui.theme.ui.adapter.CommonThemeCardAdapter;
import com.ihs.inputmethod.uimodules.ui.theme.ui.model.ThemeHomeModel;
import com.ihs.inputmethod.uimodules.ui.theme.utils.LockedCardActionUtils;
import com.ihs.inputmethod.uimodules.ui.theme.utils.ThemeMenuUtils;
import com.ihs.inputmethod.uimodules.ui.theme.utils.ThemeZipDownloadUtils;
import com.ihs.inputmethod.uimodules.utils.ViewConvertor;
import com.ihs.inputmethod.uimodules.widget.MdProgressBar;
import com.ihs.inputmethod.uimodules.widget.TrialKeyboardDialog;
import com.ihs.inputmethod.utils.ActionbarUtils;
import com.ihs.keyboardutils.adbuffer.AdLoadingView;
import com.ihs.keyboardutils.iap.RemoveAdsManager;
import com.ihs.keyboardutils.nativeads.KCNativeAdView;
import com.kc.commons.utils.KCCommonUtils;
import com.kc.utils.KCAnalytics;
import com.keyboard.common.KeyboardActivationGuideActivity;
import com.keyboard.core.themes.custom.KCCustomThemeManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ThemeDetailActivity extends HSAppCompatActivity implements View.OnClickListener, CommonThemeCardAdapter.ThemeCardItemClickListener, LockerAppGuideManager.ILockerInstallStatusChangeListener {
    public final static String INTENT_KEY_THEME_NAME = "themeName";
    private NestedScrollView rootView;
    private View screenshotContainer;
    private ImageView keyboardThemeScreenShotImageView;
    private MdProgressBar screenshotLoading;
    private TextView leftBtn;
    private TextView rightBtn;
    private RecyclerView recommendRecyclerView;
    private ThemeAdapter themeAdapter;
    private TrialKeyboardDialog trialKeyboardDialog;
    private String themeName;
    private HSKeyboardTheme.ThemeType themeType;
    private HSKeyboardTheme keyboardTheme;
    private KCNativeAdView nativeAdView;
    private String themeLockerBgUrl;

    private static final int KEYBOARD_ACTIVIATION_FROM_THEME_CARD = 1;
    private static final int KEYBOARD_ACTIVIATION_FROM_APPLY_BUTTON = 2;

    private INotificationObserver notificationObserver = new INotificationObserver() {
        @Override
        public void onReceive(String s, HSBundle hsBundle) {
            if (HSKeyboardThemeManager.HS_NOTIFICATION_THEME_LIST_CHANGED.equals(s)) {
                updateCurrentThemeStatus();
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getStringExtra(INTENT_KEY_THEME_NAME) != null) {
            themeName = intent.getStringExtra(INTENT_KEY_THEME_NAME);
        }
        if (themeName != null) {
            List<HSKeyboardTheme> allKeyboardThemeList = HSKeyboardThemeManager.getAllKeyboardThemeList();
            for (HSKeyboardTheme keyboardTheme : allKeyboardThemeList) {
                if (themeName.equals(keyboardTheme.mThemeName) || themeName.equals(keyboardTheme.getThemePkName())) {
                    this.keyboardTheme = keyboardTheme;
                    this.themeType = keyboardTheme.getThemeType();
                    break;
                }
            }
        }

        if (keyboardTheme != null) {

            if (keyboardTheme.getThemeType() == HSKeyboardTheme.ThemeType.CUSTOM) {
                screenshotContainer.getLayoutParams().height = (int) (getResources().getDisplayMetrics().widthPixels * (HSResourceUtils.getDefaultKeyboardHeight(getResources()) * 1.0f / HSResourceUtils.getDefaultKeyboardWidth(getResources())));
                getSupportActionBar().setTitle(R.string.theme_detail_custom_theme_title_name);
                keyboardThemeScreenShotImageView.setImageURI(Uri.fromFile(new File(HSKeyboardThemeManager.getKeyboardThemeScreenshotFile(keyboardTheme))));
            } else {
                screenshotContainer.getLayoutParams().height = (int) (getResources().getDisplayMetrics().widthPixels * 850 * 1.0f / 1080);
                String themeNameTitle = keyboardTheme.getThemeShowName();
                getSupportActionBar().setTitle(getString(R.string.default_themes, getString(R.string.app_name)));

                String largePreviewImgUrl = keyboardTheme.getLargePreivewImgUrl();
                if (largePreviewImgUrl != null) {
                    keyboardThemeScreenShotImageView.setImageDrawable(null);
                    Glide.with(HSApplication.getContext()).load(largePreviewImgUrl).listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            if (isCurrentImageUri(largePreviewImgUrl)) {
                                screenshotLoading.setVisibility(View.GONE);
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            if (isCurrentImageUri(largePreviewImgUrl)) {
                                screenshotLoading.setVisibility(View.GONE);
                            }
                            return false;
                        }
                    }).into(keyboardThemeScreenShotImageView);

                }
            }

            themeLockerBgUrl = ThemeLockerBgUtil.getInstance().getThemeBgUrl(themeName);

            setButtonText();

        }

        String text = rightBtn.getText().toString();
        boolean applied = text.equalsIgnoreCase(getString(R.string.theme_card_menu_applied));
        if (ThemeAnalyticsReporter.getInstance().isThemeAnalyticsEnabled() && !applied && themeName != null) {
            ThemeAnalyticsReporter.getInstance().recordThemeShownInDetailActivity(themeName);
        }

        //show all themes except custom themes and current theme
        themeAdapter.setDataList(getKeyboardThemesExceptMe());
        themeAdapter.notifyDataSetChanged();

        rootView.smoothScrollTo(0, 0);
    }

    private boolean isCurrentImageUri(String imageUri) {
        if (keyboardTheme != null && keyboardTheme.getLargePreivewImgUrl() != null && keyboardTheme.getLargePreivewImgUrl().equals(imageUri)) {
            return true;
        }
        return false;
    }

    private List getKeyboardThemesExceptMe() {
        List<HSKeyboardTheme> keyboardThemeList = new ArrayList<>();
        keyboardThemeList.addAll(HSKeyboardThemeManager.getAllKeyboardThemeList());
        keyboardThemeList.removeAll(KCCustomThemeManager.getInstance().getAllCustomThemes());
        keyboardThemeList.removeAll(HSKeyboardThemeManager.getDownloadedThemeList());
        if (keyboardTheme != null) {
            keyboardThemeList.remove(keyboardTheme);
        }

        List<HomeModel> homeModelList = new ArrayList<>();
        HomeModel<HSKeyboardTheme> homeModel;
        for (HSKeyboardTheme hsKeyboardTheme : keyboardThemeList) {
            homeModel = new HomeModel<>();
            homeModel.item = hsKeyboardTheme;
            homeModel.isTheme = true;
            homeModelList.add(homeModel);
        }

        return homeModelList;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KeyboardFullScreenAd.showSessionOneTimeAd("themeDetail");

        setContentView(R.layout.activity_theme_detail);
        ActionbarUtils.setCustomTitleWithBackIcon(this, findViewById(R.id.toolbar), R.string.activity_thematic_details_title);
        initView();
        onNewIntent(getIntent());

        HSGlobalNotificationCenter.addObserver(HSKeyboardThemeManager.HS_NOTIFICATION_THEME_LIST_CHANGED, notificationObserver);
        LockerAppGuideManager.getInstance().addLockerInstallStatusChangeListener(this);
    }

    private void initView() {
        rootView = (NestedScrollView) findViewById(R.id.root_view);
        screenshotContainer = findViewById(R.id.keyboard_theme_screenshot_container);
        keyboardThemeScreenShotImageView = (ImageView) findViewById(R.id.keyboard_theme_screenshot);
        screenshotLoading = (MdProgressBar) findViewById(R.id.screenshot_loading);
        leftBtn = (TextView) findViewById(R.id.theme_detail_left_btn);
        rightBtn = (TextView) findViewById(R.id.theme_detail_right_btn);
        leftBtn.setOnClickListener(this);
        rightBtn.setOnClickListener(this);

        recommendRecyclerView = (RecyclerView) findViewById(R.id.theme_detail_recommend_recycler_view);
        recommendRecyclerView.setNestedScrollingEnabled(false);
        recommendRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        themeAdapter = new ThemeAdapter(this);
        recommendRecyclerView.setAdapter(themeAdapter);

        addNativeAdView();
    }

    private void addNativeAdView() {
        if (!RemoveAdsManager.getInstance().isRemoveAdsPurchased()) {
            // 添加广告
            if (nativeAdView == null) {
                final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ad_container);
                int width = HSDisplayUtils.getScreenWidthForContent() - HSDisplayUtils.dip2px(16);
                View view = LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.ad_style_2, null);
                LinearLayout loadingView = (LinearLayout) LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.ad_loading_3, null);
                LinearLayout.LayoutParams loadingLP = new LinearLayout.LayoutParams(width, (int) (width / 1.9f + HSApplication.getContext().getResources().getDimension(R.dimen.ad_style_theme_card_bottom_container_height)));
                loadingView.setLayoutParams(loadingLP);
                loadingView.setGravity(Gravity.CENTER);
                nativeAdView = new KCNativeAdView(HSApplication.getContext());
                nativeAdView.setAdLayoutView(view);
                nativeAdView.setLoadingView(loadingView);
                nativeAdView.setPrimaryViewSize(width, (int) (width / 1.9f));
                nativeAdView.load(AdPlacements.NATIVE_THEME_TRY);
                CardView cardView = ViewConvertor.toCardView(nativeAdView);
                linearLayout.addView(cardView);
                linearLayout.setVisibility(View.GONE);
                nativeAdView.setOnAdLoadedListener(new KCNativeAdView.OnAdLoadedListener() {
                    @Override
                    public void onAdLoaded(KCNativeAdView nativeAdView) {
                        linearLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (trialKeyboardDialog != null) {
            KCCommonUtils.dismissDialog(trialKeyboardDialog);
        }
    }

    private void setButtonText() {
        switch (themeType) {
            case NEED_DOWNLOAD:
                if (TextUtils.isEmpty(themeLockerBgUrl) || LockerSettings.isLockerMuted()) {
                    leftBtn.setText(R.string.theme_card_menu_share);
                } else {
                    leftBtn.setText(R.string.theme_card_set_locker_bg);
                }
                if (ThemeDownloadManager.getInstance().isDownloading(keyboardTheme.mThemeName)) {
                    rightBtn.setText(HSApplication.getContext().getString(R.string.theme_card_menu_downloading));
                    rightBtn.setEnabled(false);
                } else {
                    ThemeHomeModel themeHomeModel = new ThemeHomeModel();
                    themeHomeModel.keyboardTheme = keyboardTheme;
                    if (LockedCardActionUtils.shouldLock(themeHomeModel)) {
                        rightBtn.setText(HSApplication.getContext().getString(R.string.theme_card_menu_unlock_for_free));
                    } else {
                        rightBtn.setText(HSApplication.getContext().getString(R.string.theme_card_menu_download));
                    }
                    rightBtn.setEnabled(true);
                }
                break;
            case CUSTOM:
            case DOWNLOADED:
            case BUILD_IN:
                updateApplyButton();
                if (TextUtils.isEmpty(themeLockerBgUrl)) {
                    leftBtn.setText(R.string.theme_card_menu_share);
                } else {
                    leftBtn.setText(R.string.theme_card_set_locker_bg);
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.theme_detail_left_btn) {
            handleButtonClick(v);
        } else if (id == R.id.theme_detail_right_btn) {
            handleButtonClick(v);
        }
    }

    private void handleButtonClick(View v) {
        if (keyboardTheme == null) {
            return;
        }

        String text = ((TextView) v).getText().toString();
        if (HSApplication.getContext().getString(R.string.theme_card_menu_download).equalsIgnoreCase(text)) {
            ((TextView) v).setText(R.string.theme_card_menu_downloading);
            v.setEnabled(false);
            if (!downloadTheme()) return;
            KCAnalytics.logEvent("themedetails_download_clicked", "themeName", themeName);
            if (ThemeAnalyticsReporter.getInstance().isThemeAnalyticsEnabled()) {
                ThemeAnalyticsReporter.getInstance().recordThemeDownloadInDetailActivity(themeName);
            }
        } else if (HSApplication.getContext().getString(R.string.theme_card_menu_delete).equalsIgnoreCase(text)) {
            KCCustomThemeManager.getInstance().removeCustomTheme(keyboardTheme.getThemeId());

        } else if (HSApplication.getContext().getString(R.string.theme_card_menu_share).equalsIgnoreCase(text)) {
            ThemeMenuUtils.shareTheme(this, keyboardTheme);
            KCAnalytics.logEvent("themedetails_share_clicked", "themeName", themeName);
        } else if (HSApplication.getContext().getString(R.string.theme_card_set_locker_bg).equalsIgnoreCase(text)) {
            KCAnalytics.logEvent("keyboard_setaslockscreen_button_clicked", "occasion", "app_theme_detail");
            LockerEnableDialog.showLockerEnableDialog(this, themeLockerBgUrl, getString(R.string.locker_enable_title_no_desc), themeName, new LockerEnableDialog.OnLockerBgLoadingListener() {
                @Override
                public void onFinish() {
                }
            });

        } else if (HSApplication.getContext().getString(R.string.theme_card_menu_apply).equalsIgnoreCase(text)) {
            File file = new File(ThemeDownloadManager.getThemeDownloadLocalFile(keyboardTheme.mThemeName));
            if (keyboardTheme.getThemeType() == HSKeyboardTheme.ThemeType.DOWNLOADED && !HSInstallationUtils.isAppInstalled(keyboardTheme.getThemePkName()) && file.exists() && file.length() > 0) {
                ApkUtils.startInstall(HSApplication.getContext(), Uri.fromFile(file));
            } else {
                if (HSKeyboardThemeManager.setKeyboardTheme(themeName)) {
                    Intent intent = new Intent(this, KeyboardActivationGuideActivity.class);
                    startActivityForResult(intent, KEYBOARD_ACTIVIATION_FROM_APPLY_BUTTON);
                } else {
                    String failedString = HSApplication.getContext().getResources().getString(R.string.theme_apply_failed);
                    HSToastUtils.toastCenterLong(String.format(failedString, keyboardTheme.getThemeShowName()));
                }
            }
        } else if (HSApplication.getContext().getString(R.string.theme_card_menu_applied).equalsIgnoreCase(text)) {

        } else if (HSApplication.getContext().getString(R.string.theme_card_menu_unlock_for_free).equalsIgnoreCase(text)) {
            ThemeHomeModel themeHomeModel = new ThemeHomeModel();
            themeHomeModel.keyboardTheme = keyboardTheme;
            LockedCardActionUtils.handleLockAction(this, LockedCardActionUtils.LOCKED_CARD_FROM_THEME_DETAIL, themeHomeModel, new Runnable() {
                @Override
                public void run() {
                    downloadTheme();
                    ((TextView) v).setText(R.string.theme_card_menu_downloading);
                }
            });
        }
    }

    private boolean downloadTheme() {
        //直接下载主题zip包
        boolean downloadThemeZip = HSConfig.optBoolean(false, "Application", "KeyboardTheme", "DownloadThemeZip", "DownloadInApp");
        if (downloadThemeZip) {
            if (HSKeyboardThemeManager.isThemeZipFileDownloadAndUnzipSuccess(keyboardTheme.mThemeName)) {
                return false;
            }
            String from = "detail";
            ThemeZipDownloadUtils.startDownloadThemeZip(this, from, keyboardTheme.mThemeName, keyboardTheme.getSmallPreivewImgUrl(), new AdLoadingView.OnAdBufferingListener() {
                @Override
                public void onDismiss(boolean success, boolean manually) {
                    if (success) {
                        ThemeZipDownloadUtils.logDownloadSuccessEvent(keyboardTheme.mThemeName, from);
                        if (HSKeyboardThemeManager.isThemeZipFileDownloadAndUnzipSuccess(keyboardTheme.mThemeName)) {
                            HSKeyboardThemeManager.moveNeedDownloadThemeToDownloadedList(keyboardTheme.mThemeName, true);
                            //直接应用主题
                            if (HSKeyboardThemeManager.setKeyboardTheme(themeName)) {
                                Intent intent = new Intent(ThemeDetailActivity.this, KeyboardActivationGuideActivity.class);
                                startActivityForResult(intent, KEYBOARD_ACTIVIATION_FROM_APPLY_BUTTON);
                            } else {
                                String failedString = HSApplication.getContext().getResources().getString(R.string.theme_apply_failed);
                                HSToastUtils.toastCenterLong(String.format(failedString, keyboardTheme.getThemeShowName()));
                            }
                        }
                    }
                }
            });

            ThemeZipDownloadUtils.logDownloadSuccessEvent(keyboardTheme.mThemeName, from);
        } else {
            ThemeDownloadManager.getInstance().downloadTheme(keyboardTheme);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == KEYBOARD_ACTIVIATION_FROM_THEME_CARD) {
            themeAdapter.finishKeyboardActivation(resultCode == RESULT_OK);
        } else if (requestCode == KEYBOARD_ACTIVIATION_FROM_APPLY_BUTTON) {
            if (resultCode == RESULT_OK) {
                if (LockerSettings.isLockerEnableShowSatisfied()) {
                    LockerEnableDialog.showLockerEnableDialog(ThemeDetailActivity.this,
                            ThemeLockerBgUtil.getInstance().getThemeBgUrl(HSKeyboardThemeManager.getCurrentThemeName()),
                            getString(R.string.locker_enable_title_has_text),
                            this::showTryKeyboardDialog);
                } else {
                    showTryKeyboardDialog();
                }
            }
        }
    }

    private void showTryKeyboardDialog() {
        if (trialKeyboardDialog == null) {
            trialKeyboardDialog = new TrialKeyboardDialog.Builder(this).create();
            trialKeyboardDialog.setOnDismissListener(dialog -> updateApplyButton());
        }
        trialKeyboardDialog.show(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 更改当前主题状态,有可能当前主题对应的主题包APK被安装或者删除了
     */
    private void updateCurrentThemeStatus() {
        if (themeName != null) {
            if (themeType == HSKeyboardTheme.ThemeType.DOWNLOADED) {
                //查找是否当前显示的主题是否被删了,需要重新下载
                boolean isNeedDownload = false;
                List<HSKeyboardTheme> needDownloadThemes = HSKeyboardThemeManager.getNeedDownloadThemeList();
                for (HSKeyboardTheme keyboardTheme : needDownloadThemes) {
                    if (themeName.equals(keyboardTheme.mThemeName)) {
                        isNeedDownload = true;
                        break;
                    }
                }

                //如果需要重新需要下载,修改类型并修改按钮文字
                if (isNeedDownload) {
                    themeType = HSKeyboardTheme.ThemeType.NEED_DOWNLOAD;
                    setButtonText();
                }
            } else if (themeType == HSKeyboardTheme.ThemeType.NEED_DOWNLOAD) {
                //查找是否当前显示的主题是否已下载
                boolean isDownloaded = false;
                List<HSKeyboardTheme> downloadedThemes = HSKeyboardThemeManager.getDownloadedThemeList();
                for (HSKeyboardTheme keyboardTheme : downloadedThemes) {
                    if (themeName.equals(keyboardTheme.mThemeName)) {
                        isDownloaded = true;
                        break;
                    }
                }
                //如果已下载,修改类型并修改按钮文字
                if (isDownloaded) {
                    themeType = HSKeyboardTheme.ThemeType.DOWNLOADED;
                    setButtonText();
                }
            }
        }
        if (themeAdapter != null) {
            themeAdapter.setDataList(getKeyboardThemesExceptMe());
            themeAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        if (nativeAdView != null) {
            nativeAdView.release();
            nativeAdView = null;
        }
        if (trialKeyboardDialog != null) {
            KCCommonUtils.dismissDialog(trialKeyboardDialog);
            trialKeyboardDialog = null;
        }
        HSGlobalNotificationCenter.removeObserver(notificationObserver);
        LockerAppGuideManager.getInstance().removeLockerInstallStatusChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onCardClick(HSKeyboardTheme keyboardTheme) {
        KCAnalytics.logEvent("themedetails_themes_preview_clicked", "keyboardTheme", keyboardTheme.mThemeName);
    }

    @Override
    public void onKeyboardActivationStart() {
        Intent intent = new Intent(this, KeyboardActivationGuideActivity.class);
        startActivityForResult(intent, KEYBOARD_ACTIVIATION_FROM_THEME_CARD);
    }

    @Override
    public void onMenuShareClick(HSKeyboardTheme keyboardTheme) {
    }

    @Override
    public void onMenuDownloadClick(HSKeyboardTheme keyboardTheme) {
        KCAnalytics.logEvent("themedetails_themes_download_clicked", "keyboardTheme", keyboardTheme.mThemeName);
        if (ThemeAnalyticsReporter.getInstance().isThemeAnalyticsEnabled()) {
            ThemeAnalyticsReporter.getInstance().recordThemeDownloadInDetailActivity(keyboardTheme.mThemeName);
        }
    }

    @Override
    public void onMenuDeleteClick(HSKeyboardTheme keyboardTheme) {

    }

    @Override
    public void onMenuAppliedClick(HSKeyboardTheme keyboardTheme) {

    }

    private void updateApplyButton() {
        if (TextUtils.equals(themeName, HSKeyboardThemeManager.getCurrentThemeName())) {
            rightBtn.setText(R.string.theme_card_menu_applied);
            rightBtn.setEnabled(false);
        } else {
            rightBtn.setText(R.string.theme_card_menu_apply);
            rightBtn.setEnabled(true);
        }
    }

    @Override
    public void onLockerInstallStatusChange() {
        setButtonText();
    }
}
