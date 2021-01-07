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
