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
package com.tencent.mtt.hippy.views.modal;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.HippyInstanceLifecycleEventListener;
import com.tencent.mtt.hippy.utils.ContextHolder;
import com.tencent.mtt.hippy.views.view.HippyViewGroup;

import java.lang.reflect.Field;
import java.util.ArrayList;



public class HippyModalHostView extends HippyViewGroup implements HippyInstanceLifecycleEventListener
{
	@Override
	public void onInstanceLoad(int instanceId)
	{
        showOrUpdate();
	}

	@Override
	public void onInstanceResume(int instanceId)
	{
			showOrUpdate();
	}

	@Override
	public void onInstancePause(int instanceId)
	{
			dismiss();
	}

	@Override
	public void onInstanceDestroy(int instanceId)
	{
			HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) getContext();
			hippyInstanceContext.getEngineContext().removeInstanceLifecycleEventListener(this);
			dismiss();
	}

	public interface OnRequestCloseListener
	{
		void onRequestClose(DialogInterface dialog);
	}

	private final int STYLE_THEME_FULL_SCREEN_DIALOG     = 0;
  private final int STYLE_THEME_ANIMATED_FADE_DIALOG   = 1;
  private final int STYLE_THEME_ANIMATED_SLIDE_DIALOG  = 2;

	private DialogRootViewGroup				mHostView;
	private Dialog							mDialog;
	private View							mContentView;
	private boolean							mTransparent	= true;
	private String							mAnimationType;
	private boolean							mPropertyRequiresNewDialog;
	private DialogInterface.OnShowListener	mOnShowListener;
	OnRequestCloseListener					mOnRequestCloseListener;
	private int mAniType ;
	private boolean							mEnterImmersionStatusBar = false;
	private boolean	 						mStatusBarTextDarkColor = false;

	public HippyModalHostView(Context context)
	{
		super(context);

		HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) context;
		hippyInstanceContext.getEngineContext().addInstanceLifecycleEventListener(this);


		mHostView = new DialogRootViewGroup(context);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{

	}

	@Override
	public void addView(View child, int index)
	{
		mHostView.addView(child, index);
	}

	@Override
	public int getChildCount()
	{
		return mHostView.getChildCount();
	}

	@Override
	public View getChildAt(int index)
	{
		return mHostView.getChildAt(index);
	}

	@Override
	public void removeView(View child)
	{
		mHostView.removeView(child);
	}

	@Override
	public void removeViewAt(int index)
	{
		View child = getChildAt(index);
		mHostView.removeView(child);
	}

	public void addChildrenForAccessibility(ArrayList<View> outChildren)
	{

	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event)
	{
		return false;
	}


	private void dismiss()
	{
		if (isActivityFinishing()) {
			return;
		}

		if (mDialog != null)
		{
			mDialog.dismiss();
			mDialog = null;
			ViewGroup parent = (ViewGroup) mHostView.getParent();
			parent.removeViewAt(0);
		}
	}

	public void setOnRequestCloseListener(OnRequestCloseListener listener)
	{
		mOnRequestCloseListener = listener;
	}

	public void requestClose()
	{
		if(mOnRequestCloseListener != null)
		{
			mOnRequestCloseListener.onRequestClose(mDialog);
		}
	}

	public void setOnShowListener(DialogInterface.OnShowListener listener)
	{
		mOnShowListener = listener;
	}

	protected void setTransparent(boolean transparent)
	{
		mTransparent = transparent;
	}

	protected void setAnimationType(String animationType)
	{
		mAnimationType = animationType;
		mPropertyRequiresNewDialog = true;
	}

	protected void setEnterImmersionStatusBar(boolean fullScreen)
	{
		mEnterImmersionStatusBar = fullScreen;
	}

	protected void setImmersionStatusBarTextDarkColor(boolean darkColor)
	{
		mStatusBarTextDarkColor = darkColor;
	}
	public Dialog getDialog()
	{
		return mDialog;
	}

	static int		mStatusBarHeight		= -1;
	static boolean	hasCheckStatusBarHeight	= false;
	public int getStatusBarHeightFixed()
	{
		if (mStatusBarHeight == -1)
		{
			mStatusBarHeight = getStatusBarHeightFromSystem();

			hasCheckStatusBarHeight = true;
		}
		return mStatusBarHeight;
	}
	private static int statusBarHeight = -1;

	public static int getStatusBarHeightFromSystem()
	{
		if (statusBarHeight > 0)
		{
			return statusBarHeight;
		}

		Class<?> c = null;
		Object obj = null;
		Field field = null;
		int x = 0;
		try
		{
			c = Class.forName("com.android.internal.R$dimen");
			obj = c.newInstance();
			field = c.getField("status_bar_height");
			x = Integer.parseInt(field.get(obj).toString());
			statusBarHeight = ContextHolder.getAppContext().getResources().getDimensionPixelSize(x);
		}
		catch (Exception e1)
		{
			statusBarHeight = -1;
			e1.printStackTrace();
		}
		if (statusBarHeight < 1)
		{
			try
			{
				int statebarH_id = ContextHolder.getAppContext().getResources().getIdentifier("statebar_height", "dimen",
						ContextHolder.getAppContext().getPackageName());
				statusBarHeight = Math.round(ContextHolder.getAppContext().getResources().getDimension(statebarH_id));
			}
			catch (Exception e)
			{
				statusBarHeight = -1;
				e.printStackTrace();
			}
		}
		return statusBarHeight;
	}
	public void setDialogBar( boolean isDarkIcon)
	{
		try
		{
			Window window = mDialog.getWindow();
			int sysUI = window.getDecorView().getSystemUiVisibility();
			sysUI = sysUI & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
			sysUI = sysUI & ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			int extra = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			if (isDarkIcon)
			{
				extra = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
			}
			else
			{
				extra = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			}
			window.getDecorView().setSystemUiVisibility(sysUI | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| extra);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				window.setStatusBarColor(Color.TRANSPARENT);
			}
			else
			{
				window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
				window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			}
		}
		catch (Throwable throwable)
		{
			throwable.printStackTrace();
		}
	}

	private boolean isActivityFinishing() {
		HippyInstanceContext hippyInstanceContext = (HippyInstanceContext)getContext();
		if (hippyInstanceContext == null){
			 return true;
		}

		Context context = hippyInstanceContext.getBaseContext();
		if (context == null || !(context instanceof Activity)) {
			return true;
		}

		Activity currentActivity = (Activity)context;
		if (currentActivity.isFinishing()) {
			return true;
		}

		return false;
	}

	protected void showOrUpdate()
	{
		if (isActivityFinishing()) {
			return;
		}

		if (mDialog != null)
		{
			if (mPropertyRequiresNewDialog)
			{
				dismiss();
			}
			else
			{
				updateProperties();
				return;
			}
		}

		mPropertyRequiresNewDialog = false;
		mAniType = STYLE_THEME_FULL_SCREEN_DIALOG;
		if (!TextUtils.isEmpty(mAnimationType) && mAnimationType.equals("fade"))
		{
			mAniType = STYLE_THEME_ANIMATED_FADE_DIALOG;
		}
		else if (!TextUtils.isEmpty(mAnimationType) && mAnimationType.equals("slide"))
		{
			mAniType = STYLE_THEME_ANIMATED_SLIDE_DIALOG;
		}
		Context currentContext = getContext();

		mDialog = createDialog(currentContext);

		mContentView = createContentView(mHostView);
		mDialog.setContentView(mContentView);
		updateProperties();
		if(mDialog != null && mDialog.getWindow() != null && mEnterImmersionStatusBar)
		{
			setDialogBar(mStatusBarTextDarkColor);
		}

		mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				mOnShowListener.onShow(dialogInterface);
				if(mAniType == STYLE_THEME_ANIMATED_FADE_DIALOG)
				{
					ObjectAnimator mAlphaAnimation = ObjectAnimator.ofFloat(mContentView, "alpha", 0.0f, 1.0f);
					mAlphaAnimation.setDuration(200);
					mAlphaAnimation.start();
				}
				else if(mAniType == STYLE_THEME_ANIMATED_SLIDE_DIALOG)
				{
					ObjectAnimator mAlphaAnimation = ObjectAnimator.ofFloat(mContentView, "translationY", 0);
					mAlphaAnimation.setDuration(200);
					mAlphaAnimation.start();
				}
			}
		});
		mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialogInterface) {
			}
		});
		mDialog.setOnKeyListener(new DialogInterface.OnKeyListener()
		{
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_UP)
				{

 					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
						mOnRequestCloseListener.onRequestClose(dialog);
						return true;
					}
					else
					{
						if(((HippyInstanceContext)getContext()).getBaseContext() instanceof Activity)
						{
							Activity currentActivity  = (Activity) ((HippyInstanceContext)getContext()).getBaseContext();
							if (currentActivity != null)
							{
								return currentActivity.onKeyUp(keyCode, event);
							}
						}
					}
				}
				return false;
			}
		});

		mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		mDialog.show();
		if(mAniType == STYLE_THEME_ANIMATED_FADE_DIALOG)
		{
			mContentView.setAlpha(0);
		}
		else if(mAniType == STYLE_THEME_ANIMATED_SLIDE_DIALOG)
		{
			int nScreenHeight = getScreenHeight();
			if (nScreenHeight != -1)
				mContentView.setTranslationY(nScreenHeight);
		}
	}
	private int getScreenHeight()
	{
		try
		{
			Context context = ContextHolder.getAppContext();
			android.view.WindowManager manager = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			Display display = manager.getDefaultDisplay();
			if (display != null)
			{
				int height = manager.getDefaultDisplay().getHeight();
				return height;
			}
		}
		catch (SecurityException e)
		{
		}
		return -1;
	}

	protected Dialog createDialog(Context context)
	{
		int theme = 0;
		if (context != null) {
			Resources res = context.getResources();
			theme = res.getIdentifier("HippyFullScreenDialog", "style", context.getPackageName());
		}

		Dialog dialog = new Dialog(context, theme);
		if (theme == 0) {
			Window window = dialog.getWindow();
			if (window != null) {
				window.requestFeature(Window.FEATURE_NO_TITLE);
				window.setBackgroundDrawableResource(android.R.color.transparent);
				window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams
						.MATCH_PARENT);
			}
		}

		return dialog;
	}

	protected String getAnimationType()
	{
		return mAnimationType;
	}


	protected View createContentView(View hostView)
	{
		FrameLayout frameLayout = new FrameLayout(getContext()){
			@Override
			protected void dispatchDraw(Canvas canvas) {
				super.dispatchDraw(canvas);
				if (mEnterImmersionStatusBar && mStatusBarHeight != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
				{
					canvas.save();
					canvas.clipRect(0, 0, getMeasuredWidth(), mStatusBarHeight);
					canvas.drawColor(0x40000000);
					canvas.restore();
				}
			}
		};
		if (mEnterImmersionStatusBar && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
		{
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			params.topMargin = -1* getStatusBarHeightFixed();
			frameLayout.addView(hostView, params);
		}
		else
		{
		frameLayout.addView(hostView);
		}
		frameLayout.setFitsSystemWindows(false);
		return frameLayout;
	}

	private void updateProperties()
	{
		if (mTransparent)
		{
			mDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}
		else
		{
			mDialog.getWindow().setDimAmount(0.5f);
			mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}
	}

	static class DialogRootViewGroup extends HippyViewGroup
	{

		public DialogRootViewGroup(Context context)
		{
			super(context);
			setFitsSystemWindows(false);
		}

		@Override
		protected void onSizeChanged(final int w, final int h, int oldw, int oldh)
		{
			super.onSizeChanged(w, h, oldw, oldh);
			if (getChildCount() > 0)
			{
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
				{
					getChildAt(0).layout(getChildAt(0).getLeft(), getChildAt(0).getTop(), getChildAt(0).getLeft() + w, getChildAt(0).getTop() + h);
				}

				HippyInstanceContext hippyInstanceContext = (HippyInstanceContext) getContext();
				if(hippyInstanceContext != null && hippyInstanceContext.getEngineContext() != null)
				{
					final HippyEngineContext engineContext = hippyInstanceContext.getEngineContext();
					if(engineContext.getThreadExecutor() != null)
					{
						final int id = getChildAt(0).getId();
						final int width = w;
						final int height = h;
						engineContext.getThreadExecutor().postOnDomThread(new Runnable()
						{
							@Override
							public void run()
							{
								if (engineContext != null && engineContext.getDomManager() != null)
								{
									engineContext.getDomManager().updateNodeSize(id, width, height);
								}
							}
						});

					}

				}
			}
		}

		@Override
		public void requestDisallowInterceptTouchEvent(boolean disallowIntercept)
		{

		}


	}
}
