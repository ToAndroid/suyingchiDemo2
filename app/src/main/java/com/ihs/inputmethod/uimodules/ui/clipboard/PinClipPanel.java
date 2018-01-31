package com.ihs.inputmethod.uimodules.ui.clipboard;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.ihs.app.framework.HSApplication;
import com.ihs.inputmethod.uimodules.R;
import com.ihs.panelcontainer.BasePanel;

/**
 * Created by Arthur on 17/12/8.
 */

public class PinClipPanel extends BasePanel {
    @Override
    protected View onCreatePanelView() {
        RecyclerView clipboardPanelPinsView = (RecyclerView) LayoutInflater.from(HSApplication.getContext()).inflate(R.layout.clipboard_pins_layout, null, false);
        setPanelView(clipboardPanelPinsView);
        return clipboardPanelPinsView;
    }
}
