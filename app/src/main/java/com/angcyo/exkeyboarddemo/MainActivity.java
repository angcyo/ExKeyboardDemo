package com.angcyo.exkeyboarddemo;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    Control mControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mControl = new Control((RecyclerView) findViewById(R.id.recycler_view),
                (RSoftInputLayout2) findViewById(R.id.soft_input_layout),
                this);
        findViewById(R.id.padd100).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetPadding100Click(v);
            }
        });
        findViewById(R.id.padd400).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSetPadding400Click(v);
            }
        });
        findViewById(R.id.show_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShowClick(v);
            }
        });
        findViewById(R.id.hide_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHideClick(v);
            }
        });
        findViewById(R.id.onTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTest(v);
            }
        });
        findViewById(R.id.dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inDialog(v);
            }
        });
        findViewById(R.id.layout_full).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLayoutFullScreen(v);
            }
        });
        mControl.initContentLayout();
    }

    @Override
    public void onBackPressed() {
        if (mControl.onBackPressed()) {
            super.onBackPressed();
        }
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
        FullscreenActivity.launcher(this);
    }

    public void onLayoutFullScreen(View view) {
        enableLayoutFullScreen();
        mControl.onLayoutFullScreen();
    }

    public void inDialog(View view) {
        new DialogDemo().show(getSupportFragmentManager(), "dialog");
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
