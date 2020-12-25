package com.tencent.mtt.hippy.views.hippylist;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.tencent.mtt.hippy.uimanager.RenderNode;

/**
 * Created by niuniuyang on 2020/12/22.
 * Description
 */
public class HippyRecyclerViewHolder extends ViewHolder {

  public RenderNode bindNode;
  public boolean isCreated = true;

  public HippyRecyclerViewHolder(@NonNull View itemView, RenderNode renderNode) {
    super(itemView);
    bindNode = renderNode;
  }
}
