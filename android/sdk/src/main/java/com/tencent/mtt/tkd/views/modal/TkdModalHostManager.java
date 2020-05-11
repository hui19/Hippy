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
package com.tencent.mtt.tkd.views.modal;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.tencent.mtt.hippy.annotation.HippyController;
import com.tencent.mtt.hippy.views.modal.HippyModalHostManager;
import com.tencent.mtt.hippy.views.modal.HippyModalHostView;
import com.tencent.mtt.hippy.views.modal.RequestCloseEvent;
import com.tencent.mtt.hippy.views.modal.ShowEvent;

@HippyController(name = TkdModalHostManager.CLASS_NAME)
public class TkdModalHostManager extends HippyModalHostManager
{
	public static final String CLASS_NAME	= "tkdModal";

	@Override
	protected View createViewImpl(Context context)
	{
		final TkdModalHostView hippyModalHostView = new TkdModalHostView(context);

		hippyModalHostView.setOnRequestCloseListener(new HippyModalHostView.OnRequestCloseListener()
		{
			@Override
			public void onRequestClose(DialogInterface dialog)
			{
				new RequestCloseEvent().send(hippyModalHostView, null);
			}
		});
		hippyModalHostView.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialog)
			{
				new ShowEvent().send(hippyModalHostView, null);
			}
		});

		return hippyModalHostView;
	}

}
