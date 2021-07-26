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

package com.tencent.mtt.hippy.views.hippypager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.hippypager.transform.VerticalPageTransformer;
import com.tencent.mtt.hippy.views.viewpager.HippyViewPagerItem;
import com.tencent.mtt.supportui.views.ScrollChecker;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class HippyPager extends ViewPager implements HippyViewBase {

    private static final String TAG = "HippyViewPager";

    private final Runnable mMeasureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    private NativeGestureDispatcher mGestureDispatcher;
    private boolean mScrollEnabled = true;
    private boolean mFirstUpdateChild = true;
    private HippyPagerPageChangeListener mPageListener;
    private String mOverflow;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Promise mCallBackPromise;
    private boolean mIsVertical = false;
    private Scroller scroller;
    private boolean ignoreCheck;

    public HippyPager(Context context, boolean isVertical) {
        super(context);
        mIsVertical = isVertical;
        init(context);
    }

    public HippyPager(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mPageListener = new HippyPagerPageChangeListener(this);
        addOnPageChangeListener(mPageListener);
        setAdapter(createAdapter(context));
        initViewPager();
        initScroller();
    }

    public int getCurrentPage() {
        return getCurrentItem();
    }


    protected void initViewPager() {
        if (mIsVertical) {
            setPageTransformer(true, new VerticalPageTransformer());
            // The easiest way to get rid of the overscroll drawing that happens on the left and right
            setOverScrollMode(OVER_SCROLL_NEVER);
        }
    }

    public int getPageCount() {
        return getAdapter() == null ? 0 : getAdapter().getCount();
    }

    public Object getCurrentItemView() {
        if (getAdapter() != null) {
            return getAdapter().getCurrentItemObj();
        }
        return null;
    }

    public void setCallBackPromise(Promise promise) {
        mCallBackPromise = promise;
    }

    public Promise getCallBackPromise() {
        return mCallBackPromise;
    }

    protected HippyPagerAdapter createAdapter(Context context) {
        return new HippyPagerAdapter((HippyInstanceContext) context, this);
    }

    public void setInitialPageIndex(final int index) {
        LogUtils.d(TAG, HippyPager.this.getClass().getName() + " " + "setInitialPageIndex=" + index);
        setCurrentItem(index);
        setDefaultItem(index);
    }

    public void setChildCountAndUpdate(final int childCount) {
        LogUtils.d(TAG, "doUpdateInternal: " + hashCode() + ", childCount=" + childCount);
        getAdapter().setChildSize(childCount);
        getAdapter().notifyDataSetChanged();
        triggerRequestLayout();
        if (mFirstUpdateChild) {
            mPageListener.onPageSelected(getCurrentItem());
            mFirstUpdateChild = false;
        }
    }

    protected void addViewToAdapter(HippyViewPagerItem view, int postion) {
        HippyPagerAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.addView(view, postion);
        }
    }

    protected int getAdapterViewSize() {
        HippyPagerAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.getItemViewSize();
        }
        return 0;
    }

    protected void removeViewFromAdapter(HippyViewPagerItem view) {
        HippyPagerAdapter adapter = getAdapter();
        if (adapter != null) {
            adapter.removeView(view);
        }
    }

    public View getViewFromAdapter(int currentItem) {
        HippyPagerAdapter adapter = getAdapter();
        if (adapter != null) {
            return adapter.getViewAt(currentItem);
        }
        return null;
    }

    @Override
    public HippyPagerAdapter getAdapter() {
        return (HippyPagerAdapter) super.getAdapter();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        resetIgnoreCheck(ev);
        if (!mScrollEnabled) {
            return false;
        }
        if (mIsVertical) {
            boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
            swapXY(ev); // return touch coordinates to original reference frame for any child views
            return intercepted;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        resetIgnoreCheck(ev);
        if (!mScrollEnabled) {
            return false;
        }
        if (mIsVertical) {
            return super.onTouchEvent(swapXY(ev));
        }
        return super.onTouchEvent(ev);
    }

    private void resetIgnoreCheck(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            ignoreCheck = false;
        }
    }

    public void switchToPage(int item, boolean animated) {
        // viewpager的children没有初始化好的时候，直接设置mInitialPageIndex
        if (getAdapter() == null || getAdapter().getCount() == 0) {
            setInitialPageIndex(item);
        } else {
            if (getCurrentItem() != item) {
                stopAnimationAndScrollToFinal();
                setCurrentItem(item, animated);
            } else if (!mFirstUpdateChild) {
                mPageListener.onPageSelected(item);
            }
        }
    }

    /**
     * 如果仍然在滑动中，重置一下状态，abortAnimation ,getScrollX 会处于mFinalX的状态，直接scrollTo到mFinalX
     */
    private void stopAnimationAndScrollToFinal() {
        if (!scroller.isFinished()) {
            invokeSetScrollingCacheEnabled(false);
            if (scroller != null) {
                scroller.abortAnimation();
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = scroller.getCurrX();
                int y = scroller.getCurrY();
                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                }
            }
            invokeSetScrollState(SCROLL_STATE_IDLE);
        }
    }

    public void setScrollEnabled(boolean scrollEnabled) {
        mScrollEnabled = scrollEnabled;
    }

    @Override
    public NativeGestureDispatcher getGestureDispatcher() {
        return mGestureDispatcher;
    }

    @Override
    public void setGestureDispatcher(NativeGestureDispatcher nativeGestureDispatcher) {
        mGestureDispatcher = nativeGestureDispatcher;
    }

    public void triggerRequestLayout() {
        mHandler.post(mMeasureAndLayout);
    }

    public void setOverflow(String overflow) {
        mOverflow = overflow;
        //robinsli Android 支持 overflow: visible，超出容器之外的属性节点也可以正常显示
        if (!TextUtils.isEmpty(mOverflow)) {
            switch (mOverflow) {
                case "visible":
                    setClipChildren(false); //可以超出父亲区域
                    break;
                case "hidden": {
                    setClipChildren(true); //默认值是false
                    break;
                }
            }
        }
        invalidate();
    }

    public void onOverScrollSuccess() {
        invokeSetIsUnableToDrag(false);
        ignoreCheck = true;
    }


    /**
     * viewpPager的孩子已经滚动到底了，已经不能继续滚动了，会触发通过onOverScroll事件，告诉
     * viewPager的进行继续，需要执行onOverScrollSuccess，让viewPager开始滚动
     * 会让mIsUnableToDrag设置为false，ignoreCheck 表示不在进行孩子的判断，有些孩子没有正确实现canScroll，
     * 用ignoreCheck的值来忽略孩子的滚动，这是一种兼容老代码的逻辑，按道理来说，ignoreCheck应该不需要
     *
     * @return
     */
    public boolean onOverScroll(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
            int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        if (mIsVertical) {
            if (((scrollY == 0 && deltaY < 0) || (scrollY == scrollRangeY && deltaY > 0))) {
                onOverScrollSuccess();
            }
        } else {
            if (((scrollX == 0 && deltaX < 0) || (scrollX == scrollRangeX && deltaX > 0))) {
                onOverScrollSuccess();
            }
        }
        return true;
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (ignoreCheck) {
            return false;
        }
        return ScrollChecker.canScroll(v, checkV, mIsVertical, dx, x, y);
    }

    private MotionEvent swapXY(MotionEvent ev) {
        float width = getWidth();
        float height = getHeight();

        float newX = (ev.getY() / height) * width;
        float newY = (ev.getX() / width) * height;

        ev.setLocation(newX, newY);

        return ev;
    }


    /**
     * hook 方法，不建议调用，这里只是为了兼容,目的是为了触发一次firstLayout恢复状态
     *
     * @param isFirstLayout
     */
    private void setFirstLayout(boolean isFirstLayout) {
        try {
            Field field = ViewPager.class.getDeclaredField("mFirstLayout");
            field.setAccessible(true);
            field.set(this, isFirstLayout);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 也是Hack方法，设置初始化index
     *
     * @param position
     */
    private void setDefaultItem(int position) {
        try {
            Field field = ViewPager.class.getDeclaredField("mCurItem");
            field.setAccessible(true);
            field.setInt(this, position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initScroller() {
        try {
            Field velocityTrackerField = ViewPager.class.getDeclaredField("mScroller");
            velocityTrackerField.setAccessible(true);
            scroller = (Scroller) velocityTrackerField.get(this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void invokeSetIsUnableToDrag(boolean enabled) {
        try {
            Field field = ViewPager.class.getDeclaredField("mIsUnableToDrag");
            field.setAccessible(true);
            field.set(this, enabled);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void invokeSetScrollingCacheEnabled(boolean enabled) {
        try {
            Method method = ViewPager.class.getDeclaredMethod("setScrollingCacheEnabled", Boolean.class);
            method.setAccessible(true);
            method.invoke(this, enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void invokeSetScrollState(int state) {
        try {
            Method method = ViewPager.class.getDeclaredMethod("setScrollState", Integer.class);
            method.setAccessible(true);
            method.invoke(this, state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
