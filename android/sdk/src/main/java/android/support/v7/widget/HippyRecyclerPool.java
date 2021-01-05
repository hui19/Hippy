package android.support.v7.widget;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
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
    return delegateHolder;
  }

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

  private boolean isTheSameRenderNode(HippyRecyclerViewHolder holder) {
    return holder.bindNode == hpContext.getRenderManager()
      .getRenderNode(recyclerView.getId()).getChildAt(viewCacheExtension.getCurrentPosition());
  }
}
