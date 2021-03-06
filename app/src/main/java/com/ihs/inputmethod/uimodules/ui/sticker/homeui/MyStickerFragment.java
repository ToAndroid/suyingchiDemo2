package com.ihs.inputmethod.uimodules.ui.sticker.homeui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ihs.app.framework.HSApplication;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.sticker.StickerDataManager;
import com.ihs.inputmethod.uimodules.ui.sticker.StickerDownloadManager;
import com.ihs.inputmethod.uimodules.ui.sticker.StickerGroup;
import com.ihs.inputmethod.uimodules.ui.theme.ui.model.StickerHomeModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by guonan.lv on 17/8/14.
 */

public class MyStickerFragment extends Fragment {
    private RecyclerView recyclerView;
    private CommonStickerAdapter stickerCardAdapter;
    private List<StickerHomeModel> stickerModelList = new ArrayList<>();
    // --Commented out by Inspection (18/1/11 下午2:41):public static final String tabTitle = HSApplication.getContext().getString(R.string.tab_sticker_my);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mysticker, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        initView();
        return view;
    }

    private void initView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        loadStickerModel();
        stickerCardAdapter = new MyStickerAdapter();
        stickerCardAdapter.setItems(stickerModelList);
        recyclerView.setAdapter(stickerCardAdapter);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void loadStickerModel() {
        List<String> downloadStickerNameList = StickerDownloadManager.getInstance().getDownloadedStickerFileList();
        if (downloadStickerNameList == null) {
            return;
        }
        List<StickerGroup> stickerGroupList = StickerDataManager.getInstance().getStickerGroupList();
        for (int i = 0; i < downloadStickerNameList.size(); i++) {
            String downloadStickerName = downloadStickerNameList.get(i);
            if(!TextUtils.isEmpty(downloadStickerName)) {
                for(StickerGroup stickerGroup : stickerGroupList){
                    if (downloadStickerName.equals(stickerGroup.getStickerGroupName())){
                        StickerHomeModel stickerHomeModel = new StickerHomeModel();
                        stickerHomeModel.stickerGroup = stickerGroup;
                        stickerHomeModel.isDownloaded = true;
                        stickerModelList.add(stickerHomeModel);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
