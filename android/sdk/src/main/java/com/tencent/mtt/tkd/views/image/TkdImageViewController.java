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
package com.tencent.mtt.tkd.views.image;

import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.views.image.HippyImageViewController;

import android.content.Context;
import android.view.View;


@HippyController(name = TkdImageViewController.CLASS_NAME)
public class TkdImageViewController extends HippyImageViewController
{
	public static final String	CLASS_NAME	= "tkdImage";

	@Override
	protected View createViewImpl(Context context, HippyMap iniProps)
	{
    TkdImageView imageView = new TkdImageView(context);
		if (iniProps != null) {
			imageView.setInitProps(iniProps);
		}

		return imageView;
	}

	@Override
	protected View createViewImpl(Context context)
	{
		return new TkdImageView(context);
	}
}
