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

import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.annotation.HippyControllerProps;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.uimanager.ControllerManager;
import com.tencent.mtt.hippy.uimanager.HippyViewController;
import com.tencent.mtt.hippy.uimanager.ListViewRenderNode;
import com.tencent.mtt.hippy.uimanager.RenderNode;
import android.support.v7.widget.LinearLayoutManager;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

@HippyController(name = HippyRecyclerViewController.CLASS_NAME)
public class HippyRecyclerViewController extends HippyViewController<HippyRecyclerView>
{
	public static final String CLASS_NAME = "ListView";


//	@Override
//	protected void deleteChild(ViewGroup parentView, View childView, int childIndex)
//	{
//		// List的childView是RecyclerViewItem类型，不是由Hippy构建的，所以这里需要提前删除RecyclerViewItem的child
//		if (childView instanceof RecyclerViewItem)
//		{
//			((RecyclerViewItem) childView).removeAllViews();
//		}
//		// list里，删掉某个条目后，它后面的条目的位置都要减1
//		if (childIndex >= 0 && parentView instanceof HippyListView)
//		{
//			HippyListView listView = (HippyListView) parentView;
//			listView.getRecycler().updateHolderPositionWhenDelete(childIndex);
//		}
//	}

//	@Override
//	public int getChildCount(HippyRecyclerView viewGroup) {
//		return ((HippyRecyclerAdapter)viewGroup.getAdapter()).getRecyclerItemCount();
//	}
//
//	@Override
//	public View getChildAt(HippyRecyclerView viewGroup, int i) {
//		return ((HippyRecyclerAdapter) viewGroup.getAdapter()).getRecyclerItemView(i);
//	}

	@Override
	public void onBatchComplete(HippyRecyclerView view) {
		super.onBatchComplete(view);
		view.onBatchComplete();
	}

	@Override
	protected View createViewImpl(Context context) {
		return new HippyRecyclerView(context, LinearLayoutManager.VERTICAL);
	}

	@Override
	protected View createViewImpl(Context context, HippyMap iniProps) {
		if (iniProps != null && iniProps.containsKey("horizontal")) {
			return new HippyRecyclerView(context, LinearLayoutManager.HORIZONTAL);
		} else {
			return new HippyRecyclerView(context, LinearLayoutManager.VERTICAL);
		}
	}

	@Override
	public RenderNode createRenderNode(int id, HippyMap props, String className, HippyRootView hippyRootView, ControllerManager controllerManager,
			boolean lazy) {
		return new ListViewRenderNode(id, props, className, hippyRootView, controllerManager, lazy);
	}

}
