package android.support.v7.widget;

import android.support.v7.widget.RecyclerView.RecycledViewPool;
import android.support.v7.widget.RecyclerView.RecycledViewPool.ScrapData;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.SparseArray;
import com.tencent.mtt.hippy.uimanager.ListItemRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import com.tencent.mtt.hippy.views.hippylist.HippyRecyclerViewHolder;
import java.util.ArrayList;

/**
 * Created by niuniuyang on 2021/1/4.
 * Description
 */
public class HippyItemTypeHelper {

  HippyRecyclerViewBase recyclerView;
  private Recycler recycler;

  public HippyItemTypeHelper(HippyRecyclerViewBase recyclerView) {
    this.recyclerView = recyclerView;
    this.recycler = recyclerView.mRecycler;
  }

  public void updateItemType(int oldType, int newType, ListItemRenderNode listItemRenderNode) {
    int count = recyclerView.getChildCount();
    for (int i = 0; i < count; i++) {
      final RecyclerView.ViewHolder holder = recyclerView
        .getChildViewHolder(recyclerView.getChildAt(i));
      if (changeTypeIfNeed(oldType, newType, listItemRenderNode, holder)) {
        return;
      }
    }

    if (updateItemType(oldType, newType, listItemRenderNode, recycler.mAttachedScrap)) {
      return;
    }

    if (updateItemType(oldType, newType, listItemRenderNode, recyclerView.mRecycler.mCachedViews)) {
      return;
    }

    updateTypeForRecyclerPool(oldType, newType, listItemRenderNode);
  }

  private void updateTypeForRecyclerPool(int oldType, int newType, ListItemRenderNode renderNode) {
    if (recycler.getRecycledViewPool() != null) {
      SparseArray<ScrapData> scrap = recycler.getRecycledViewPool().mScrap;
      RecycledViewPool.ScrapData scrapData = scrap.get(oldType);
      if (scrapData != null && !scrapData.mScrapHeap.isEmpty()) {
        for (RecyclerView.ViewHolder holder : scrapData.mScrapHeap) {
          if (changeTypeIfNeed(oldType, newType, renderNode, holder)) {
            scrapData.mScrapHeap.remove(holder);
            addNewType(newType, holder);
            return;
          }
        }
      }
    }
  }

  private void addNewType(int newType, ViewHolder holder) {
    holder.mItemViewType = newType;
    SparseArray<ScrapData> scrap = recycler.getRecycledViewPool().mScrap;
    ScrapData newScrapData = scrap.get(newType);
    if (newScrapData == null) {
      newScrapData = new ScrapData();
      scrap.append(newType, newScrapData);
    }
    newScrapData.mScrapHeap.add(holder);
  }

  private boolean updateItemType(int oldType, int newType, ListItemRenderNode listItemRenderNode,
    ArrayList<RecyclerView.ViewHolder> viewHolders) {
    final int cacheSize = viewHolders.size();
    for (int i = 0; i < cacheSize; i++) {
      final RecyclerView.ViewHolder holder = viewHolders.get(i);
      if (changeTypeIfNeed(oldType, newType, listItemRenderNode, holder)) {
        return true;
      }
    }
    return false;
  }

  private boolean changeTypeIfNeed(int oldType, int newType, ListItemRenderNode listItemRenderNode,
    RecyclerView.ViewHolder holder) {
    if (holder.getItemViewType() == oldType && holder instanceof HippyRecyclerViewHolder) {
      RenderNode holderNode = ((HippyRecyclerViewHolder) holder).bindNode;
      if (holderNode == listItemRenderNode) {
        holder.mItemViewType = newType;
        return true;
      }
    }
    return false;
  }
}
