package android.support.v7.widget;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.RecycledViewPool.ScrapData;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;

/**
 * Created by niuniuyang on 2020/10/14.
 *
 * 由于Hippy的特殊需求，需要看到更多的RecyclerVew的方法和成员，这里创建和系统RecyclerView同包名。
 */

public class HippyRecyclerViewBase extends RecyclerView {


  public HippyRecyclerViewBase(@NonNull Context context) {
    super(context);
  }

  public HippyRecyclerViewBase(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public HippyRecyclerViewBase(@NonNull Context context, @Nullable AttributeSet attrs,
    int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override public void requestLayout() {
    super.requestLayout();
  }

  public void recycleAndClearCachedViews() {
    mRecycler.recycleAndClearCachedViews();
  }

  public int getChildCountWithCaches() {
    return getCachedViewHolderCount() + getChildCount();
  }

  public View getChildAtWithCaches(int index) {
    ArrayList<ViewHolder> viewHolders = getCachedViewHolders();
    if (index < viewHolders.size()) {
      return viewHolders.get(index).itemView;
    } else {
      return getChildAt(index - viewHolders.size());
    }
  }

  int getCachedViewHolderCount() {
    int count = mRecycler.mAttachedScrap.size() + mRecycler.mCachedViews.size();
    for (int i = 0; i < mRecycler.getRecycledViewPool().mScrap.size(); i++) {
      ScrapData scrapData = mRecycler.getRecycledViewPool().mScrap.valueAt(i);
      count += scrapData.mScrapHeap.size();
    }
    return count;
  }

  ArrayList<ViewHolder> getCachedViewHolders() {
    ArrayList<ViewHolder> listViewHolder = new ArrayList<>();
    listViewHolder.addAll(mRecycler.mAttachedScrap);
    listViewHolder.addAll(mRecycler.mCachedViews);
    for (int i = 0; i < mRecycler.getRecycledViewPool().mScrap.size(); i++) {
      ScrapData scrapData = mRecycler.getRecycledViewPool().mScrap.valueAt(i);
      listViewHolder.addAll(scrapData.mScrapHeap);
    }
    return listViewHolder;
  }

  public boolean didStructureChange() {
    return mState.didStructureChange();
  }

  public void smoothScrollBy(int dx, int dy, int duration) {
    if (!this.mLayoutFrozen) {
      if (!this.mLayout.canScrollHorizontally()) {
        dx = 0;
      }
      if (!this.mLayout.canScrollVertically()) {
        dy = 0;
      }
      if (dx != 0 || dy != 0) {
        this.mViewFlinger.smoothScrollBy(dx, dy, duration);
      }
    }
  }

  public int getFirstChildPosition() {
    return getChildLayoutPosition(getChildCount() > 0 ? getChildAt(0) : null);
  }


  /**
   * 改成public接口，主要用于hippy业务的特殊需求
   */
  public void dispatchLayout() {
    super.dispatchLayout();
  }
}
