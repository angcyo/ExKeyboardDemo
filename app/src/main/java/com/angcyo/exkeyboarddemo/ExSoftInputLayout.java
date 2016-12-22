package com.angcyo.exkeyboarddemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：
 * 创建人员：Robi
 * 创建时间：2016/12/21 9:01
 * 修改人员：Robi
 * 修改时间：2016/12/21 9:01
 * 修改备注：
 * Version: 1.0.0
 */
public class ExSoftInputLayout extends ViewGroup {

    private static final String TAG = "Robi";
    View contentLayout;
    View emojiLayout;
    /**
     * 缺省的键盘高度
     */
    int keyboardHeight;

    /**
     * 需要显示的键盘高度,可以指定显示多高
     */
    int showKeyboardHeight;

    boolean isEmojiShow = false;

    /**
     * 是否需要显示表情布局, 当键盘已经弹出, 切换表情时, 需要锁定paddingBottom值, 否则表情布局无法显示
     */
    boolean requestShowEmojiLayout = false;

    int lastPaddingBottom = 0;

    HashSet<OnEmojiLayoutChangeListener> mEmojiLayoutChangeListeners = new HashSet<>();

    /**
     * 键盘是否显示
     */
    private boolean isKeyboardShow = false;

    public ExSoftInputLayout(Context context) {
        super(context);
    }

    public ExSoftInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExSoftInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ExSoftInputLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getChildCount() != 2) {
            throw new IllegalArgumentException("必须含有2个子View.");
        }
        /*请按顺序布局*/
        contentLayout = getChildAt(0);
        emojiLayout = getChildAt(1);

//        Activity activity = (Activity) getContext();
//        Window window = activity.getWindow();
//        ((ViewGroup) window.getDecorView()).getChildAt(0).setFitsSystemWindows(false);
//        ((ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT)).getChildAt(0).setFitsSystemWindows(false);
//        ((View) getParent()).setFitsSystemWindows(false);

        setFitsSystemWindows(true);
        setClipToPadding(false);

        if (keyboardHeight == 0) {
            keyboardHeight = (int) (getResources().getDisplayMetrics().density * 200);
        }

        //fix(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        contentLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightSize - getShowPaddingBottom(), MeasureSpec.EXACTLY));
        emojiLayout.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getShowPaddingBottom(), MeasureSpec.EXACTLY));
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingBottom = getShowPaddingBottom();

        if ((lastPaddingBottom != 0 || paddingBottom != 0) && lastPaddingBottom != paddingBottom) {
            int oldBottom = lastPaddingBottom;
            lastPaddingBottom = paddingBottom;
            notifyEmojiLayoutChangeListener(isEmojiShow, isKeyboardShow, oldBottom, paddingBottom);
        }

        contentLayout.layout(l, t, r, b - paddingBottom);
        emojiLayout.layout(l, b - paddingBottom, r, b);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        Log.w(TAG, "fitSystemWindows: ");
        int oldBottom = getPaddingBottom();
        boolean result = super.fitSystemWindows(insets);
        int newBottom = getPaddingBottom();

        if (newBottom > 0) {
            keyboardHeight = newBottom;
            isKeyboardShow = true;
        } else {
            isKeyboardShow = false;
        }

        boolean notify = false;
        if (showKeyboardHeight == newBottom) {
            //当2次设置的padding值一样时, onLayout方法不会执行, 所有需要在此方法中通知监听者事件的变化
            notify = true;
        } else {
            if (showKeyboardHeight == oldBottom && !isKeyboardShow && requestShowEmojiLayout) {
                //键盘隐藏了, 并且切换到相同相同键盘高度的Emoji布局(而且高度和上一次的Emoji布局高度还不一样)
                notify = true;
            }
        }
        if (requestShowEmojiLayout) {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), showKeyboardHeight);
        } else {
            if (isKeyboardShow) {
                isEmojiShow = false;
            }
        }

        requestShowEmojiLayout = false;

        if (notify) {
            notifyEmojiLayoutChangeListener(isEmojiShow, isKeyboardShow, oldBottom, newBottom);
        }

        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.w(TAG, "onSizeChanged: " + w + " " + h + " " + oldw + " " + oldh);
        super.onSizeChanged(w, h, oldw, oldh);

        test(this);
    }

    private void test(View view) {
        Activity activity = (Activity) getContext();
        Window window = activity.getWindow();
        View viewById = window.findViewById(Window.ID_ANDROID_CONTENT);
        ViewParent parent = view.getParent();
        if (parent != null) {
            if (parent instanceof View) {
                View p = (View) parent;
                int paddingBottom = p.getPaddingBottom();
                if (paddingBottom != 0) {
                    Log.e(TAG, "test: " + parent.getClass().getSimpleName());
                } else {
                    test(p);
                }
            }
        }
    }

    private void fix(View view) {
        Activity activity = (Activity) getContext();
        Window window = activity.getWindow();
        View viewById = window.findViewById(Window.ID_ANDROID_CONTENT);
        ViewParent parent = view.getParent();
        if (parent != null) {
            if (parent instanceof View) {
                View p = (View) parent;
                p.setFitsSystemWindows(false);
                fix(p);
            }
        }
        ((ViewGroup) viewById).getChildAt(0).setFitsSystemWindows(false);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        Log.w(TAG, "onApplyWindowInsets: ");
        return super.onApplyWindowInsets(insets);
    }

    private int getShowPaddingBottom() {
        if (requestShowEmojiLayout) {
            return showKeyboardHeight;
        }
        return super.getPaddingBottom();
    }

    private void showEmojiLayoutInner(int height) {
        isEmojiShow = true;
        showKeyboardHeight = height;
        requestShowEmojiLayout = isKeyboardShow;
        if (isKeyboardShow) {
            hideSoftInput();
        } else {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), showKeyboardHeight);
        }
    }

    /**
     * 显示表情布局
     *
     * @param height 指定表情需要显示的高度
     */
    public void showEmojiLayout(final int height) {
        showEmojiLayoutInner(height);
    }

    /**
     * 采用默认的键盘高度显示表情, 如果键盘从未弹出过, 则使用一个缺省的高度
     */
    public void showEmojiLayout() {
        showEmojiLayoutInner(keyboardHeight);
    }

    /**
     * 采用默认的键盘高度显示表情, 如果键盘从未弹出过, 则使用一个缺省的高度
     */
    public void hideEmojiLayout() {
        isEmojiShow = false;
        requestShowEmojiLayout = false;
        test(this);
        if (isKeyboardShow) {
            hideSoftInput();
        } else {
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), 0);
        }
    }

    public void addOnEmojiLayoutChangeListener(OnEmojiLayoutChangeListener listener) {
        mEmojiLayoutChangeListeners.add(listener);
    }

    public void removeOnEmojiLayoutChangeListener(OnEmojiLayoutChangeListener listener) {
        mEmojiLayoutChangeListeners.remove(listener);
    }

    private void notifyEmojiLayoutChangeListener(boolean isEmojiShow, boolean isKeyboardShow, int oldHeight, int height) {
        Iterator<OnEmojiLayoutChangeListener> iterator = mEmojiLayoutChangeListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onEmojiLayoutChange(isEmojiShow, isKeyboardShow, oldHeight, height);
        }
    }

    public void hideSoftInput() {
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    public void showSoftInput() {
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.showSoftInputFromInputMethod(getWindowToken(), 0);
    }

    public int getKeyboardHeight() {
        return keyboardHeight;
    }

    public boolean isEmojiShow() {
        return isEmojiShow;
    }

    public boolean isKeyboardShow() {
        return isKeyboardShow;
    }

    public int getShowKeyboardHeight() {
        return showKeyboardHeight;
    }

    public interface OnEmojiLayoutChangeListener {
        /**
         * @param oldHeight      之前, EmojiLayout弹出的高度 或者 键盘弹出的高度
         * @param height         EmojiLayout弹出的高度 或者 键盘弹出的高度
         * @param isEmojiShow    表情布局是否显示了
         * @param isKeyboardShow 键盘是否显示了
         */
        void onEmojiLayoutChange(boolean isEmojiShow, boolean isKeyboardShow, int oldHeight, int height);
    }
}
