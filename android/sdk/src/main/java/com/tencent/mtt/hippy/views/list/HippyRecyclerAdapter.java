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
package com.tencent.mtt.hippy.views.list;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import com.tencent.mtt.hippy.HippyEngineContext;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.DiffUtils;
import com.tencent.mtt.hippy.uimanager.DiffUtils.LayoutPatch;
import com.tencent.mtt.hippy.uimanager.DiffUtils.Patch;
import com.tencent.mtt.hippy.uimanager.DiffUtils.PatchType;
import com.tencent.mtt.hippy.uimanager.ListItemRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class HippyRecyclerAdapter extends Adapter
{
	protected HippyEngineContext mHippyContext;
	private WeakReference<RecyclerView> mParentRecyclerView;

	public HippyRecyclerAdapter(HippyEngineContext HippyContext, RecyclerView parent) {
		super();
		mHippyContext = HippyContext;
		mParentRecyclerView = new WeakReference<RecyclerView>(parent);
	}

	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		HippyRecyclerViewItem itemView = new HippyRecyclerViewItem(parent.getContext(), parent);
		HippyRecyclerViewHolder holder = new HippyRecyclerViewHolder(itemView);

		return holder;
	}

	public void onBindViewHolder(ViewHolder holder, int position) {
		RecyclerView recyclerView = mParentRecyclerView.get();
		if (holder.itemView == null || recyclerView == null || !(holder instanceof HippyRecyclerViewHolder)
				|| !(holder.itemView instanceof HippyRecyclerViewItem)) {
			return;
		}

		int parentId = recyclerView.getId();
		HippyRecyclerViewItem itemView = (HippyRecyclerViewItem)holder.itemView;
		HippyRecyclerViewHolder viewHoler = (HippyRecyclerViewHolder)holder;

		RenderNode fromNode = viewHoler.mBindNode;
		RenderNode toNode = mHippyContext.getRenderManager().getRenderNode(parentId).getChildAt(position);
		if (toNode == null || toNode == fromNode) {
			return;
		}

		ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
		if (layoutParams == null) {
			layoutParams = ((HippyRecyclerView)recyclerView).generateLayoutParams();
			layoutParams.height = toNode.getHeight();
			layoutParams.width = LayoutParams.MATCH_PARENT;
			itemView.setLayoutParams(layoutParams);
		} else {
			layoutParams.height = toNode.getHeight();
			layoutParams.width = LayoutParams.MATCH_PARENT;
		}

		if (fromNode == null) {
			toNode.setLazy(false);
			View contentView = mHippyContext.getRenderManager().getControllerManager().findView(toNode.getId());
			if (contentView == null) {
				contentView = toNode.createViewRecursive();
			}
			toNode.updateViewRecursive();
			if (itemView.getChildCount() > 0) {
				itemView.removeAllViews();
			}
			itemView.addView(contentView);
		} else {
			ArrayList<DiffUtils.PatchType> patchTypes = DiffUtils.diff(fromNode, toNode);
			LayoutPatch lp = new LayoutPatch(toNode.getX(), toNode.getY(), toNode.getHeight(), toNode.getWidth(), toNode.getId(), toNode.mParent.getId(),
					toNode.getClassName());
			patchTypes.add(new PatchType(Patch.TYPE_LAYOUT, lp));
			DiffUtils.deleteViews(mHippyContext.getRenderManager().getControllerManager(), patchTypes);
			DiffUtils.replaceIds(mHippyContext.getRenderManager().getControllerManager(), patchTypes);
			DiffUtils.createView(mHippyContext.getRenderManager().getControllerManager(), patchTypes);
			DiffUtils.doPatch(mHippyContext.getRenderManager().getControllerManager(), patchTypes);
		}

		viewHoler.mBindNode = toNode;

	}

	@Override
	public int getItemViewType(int position) {
		RecyclerView recyclerView = mParentRecyclerView.get();
		if (recyclerView == null) {
			return 0;
		}

		int parentId = recyclerView.getId();
		RenderNode parentNode = mHippyContext.getRenderManager().getRenderNode(parentId);
		if (parentNode != null && parentNode.getChildCount() > position) {
			RenderNode itemNode = parentNode.getChildAt(position);
			if (itemNode != null && itemNode.getProps() != null) {
				HippyMap listItemProps = itemNode.getProps();
				return listItemProps.getInt(ListItemRenderNode.ITEM_VIEW_TYPE);
			}
		}

		return super.getItemViewType(position);
	}

	public int getItemCount() {
		RecyclerView recyclerView = mParentRecyclerView.get();
		if (recyclerView != null) {
			RenderNode node = mHippyContext.getRenderManager().getRenderNode(recyclerView.getId());
			if (node != null) {
				int count = node.getChildCount();
				return count;
			}
		}

		return 0;
	}


}
