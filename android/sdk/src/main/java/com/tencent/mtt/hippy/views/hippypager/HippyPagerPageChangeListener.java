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

import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.viewpager.event.HippyPageItemExposureEvent;
import com.tencent.mtt.hippy.views.viewpager.event.HippyPageScrollEvent;
import com.tencent.mtt.hippy.views.viewpager.event.HippyPageScrollStateChangedEvent;
import com.tencent.mtt.hippy.views.viewpager.event.HippyPageSelectedEvent;

/**
 * Created  on 2021/7/23.
 */

public class HippyPagerPageChangeListener implements ViewPager.OnPageChangeListener {

    public static final String IDLE = "idle";
    public static final String DRAGGING = "dragging";
    public static final String SETTLING = "settling";
    private HippyPageScrollEvent mPageScrollEmitter;
    private HippyPageScrollStateChangedEvent mPageScrollStateChangeEmitter;
    private HippyPageSelectedEvent mPageSelectedEmitter;
    private int mLastPageIndex;
    private int mCurrPageIndex;
    private HippyPager mPager;

    public HippyPagerPageChangeListener(HippyPager pager) {
        mPager = pager;
        mPageScrollEmitter = new HippyPageScrollEvent(pager);
        mPageScrollStateChangeEmitter = new HippyPageScrollStateChangedEvent(pager);
        mPageSelectedEmitter = new HippyPageSelectedEvent(pager);
        mLastPageIndex = 0;
        mCurrPageIndex = 0;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mPageScrollEmitter.send(position, positionOffset);
    }

    @Override
    public void onPageSelected(int position) {
        Log.i("yogachen",
                mPager.getClass().getName() + " " + "onPageSelected send to hippy mCurrPageIndex=" + mCurrPageIndex
                        + " position=" + position);
        mCurrPageIndex = position;
        mPageSelectedEmitter.send(position);
        if (mPager != null) {
            View currView = mPager.getViewFromAdapter(mCurrPageIndex);
            HippyPageItemExposureEvent eventWillAppear = new HippyPageItemExposureEvent(
                    HippyPageItemExposureEvent.EVENT_PAGER_ITEM_WILL_APPEAR);
            eventWillAppear.send(currView, mCurrPageIndex);

            View lastView = mPager.getViewFromAdapter(mLastPageIndex);
            HippyPageItemExposureEvent eventWillDisAppear = new HippyPageItemExposureEvent(
                    HippyPageItemExposureEvent.EVENT_PAGER_ITEM_WILL_DISAPPEAR);
            eventWillDisAppear.send(lastView, mLastPageIndex);
        }
    }

    private void onScrollStateChangeToIdle() {
        Log.i("yogachen", mPager.getClass().getName() + " " + "onScrollStateChangeToIdle send to hippy mCurrPageIndex="
                + mCurrPageIndex + " mLastPageIndex=" + mLastPageIndex);
        if (mPager != null && mCurrPageIndex != mLastPageIndex) {
            Promise promise = mPager.getCallBackPromise();
            if (promise != null) {
                String msg = "on set index successful!";
                HippyMap resultMap = new HippyMap();
                resultMap.pushString("msg", msg);
                promise.resolve(resultMap);
                mPager.setCallBackPromise(null);
            }

            View currView = mPager.getViewFromAdapter(mCurrPageIndex);
            HippyPageItemExposureEvent eventWillAppear = new HippyPageItemExposureEvent(
                    HippyPageItemExposureEvent.EVENT_PAGER_ITEM_DID_APPEAR);
            eventWillAppear.send(currView, mCurrPageIndex);

            View lastView = mPager.getViewFromAdapter(mLastPageIndex);
            HippyPageItemExposureEvent eventWillDisAppear = new HippyPageItemExposureEvent(
                    HippyPageItemExposureEvent.EVENT_PAGER_ITEM_DID_DISAPPEAR);
            eventWillDisAppear.send(lastView, mLastPageIndex);

            mLastPageIndex = mCurrPageIndex;
        }
    }

    @Override
    public void onPageScrollStateChanged(int newState) {
        LogUtils.i("HippyPagerStateChanged", "onPageScrollStateChanged newState=" + newState);
        String pageScrollState;
        switch (newState) {
            case ViewPager.SCROLL_STATE_IDLE:
                pageScrollState = IDLE;
                onScrollStateChangeToIdle();
                break;
            case ViewPager.SCROLL_STATE_DRAGGING:
                pageScrollState = DRAGGING;
                break;
            case ViewPager.SCROLL_STATE_SETTLING:
                pageScrollState = SETTLING;
                break;
            default:
                throw new IllegalStateException("Unsupported pageScrollState");
        }

        mPageScrollStateChangeEmitter.send(pageScrollState);
    }
}
