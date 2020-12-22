package com.tencent.mtt.hippy.views.list;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import android.support.v7.widget.RecyclerView;

public class HippyRecyclerViewItem extends FrameLayout
{
	protected ViewGroup mParent;
	public RecyclerView.ViewHolder	        mHolder;
	public View								mContentView;

	public HippyRecyclerViewItem(Context context, ViewGroup recyclerView) {
		super(context);
		mParent = recyclerView;
	}
}
