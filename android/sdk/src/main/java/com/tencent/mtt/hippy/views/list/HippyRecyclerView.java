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

import android.icu.util.Measure;
import android.view.ViewGroup;
import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;

public class HippyRecyclerView extends RecyclerView implements HippyViewBase
{
	private HippyRecyclerAdapter				mListAdapter;
	private LayoutManager                       mLayoutManager;
	private Context								mContext;
	private HippyEngineContext					mHippyContext;
	private NativeGestureDispatcher				mGestureDispatcher;

    private void init(Context context, int orientation) {
        mHippyContext = ((HippyInstanceContext)context).getEngineContext();
		mLayoutManager = new LinearLayoutManager(context, orientation, false);
        this.setLayoutManager(mLayoutManager);
        mContext = context;
        mListAdapter = new HippyRecyclerAdapter(mHippyContext, this);
        setAdapter(mListAdapter);
    }

    public HippyRecyclerView(Context context, int orientation) {
        super(context);
        init(context, orientation);
    }

	public HippyRecyclerView(Context context) {
		super(context);
		init(context, LinearLayoutManager.VERTICAL);
	}

	public void onBatchComplete() {
		mListAdapter.notifyDataSetChanged();
        ViewGroup parent = (ViewGroup)getParent();
        if (parent != null) {
            measure(MeasureSpec.makeMeasureSpec(parent.getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(parent.getHeight(), MeasureSpec.EXACTLY));

            layout(parent.getLeft(), parent.getTop(), parent.getRight(), parent.getBottom());
        }
	}

	public ViewGroup.LayoutParams generateLayoutParams() {
		RecyclerView.LayoutParams lp = mLayoutManager.generateDefaultLayoutParams();
		return lp;
	}

	@Override
	public NativeGestureDispatcher getGestureDispatcher()
	{
		return mGestureDispatcher;
	}

	@Override
	public void setGestureDispatcher(NativeGestureDispatcher dispatcher) {
		this.mGestureDispatcher = dispatcher;
	}
}
