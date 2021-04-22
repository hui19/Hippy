package android.support.v7.widget;

import android.support.v7.widget.RecyclerView.Recycler;
import android.view.View;

/**
 * Created by niuniuyang on 2021/1/28.
 * Description
 * 主要用于截获getViewForPositionAndType函数，用于记录当前正在获取view的位置，用于
 * {@link EasyRecyclerPool#getRecycledView(int)} 用来在缓存池里面获取最近合适的ViewHolder
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
