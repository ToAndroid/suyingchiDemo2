package com.ihs.inputmethod.callflash;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.GridLayoutManager;

import com.acb.call.themes.Type;
import com.ihs.inputmethod.base.BaseListActivity;
import com.ihs.inputmethod.callflash.adapter.CallFlashAdapter;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.inputmethod.uimodules.ui.customize.service.CustomizeService;

import java.util.List;

/**
 * Created by jixiang on 18/1/20.
 */

public class CallFlashListActivity extends BaseListActivity implements ServiceConnection {
    private CallFlashAdapter callFlashAdapter;

    public static void startThisActivity(Activity activity) {
        activity.startActivity(new Intent(activity, CallFlashListActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind to CustomizeService
        Intent intent = new Intent(this, CustomizeService.class);
        intent.setAction(CustomizeService.class.getName());
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (callFlashAdapter != null) {
            List data = Type.values();
            callFlashAdapter.setDataList(data);
            callFlashAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    protected void initView() {
        callFlashAdapter = new CallFlashAdapter(this);
        List data = Type.values();
        callFlashAdapter.setDataList(data);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(callFlashAdapter);
    }


    @Override
    protected int getTitleTextResId() {
        return R.string.activity_call_flash_title;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }
}
