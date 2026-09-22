package com.haha.recyclerview;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.haha.baseui.mvvm.BaseMvvmActivity;
import com.haha.baseui.mvvm.BaseViewModel;
import com.haha.hahalearn.R;
import com.haha.hahalearn.databinding.ActivityRecyclerviewBinding;

/**
 * @author: haha
 * @date: 2025/4/11
 * Description: RecyclerViewActivity测试类
 **/
public class RecyclerViewActivity extends BaseMvvmActivity<ActivityRecyclerviewBinding, BaseViewModel> {
    int count = 500000;
    MyAdapter myAdapter;
    private RecyclerView recyclerView;

    @NonNull
    @Override
    protected Class<BaseViewModel> getViewModelClass() {
        return BaseViewModel.class;
    }

    @Override
    protected void initView() {
        recyclerView = findViewById(R.id.table);
        myAdapter = new MyAdapter(mContext);
        recyclerView.setAdapter(myAdapter);
    }

    @Override
    protected void initData() {

    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_recyclerview;
    }

    class MyAdapter implements RecyclerView.Adapter {
        private final int height;
        LayoutInflater inflater;

        public MyAdapter(Context context) {
            Resources resources = context.getResources();
            height = resources.getDimensionPixelSize(R.dimen.item_height);
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View onCreateViewHolder(int position, View contentView, ViewGroup parent) {
//            if (getItemViewType(position) == 1) {
//                contentView = inflater.inflate(R.layout.item_image, parent, false);
//            }
            int type = getItemViewType(position);
            if (type == 0) {
                contentView = inflater.inflate(R.layout.item_image, parent, false);
            } else {
                contentView = inflater.inflate(R.layout.item_table1, parent, false);
                TextView tv = contentView.findViewById(R.id.text1);
                tv.setText("第" + position + "行");
            }
            return contentView;
        }

        @Override
        public View onBinderViewHolder(int position, View contentView, ViewGroup parent) {
            int type = getItemViewType(position);
            if (type != 0) {
                TextView tv = contentView.findViewById(R.id.text1);
                tv.setText("第" + position + "行");
            }
            return contentView;
        }

        @Override
        public int getItemViewType(int row) {
            return row % 2;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getHeight(int index) {
            return height;
        }
    }
}
