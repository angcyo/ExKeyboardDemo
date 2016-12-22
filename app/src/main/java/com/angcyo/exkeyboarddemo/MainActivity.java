package com.angcyo.exkeyboarddemo;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RSoftInputLayout mSoftInputLayout;
    ArrayList<String> datas = new ArrayList<>();
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSoftInputLayout = (RSoftInputLayout) findViewById(R.id.soft_input_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        initContentLayout();
    }

    @Override
    public void onBackPressed() {
        if (mSoftInputLayout.requestBackPressed()) {
            super.onBackPressed();
        }
    }

    protected void initContentLayout() {
        mSoftInputLayout.addOnEmojiLayoutChangeListener(new RSoftInputLayout.OnEmojiLayoutChangeListener() {
            @Override
            public void onEmojiLayoutChange(boolean isEmojiShow, boolean isKeyboardShow, int height) {
                Log.w("Robi", "表情显示:" + mSoftInputLayout.isEmojiShow() + " 键盘显示:" + mSoftInputLayout.isKeyboardShow()
                        + " 表情高度:" + mSoftInputLayout.getShowEmojiHeight() + " 键盘高度:" + mSoftInputLayout.getKeyboardHeight());
                String log = "表情显示:" + isEmojiShow + " 键盘显示:" + isKeyboardShow + " 高度:" + height;
                Log.e("Robi", log);
                datas.add(log);
                mRecyclerView.getAdapter().notifyItemInserted(datas.size());
                mRecyclerView.smoothScrollToPosition(datas.size());
            }
        });
        datas.add("内容顶部");
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                TextView textView = new TextView(MainActivity.this);
                return new RecyclerView.ViewHolder(textView) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ((TextView) holder.itemView).setText(datas.get(position));
            }

            @Override
            public int getItemCount() {
                return datas.size();
            }
        });
    }

    public void onSetPadding100Click(View view) {
        mSoftInputLayout.showEmojiLayout(dpToPx(100));
    }

    public void onSetPadding400Click(View view) {
        mSoftInputLayout.showEmojiLayout(dpToPx(400));
    }

    public void onShowClick(View view) {
        mSoftInputLayout.showEmojiLayout();
    }

    public void onHideClick(View view) {
        mSoftInputLayout.hideEmojiLayout();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void onTest(View view) {
        FullscreenActivity.launcher(this);
    }

    public void onLayoutFullScreen(View view) {
        enableLayoutFullScreen();
        mSoftInputLayout.requestLayout();
        mSoftInputLayout.post(new Runnable() {
            @Override
            public void run() {
                mSoftInputLayout.requestLayout();
            }
        });
    }

    protected void enableLayoutFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

}
