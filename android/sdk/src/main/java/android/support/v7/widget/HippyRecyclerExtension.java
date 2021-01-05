package android.support.v7.widget;

import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.views.hippylist.HippyRecyclerViewHolder;
import java.util.ArrayList;

/**
 * Created by niuniuyang on 2021/1/4.
 * Description
 */
public class HippyRecyclerExtension extends RecyclerView.ViewCacheExtension {

  private final HippyEngineContext hpContext;
  private HippyRecyclerViewBase recyclerView;
  private int currentPosition;

  public HippyRecyclerExtension(HippyRecyclerViewBase recyclerView, HippyEngineContext hpContext) {
    this.recyclerView = recyclerView;
    this.hpContext = hpContext;
  }

  public int getCurrentPosition() {
    return currentPosition;
  }

  @Override
  public View getViewForPositionAndType(Recycler recycler, int position, int type) {
    currentPosition = position;
    View bestView = findInAttachedScrap(recycler, position, type);
    if (bestView == null) {
      bestView = findInCachedScrap(recycler, position, type);
    }
    return bestView;
  }

  private View findInCachedScrap(Recycler recycler, int position, int type) {
    ViewHolder bestHolder = findBestHolder(recycler.mCachedViews, position, type);
    if (bestHolder != null) {
      recycler.mCachedViews.remove(bestHolder);
      return bestHolder.itemView;
    }
    return null;
  }

  protected View findInAttachedScrap(Recycler recycler, int position, int type) {
    ViewHolder bestHolder = findBestHolder(recycler.mAttachedScrap, position, type);
    if (bestHolder != null) {
      bestHolder.unScrap();
      return bestHolder.itemView;
    }
    return null;
  }

  private RecyclerView.ViewHolder findBestHolder(ArrayList<ViewHolder> viewHolders, int position,
    int type) {
    int scrapCount = viewHolders.size();
    for (int i = 0; i < scrapCount; i++) {
      final RecyclerView.ViewHolder holder = viewHolders.get(i);
      if (isTheBestHolder(position, type, holder)) {
        return holder;
      }
    }
    return null;
  }

  protected boolean isTheBestHolder(int position, int type, ViewHolder holder) {
    if (holder.getAdapterPosition() != position || holder.isInvalid() || holder.isRemoved()) {
      return false;
    }
    if (holder.getItemViewType() == type && holder instanceof HippyRecyclerViewHolder) {
      return ((HippyRecyclerViewHolder) holder).bindNode == hpContext.getRenderManager()
        .getRenderNode(recyclerView.getId()).getChildAt(position);
    }
    return false;
  }
}
