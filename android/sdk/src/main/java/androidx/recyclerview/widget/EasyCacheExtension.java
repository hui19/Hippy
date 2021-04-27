package androidx.recyclerview.widget;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView.Recycler;

/**
 * Created by niuniuyang on 2021/1/28.
 * Description
 * 主要用于截获getViewForPositionAndType函数，用于记录当前正在获取view的位置，用于
 * {@link HippyRecyclerPool#getRecycledView(int)} 用来在缓存池里面获取最近合适的ViewHolder
 */
public class EasyCacheExtension extends RecyclerView.ViewCacheExtension {

    private int currentPosition = -1;

    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public View getViewForPositionAndType(Recycler recycler, int position, int type) {
        currentPosition = position;
        return null;
    }
}
