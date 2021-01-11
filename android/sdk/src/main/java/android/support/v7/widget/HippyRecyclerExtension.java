/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * RecyclerView的扩展的缓存，如果mAttachedScrap 和  mCachedViews 都没有命中，会在访问RecyclerPool之前
 * 先访问ViewCacheExtension。参看{@link Recycler#tryGetViewHolderForPositionByDeadline}的执行流程
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

    /**
     * 找到对应的bindNode
     */
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
