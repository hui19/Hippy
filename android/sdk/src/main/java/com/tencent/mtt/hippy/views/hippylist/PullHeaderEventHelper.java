package com.tencent.mtt.hippy.views.hippylist;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.utils.PixelUtil;
import com.tencent.mtt.nxeasy.listview.pagehelper.IHeaderRefreshListener;
import com.tencent.mtt.nxeasy.listview.pagehelper.IHeaderRefreshView;

/**
 * Created by niuniuyang on 2021/1/8.
 * Description
 */
class PullHeaderEventHelper implements IHeaderRefreshListener, IHeaderRefreshView {

    public static final String EVENT_TYPE_HEADER_PULLING = "onHeaderPulling";
    public static final String EVENT_TYPE_HEADER_RELEASED = "onHeaderReleased";
    private final RenderNode renderNode;
    private HippyRecyclerView recyclerView;
    private View renderNodeView;
    private LinearLayout headerContainer;
    private LayoutParams contentLayoutParams;

    PullHeaderEventHelper(HippyRecyclerView recyclerView, RenderNode renderNode) {
        this.recyclerView = recyclerView;
        this.renderNode = renderNode;
        headerContainer = new LinearLayout(recyclerView.getContext());
    }

    public void setRenderNodeView(View renderNodeView) {
        if (this.renderNodeView != renderNodeView) {
            this.renderNodeView = renderNodeView;
            headerContainer.removeAllViews();
            contentLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, renderNode.getHeight());
            contentLayoutParams.gravity = Gravity.BOTTOM;
            headerContainer.addView(renderNodeView, contentLayoutParams);
        }
    }

    public View getView() {
        return headerContainer;
    }

    @Override
    public void onMove(float deltaY, float sumOffset) {
        HippyMap params = new HippyMap();
        params.pushDouble("contentOffset", PixelUtil.px2dp(deltaY));
        sendPullHeaderEvent(EVENT_TYPE_HEADER_PULLING, params);
    }

    @Override
    public void setLoadingStatus(int headerStatusLoading) {

    }

    @Override
    public int getContentHeight() {
        return renderNode.getHeight();
    }

    @Override
    public void onHeaderLoadMore() {
        sendPullHeaderEvent(EVENT_TYPE_HEADER_RELEASED, new HippyMap());
    }

    protected void sendPullHeaderEvent(String eventName, HippyMap param) {
        new HippyViewEvent(eventName).send(renderNodeView, param);
    }

    public void onHeaderRefreshFinish() {

    }

    public void onHeaderRefresh() {

    }
}
