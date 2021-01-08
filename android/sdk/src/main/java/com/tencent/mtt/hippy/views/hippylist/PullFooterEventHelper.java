package com.tencent.mtt.hippy.views.hippylist;

import android.view.View;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;
import com.tencent.mtt.nxeasy.listview.base.footer.FooterExposureHelper;
import com.tencent.mtt.nxeasy.listview.base.footer.IFooterLoadMoreListener;

/**
 * Created by niuniuyang on 2021/1/7.
 * Description
 * 监控footerView的显示状态，并通知前端onEndReached的事件
 */
class PullFooterEventHelper implements IFooterLoadMoreListener {

    private final HippyRecyclerView recyclerView;
    private FooterExposureHelper footerExposureHelper;
    private HippyViewEvent onEndReachedEvent;

    PullFooterEventHelper(HippyRecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    public void enableFooter(View itemView) {
        disableFooter();
        footerExposureHelper = new FooterExposureHelper();
        footerExposureHelper.setFooterListener(this);
        footerExposureHelper.setExposureView(itemView);
    }

    public void disableFooter() {
        if (footerExposureHelper != null) {
            recyclerView.removeOnScrollListener(footerExposureHelper);
            footerExposureHelper = null;
        }
    }

    protected HippyViewEvent getOnEndReachedEvent() {
        if (onEndReachedEvent == null) {
            onEndReachedEvent = new HippyViewEvent("onEndReached");
        }
        return onEndReachedEvent;
    }

    @Override
    public void onFooterLoadMore() {
        getOnEndReachedEvent().send((View) recyclerView.getParent(), null);
    }
}
