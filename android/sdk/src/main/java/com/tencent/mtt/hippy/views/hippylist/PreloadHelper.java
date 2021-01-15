package com.tencent.mtt.hippy.views.hippylist;

import static com.tencent.mtt.hippy.views.hippylist.PullFooterEventHelper.EVENT_ON_END_REACHED;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.tencent.mtt.hippy.uimanager.HippyViewEvent;

/**
 * Created by niuniuyang on 2021/1/15.
 * Description
 * 预加载的通知，
 */
public class PreloadHelper extends RecyclerView.OnScrollListener {

    HippyRecyclerView hippyRecyclerView;
    private int preloadItemNumber;
    private int currentPreloadCount = 0;

    public PreloadHelper(HippyRecyclerView hippyRecyclerView) {
        this.hippyRecyclerView = hippyRecyclerView;
    }


    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        int itemCount = recyclerView.getAdapter().getItemCount();
        //频控，记录上次预加载的总条目数，相同就不再次触发预加载
        if (itemCount == currentPreloadCount) {
            return;
        }
        if (recyclerView.getChildCount() > 0) {
            View lastChild = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
            int lastPosition = recyclerView.getChildAdapterPosition(lastChild);
            if (lastPosition + preloadItemNumber >= itemCount) {
                currentPreloadCount = itemCount;
                new HippyViewEvent(EVENT_ON_END_REACHED).send((View) recyclerView.getParent(), null);
            }
        }
    }

    /**
     * @param preloadItemNumber 提前多少条Item，通知前端加载下一页数据
     */
    public void setPreloadItemNumber(int preloadItemNumber) {
        this.preloadItemNumber = preloadItemNumber;
        hippyRecyclerView.removeOnScrollListener(this);
        if (preloadItemNumber > 0) {
            hippyRecyclerView.addOnScrollListener(this);
        }
    }
}
