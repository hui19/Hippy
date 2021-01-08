package android.support.v7.widget;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.views.hippylist.HippyRecyclerViewHolder;

/**
 * Created by niuniuyang on 2021/1/4.
 * Description
 */
public class HippyRecyclerPool extends RecyclerView.RecycledViewPool {

    private final View recyclerView;
    private final HippyRecyclerExtension viewCacheExtension;
    private final HippyEngineContext hpContext;

    public HippyRecyclerPool(HippyEngineContext hpContext, View recyclerView,
            HippyRecyclerExtension viewCacheExtension) {
        this.hpContext = hpContext;
        this.recyclerView = recyclerView;
        this.viewCacheExtension = viewCacheExtension;
    }

    /**
     * 从缓存池里面获取ViewHolder进行复用
     * 1、精确命中相同的renderNode
     * 2、命中相同Type的ViewHolder，并且对应的RenderNode是没有被前端删除的
     * 如果renderNode.isDelete为true,说明前端删除了RenderNode，
     * 此时会调用 RenderManager框架的deleteChild, 所以view也不会存在了。
     * 即使找到了相同type的Holder，也不能复用了。
     */
    @Override
    public ViewHolder getRecycledView(int viewType) {
        ScrapData scrapData = mScrap.get(viewType);
        if (scrapData == null) {
            return null;
        }
        ViewHolder delegateHolder = null;
        for (ViewHolder holder : scrapData.mScrapHeap) {
            if (isTheSameRenderNode((HippyRecyclerViewHolder) holder)) {
                scrapData.mScrapHeap.remove(holder);
                delegateHolder = holder;
                break;
            }
        }
        //没有精确命中，再看看缓存池里面有没有相同类型的viewType
        if (delegateHolder == null) {
            delegateHolder = super.getRecycledView(viewType);
        }
        //检测对应的节点是否被删除
        if (delegateHolder instanceof HippyRecyclerViewHolder
                && ((HippyRecyclerViewHolder) delegateHolder).bindNode.isDelete()) {
            return null;
        }
        return delegateHolder;
    }

    /**
     * putRecycledView 可能出现缓存已经超过最大值，会发生ViewHolder被抛弃，
     * 抛弃需要后，需要同步修改 renderManager内部创建对应的view，这样
     * {@link com.tencent.mtt.hippy.views.hippylist.HippyRecyclerListAdapter#onCreateViewHolder(
     *ViewGroup, int)}，才能通过 {@link RenderNode#createViewRecursive()} 创建新的view,
     * 否则createViewRecursive会返回null。
     *
     * @param scrap
     */
    @Override
    public void putRecycledView(ViewHolder scrap) {
        notifyAboundIfNeed(scrap);
        super.putRecycledView(scrap);
    }

    private void notifyAboundIfNeed(ViewHolder scrap) {
        int viewType = scrap.getItemViewType();
        ScrapData scrapData = this.mScrap.get(viewType);
        if (scrapData != null && scrapData.mScrapHeap.size() >= scrapData.mMaxScrap) {
            onViewAbound((HippyRecyclerViewHolder) scrap);
        }
    }

    /**
     * 同步删除RenderNode对应注册的View，deleteChild是递归删除RenderNode创建的所有的view
     */
    private void onViewAbound(HippyRecyclerViewHolder viewHolder) {
        if (viewHolder.bindNode != null && !viewHolder.bindNode.isDelete()) {
            viewHolder.bindNode.setLazy(true);
            RenderNode parentNode = viewHolder.bindNode.getParent();
            if (parentNode != null) {
                hpContext.getRenderManager().getControllerManager()
                        .deleteChild(parentNode.getId(), viewHolder.bindNode.getId());
            }
            viewHolder.bindNode.setRecycleItemTypeChangeListener(null);
        }
    }

    /**
     * 是否是节点完全相等
     */
    private boolean isTheSameRenderNode(HippyRecyclerViewHolder holder) {
        return holder.bindNode == hpContext.getRenderManager()
                .getRenderNode(recyclerView.getId()).getChildAt(viewCacheExtension.getCurrentPosition());
    }
}
