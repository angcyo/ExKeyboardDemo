package com.angcyo.exkeyboarddemo;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：
 * 创建人员：Robi
 * 创建时间：2016/12/22 17:28
 * 修改人员：Robi
 * 修改时间：2016/12/22 17:28
 * 修改备注：
 * Version: 1.0.0
 */
public class DialogDemo extends DialogFragment {

    Control mControl;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //View inflate = inflater.inflate(R.layout.activity_main, container, false);
        View inflate = inflater.inflate(R.layout.dialog_main, (ViewGroup) getDialog().getWindow().findViewById(Window.ID_ANDROID_CONTENT), false);

        RSoftInputLayout softInputLayout = (RSoftInputLayout) inflate.findViewById(R.id.soft_input_layout);
        mControl = new Control((RecyclerView) inflate.findViewById(R.id.recycler_view),
                softInputLayout,
                getActivity());
        mControl.initContentLayout();

        inflate.findViewById(R.id.padd100).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetPadding100Click(v);
            }
        });
        inflate.findViewById(R.id.padd400).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetPadding400Click(v);
            }
        });
        inflate.findViewById(R.id.show_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShowClick(v);
            }
        });
        inflate.findViewById(R.id.hide_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHideClick(v);
            }
        });
        inflate.findViewById(R.id.onTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTest(v);
            }
        });
        inflate.findViewById(R.id.dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inDialog(v);
            }
        });

        initWindow();
        //enableLayoutFullScreen();

        return inflate;
    }

    protected void initWindow() {
        final Dialog dialog = getDialog();
        Window window = dialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams mWindowAttributes = window.getAttributes();
        mWindowAttributes.width = -1;//这个属性需要配合透明背景颜色,才会真正的 MATCH_PARENT
        mWindowAttributes.height = -1;
        //mWindowAttributes.gravity = getGravity();
        window.setAttributes(mWindowAttributes);

        View view = window.findViewById(Window.ID_ANDROID_CONTENT);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = -1;
        layoutParams.width = -1;
        view.setLayoutParams(layoutParams);
    }

    public void onSetPadding100Click(View view) {
        mControl.onSetPadding100Click();
    }

    public void onSetPadding400Click(View view) {
        mControl.onSetPadding400Click();
    }

    public void onShowClick(View view) {
        mControl.onShowClick();
    }

    public void onHideClick(View view) {
        mControl.onHideClick();
    }

    public void onTest(View view) {
        FullscreenActivity.launcher(getActivity());
    }

    public void inDialog(View view) {
        new DialogDemo().show(getChildFragmentManager(), "dialog");
    }

    /**
     * 对话框中, 透明状态有问题
     */
    protected void enableLayoutFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getDialog().getWindow();
            //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
            //window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            //      | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
