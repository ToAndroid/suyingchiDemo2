package com.ihs.inputmethod.uimodules.ui.sticker;

import android.os.AsyncTask;

import com.ihs.commons.config.HSConfig;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by yanxia on 2017/6/9.
 */

public class StickerDataManager {
    public static final String STICKER_DATA_LOAD_FINISH_NOTIFICATION = "sticker_data_load_finish";
    public static final String STICKER_DATA_CHANGE_NOTIFICATION = "sticker_data_change_finish";
    private static StickerDataManager instance;
    private List<StickerGroup> stickerGroups;
    private boolean isReady = false;

    private StickerDataManager() {
        stickerGroups = new ArrayList<>();
        loadStickersAsync();
    }

    public static StickerDataManager getInstance() {
        if (instance == null) {
            synchronized (StickerDataManager.class) {
                if (instance == null) {
                    instance = new StickerDataManager();
                }
            }
        }
        return instance;
    }

    public void onConfigChange() {
        loadStickersAsync();
    }

    private void loadStickersAsync() {
        new LoadDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class LoadDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            isReady = false;
            loadStickers();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isReady = true;
            HSGlobalNotificationCenter.sendNotificationOnMainThread(STICKER_DATA_LOAD_FINISH_NOTIFICATION);
        }
    }

    private synchronized void loadStickers() {
        List<Map<String, Object>> stickerConfigList = (List<Map<String, Object>>) HSConfig.getList("Application", "StickerGroupList");
        stickerGroups.clear();
        for (Map<String, Object> map : stickerConfigList) {
            String stickerGroupName = (String) map.get("name");
            String stickerGroupDownloadDisplayName = (String) map.get("showName");
            StickerGroup stickerGroup = new StickerGroup(stickerGroupName);
            stickerGroup.setDownloadDisplayName(stickerGroupDownloadDisplayName);
            stickerGroups.add(stickerGroup);
        }

    }

    boolean isStickersReady() {
        return isReady;
    }

    void updateStickerGroupList(StickerGroup stickerGroup) {
        if (!isReady) {
            return;
        }
        stickerGroup.reloadStickers();
        HSGlobalNotificationCenter.sendNotificationOnMainThread(STICKER_DATA_CHANGE_NOTIFICATION);
    }

    List<StickerGroup> getStickerGroupList() {
        if (!isReady) {
            return Collections.emptyList();
        } else {
            return stickerGroups;
        }
    }
}