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


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Created by niuniuyang on 2020/10/14.
 *
 * 由于Hippy的特殊需求，需要看到更多的RecyclerVew的方法和成员，这里创建和系统RecyclerView同包名。
 */

public class HippyRecyclerViewBase extends EasyRecyclerView {

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
}
