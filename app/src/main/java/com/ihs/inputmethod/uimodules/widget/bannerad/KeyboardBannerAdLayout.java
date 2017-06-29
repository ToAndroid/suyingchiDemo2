package com.ihs.inputmethod.uimodules.widget.bannerad;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.inputmethod.api.utils.HSDisplayUtils;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.keyboardutils.nativeads.NativeAdParams;
import com.ihs.keyboardutils.nativeads.NativeAdView;

/**
 * Created by Arthur on 17/5/18.
 */

public class KeyboardBannerAdLayout extends FrameLayout {
    private  NativeAdView  nativeAdView;


    public KeyboardBannerAdLayout(@NonNull Context context) {
        super(context);
        init();

    }

    private void init() {

        View containerView = inflate(getContext(), R.layout.keyboard_banner_ad_layout, null);
        setBackgroundColor(Color.WHITE);
        nativeAdView = new NativeAdView(HSApplication.getContext(), containerView);
        final ImageView closeBtn = new ImageView(getContext());
        closeBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.banner_ad_close_button));
        closeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setVisibility(GONE);
                nativeAdView.release();
            }
        });
        LayoutParams closeParams = new LayoutParams(HSDisplayUtils.dip2px(20), HSDisplayUtils.dip2px(20));
        closeParams.setMargins(0,0,HSDisplayUtils.dip2px(8),0);
        closeParams.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        closeBtn.setVisibility(GONE);

        NativeAdParams nativeAdParams = new NativeAdParams(getContext().getResources().getString(R.string.ad_placement_keyboard_banner));
        nativeAdView.configParams(nativeAdParams);
        nativeAdView.setOnAdLoadedListener(new NativeAdView.OnAdLoadedListener() {
            @Override
            public void onAdLoaded(NativeAdView nativeAdView) {
                closeBtn.setVisibility(VISIBLE);
            }
        });
        addView(nativeAdView, ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(closeBtn,closeParams);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        nativeAdView.release();
    }
}