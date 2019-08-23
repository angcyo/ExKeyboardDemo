package com.angcyo.exkeyboarddemo;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Copyright (C) 2016,深圳市红鸟网络科技股份有限公司 All rights reserved.
 * 项目名称：
 * 类的描述：支持透明状态栏, 支持Dialog, 支持动画
 * <p>
 * 重写于2019-8-19
 * 原理:
 * API < 21
 * 键盘弹出, 只会回调 onSizeChanged , 相差的高度就是键盘的高度
 * <p>
 * API >= 21
 * 如果未激活 View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN, 那么和 API < 21 的处理方式一样
 * 键盘弹出, 会回调 onApplyWindowInsets , insets.getSystemWindowInsetBottom, 就是键盘的高度
 * 此时onSizeChange方法不会执行, 应为系统是用 PaddingBottom的方式, 为键盘腾出空间
 * <p>
 * 创建人员：Robi
 * 创建时间：2016/12/21 9:01
 * 修改人员：Robi
 * 修改时间：2019-8-19
 * 修改备注：
 * Version: 1.0.0
 */
public class RSoftInputLayout2 extends FrameLayout {

    private static final int INTENT_NONE = 0;
    private static final int INTENT_SHOW_KEYBOARD = 1;
    private static final int INTENT_HIDE_KEYBOARD = 2;
    private static final int INTENT_SHOW_EMOJI = 3;
    private static final int INTENT_HIDE_EMOJI = 4;

    /**
     * 是否激活控件
     */
    private boolean enableSoftInput = true;
    private boolean enableSoftInputAnim = true;

    /**
     * 频繁切换键盘, 延迟检查时长.
     * 如果开启了手机的安全密码输入键盘, 可以适当的加大延迟时间. 消除抖动.
     */
    private int switchCheckDelay = 64;

    private long animDuration = 240;

    /**
     * 在软键盘展示的过程中, 动态改变此paddingTop, 需要开启 [enableSoftInputAnim]
     * 大于0, 表示激活属性
     */
    private int animPaddingTop = -1;
    /**
     * 键盘完全显示时, 依旧需要的padding大小
     */
    private int animPaddingMinTop = 0;

    /**
     * 激活表情布局恢复, (如:显示键盘之前是表情布局, 那么隐藏键盘后就会显示表情布局)
     */
    private boolean enableEmojiRestore = false;

    /**
     * 当键盘未显示过时, 默认的键盘高度
     */
    int defaultKeyboardHeight = -1;

    /**
     * 由于延迟操作带来的意图延迟, 此变量不考虑无延迟
     */
    int wantIntentAction = INTENT_NONE;
    /**
     * 当前用户操作的意图
     */
    int intentAction = INTENT_NONE;
    /**
     * 最后一次有效的操作意图
     */
    int lastIntentAction = intentAction;

    /**
     * 最后一次的意图, 用来实现表情布局状态恢复
     */
    int lastRestoreIntentAction = intentAction;
    //2级缓存状态
    int lastRestoreIntentAction2 = intentAction;

    public RSoftInputLayout2(@NonNull Context context) {
        super(context);
        initLayout(context, null);
    }

    public RSoftInputLayout2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context, attrs);
    }

    private void initLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RSoftInputLayout2);
        defaultKeyboardHeight = array.getDimensionPixelOffset(R.styleable.RSoftInputLayout2_r_default_soft_input_height, defaultKeyboardHeight);
        animPaddingTop = array.getDimensionPixelOffset(R.styleable.RSoftInputLayout2_r_soft_input_anim_padding_top, animPaddingTop);
        animPaddingMinTop = array.getDimensionPixelOffset(R.styleable.RSoftInputLayout2_r_soft_input_anim_padding_min_top, animPaddingMinTop);
        enableSoftInputAnim = array.getBoolean(R.styleable.RSoftInputLayout2_r_enable_soft_input_anim, enableSoftInputAnim);
        enableEmojiRestore = array.getBoolean(R.styleable.RSoftInputLayout2_r_enable_emoji_restore, enableEmojiRestore);
        switchCheckDelay = array.getInt(R.styleable.RSoftInputLayout2_r_switch_check_delay, switchCheckDelay);
        animDuration = array.getInt(R.styleable.RSoftInputLayout2_r_anim_duration, (int) animDuration);
        array.recycle();
    }

    //<editor-fold defaultState="collapsed" desc="核心方法">

    int contentLayoutMaxHeight = 0;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int maxWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int maxHeight = heightSize - getPaddingTop() - getPaddingBottom() - calcAnimPaddingTop();

        boolean layoutFullScreen = isLayoutFullScreen(getContext());

        int bottomHeight = bottomCurrentShowHeight;
        if (isInEditMode()) {
            if (emojiLayout == null) {
                defaultKeyboardHeight = 0;
            }
            bottomHeight = defaultKeyboardHeight;
        }

        if (isAnimStart()) {
            bottomHeight = bottomCurrentShowHeightAnim;
        }

        if (contentLayout != null) {
            FrameLayout.LayoutParams layoutParams = (LayoutParams) contentLayout.getLayoutParams();

            if (layoutFullScreen) {
                contentLayoutMaxHeight = maxHeight - bottomHeight - layoutParams.topMargin - layoutParams.bottomMargin;
            } else {
                if (isSoftKeyboardShow()) {
                    //这里加动画, 体验不好.
//                    if (isAnimStart()) {
//                        contentLayoutMaxHeight = (int) (maxHeight - layoutParams.topMargin - layoutParams.bottomMargin
//                                + bottomCurrentShowHeight * (1 - animProgress));
//                    } else {
//                        contentLayoutMaxHeight = maxHeight - layoutParams.topMargin - layoutParams.bottomMargin;
//                    }
                    contentLayoutMaxHeight = maxHeight - layoutParams.topMargin - layoutParams.bottomMargin;
                } else {
                    contentLayoutMaxHeight = maxHeight - bottomHeight - layoutParams.topMargin - layoutParams.bottomMargin;
                }
            }
            int contentLayoutMaxWidth = maxWidth - layoutParams.leftMargin - layoutParams.rightMargin;

            if (layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                measureChildWithMargins(contentLayout, widthMeasureSpec, 0,
                        heightMeasureSpec, 0);
            }

            if (contentLayout.getMeasuredHeight() > contentLayoutMaxHeight
                    || layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                contentLayout.measure(MeasureSpec.makeMeasureSpec(contentLayoutMaxWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(contentLayoutMaxHeight, MeasureSpec.EXACTLY));
            }
        }

        if (emojiLayout != null) {
            emojiLayout.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(validBottomHeight(), MeasureSpec.EXACTLY));
        }

        measureOther(widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int l = getPaddingLeft();
        int t = getPaddingTop() + calcAnimPaddingTop();
        int r = 0;
        int b = 0;

        if (contentLayout != null) {
            FrameLayout.LayoutParams layoutParams = (LayoutParams) contentLayout.getLayoutParams();

            r = l + contentLayout.getMeasuredWidth();
            b = t + contentLayoutMaxHeight;

            t = b - layoutParams.bottomMargin - contentLayout.getMeasuredHeight();

            contentLayout.layout(l, t, r, b);

            t = contentLayout.getBottom();
        }

        if (emojiLayout != null) {
            if (isSoftKeyboardShow()) {
                t = getMeasuredHeight();
            } else if (!enableSoftInputAnim && intentAction == INTENT_HIDE_KEYBOARD) {
                t = getMeasuredHeight();
            }

            r = l + emojiLayout.getMeasuredWidth();
            b = t + emojiLayout.getMeasuredHeight();
            emojiLayout.layout(l, t, r, b);
        }

        layoutOther();
    }

    //键盘/emoji 当前显示的高度
    int bottomCurrentShowHeight = 0;
    //动画过程中的高度变量
    int bottomCurrentShowHeightAnim = 0;
    //动画进度
    float animProgress = 0f;
    int lastKeyboardHeight = 0;

    private Runnable delaySizeChanged;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //低版本适配
        boolean layoutFullScreen = isLayoutFullScreen(getContext());
        if (!layoutFullScreen) {
            if (intentAction == INTENT_SHOW_EMOJI && checkSizeChanged == null) {
                return;
            }

            if (isFirstLayout(oldw, oldh)) {
                if (isSoftKeyboardShow()) {
                    //软件盘默认是显示状态
                    oldh = h + getSoftKeyboardHeight();
                }
            }

            if (handleSizeChange(w, h, oldw, oldh)) {
                //有可能是键盘弹出了
                int diffHeight = oldh - h;

                boolean softKeyboardShow = isSoftKeyboardShow();

                if (diffHeight > 0) {
                    if (softKeyboardShow) {
                        wantIntentAction = INTENT_SHOW_KEYBOARD;
                    } else {
                        wantIntentAction = INTENT_SHOW_EMOJI;
                    }
                } else {
                    if (lastIntentAction == INTENT_SHOW_EMOJI) {
                        wantIntentAction = INTENT_HIDE_EMOJI;
                    } else {
                        wantIntentAction = INTENT_HIDE_KEYBOARD;
                    }
                }
            }
        } else {
            //高版本, 默认显示键盘适配
            if (oldw == 0 && oldh == 0) {
                if (isSoftKeyboardShow()) {
                    oldh = h;
                    h = oldh - getSoftKeyboardHeight();
                }
            }
        }

        //用来解决, 快速切换 emoji布局和键盘或者普通键盘和密码键盘 时, 闪烁的不良体验.
        if (delaySizeChanged != null) {
            removeCallbacks(delaySizeChanged);
        }
        delaySizeChanged = new DelaySizeChangeRunnable(w, h, oldw, oldh, wantIntentAction);
        postDelayed(delaySizeChanged, switchCheckDelay);
    }

    //</editor-fold defaultstate="collapsed" desc="核心方法">

    //<editor-fold defaultstate="collapsed" desc="辅助方法">

    private void initDefaultKeyboardHeight() {
        //恢复上一次键盘的高度
        if (defaultKeyboardHeight < 0) {
            defaultKeyboardHeight = (int) (275 * getResources().getDisplayMetrics().density);
        }
    }

    private boolean isFirstLayout(int oldw, int oldh) {
        return oldw == 0 && oldh == 0 && intentAction == INTENT_NONE;
    }

    private boolean handleSizeChange(int w, int h, int oldw, int oldh) {
        boolean result = false;

        boolean isFirstLayout = isFirstLayout(oldw, oldh);

        int diffHeight = oldh - h;
        if (isFirstLayout) {
            //布局第一次显示在界面上, 需要排除默认键盘展示的情况
            result = diffHeight != 0;
        } else {
            if (oldw != 0 && w != oldw) {
                //有可能屏幕旋转了
            } else {
                result = diffHeight != 0;
            }
        }
        return result;
    }

    private void setIntentAction(int action) {
        if (action == INTENT_NONE || intentAction != action) {
            if (intentAction != INTENT_NONE) {
                lastIntentAction = intentAction;
            }
        }
        intentAction = action;
        wantIntentAction = action;
    }

    private void clearIntentAction() {
        setIntentAction(INTENT_NONE);
    }

    private void measureOther(int widthMeasureSpec, int heightMeasureSpec) {
        for (int i = 2; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0,
                        heightMeasureSpec, 0);
            }
        }
    }

    private void layoutOther() {
        final int count = getChildCount();

        final int parentLeft = getPaddingLeft();
        final int parentRight = getMeasuredWidth() - getPaddingRight();

        final int parentTop = getPaddingTop();
        final int parentBottom = getMeasuredHeight() - getPaddingBottom();

        final boolean forceLeftGravity = false;

        for (int i = 2; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = Gravity.TOP | Gravity.LEFT;
                }

                int layoutDirection = 1;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    layoutDirection = getLayoutDirection();
                }

                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                                lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        if (!forceLeftGravity) {
                            childLeft = parentRight - width - lp.rightMargin;
                            break;
                        }
                    case Gravity.LEFT:
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = parentTop + lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                                lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = parentBottom - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                }

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }

    View contentLayout;
    View emojiLayout;

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        int childCount = getChildCount();
        /*请按顺序布局*/
        if (childCount > 0) {
            contentLayout = getChildAt(0);
        }
        if (childCount > 1) {
            emojiLayout = getChildAt(1);
        }

        if (haveChildSoftInput(child)) {
            setEnableSoftInput(false);
        }
    }


    /**
     * 解决RSoftInputLayout2嵌套RSoftInputLayout2的问题
     */
    private boolean haveChildSoftInput(View child) {
        if (child instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) child).getChildCount(); i++) {
                return haveChildSoftInput(((ViewGroup) child).getChildAt(i));
            }
        }
        return child instanceof RSoftInputLayout2;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        initDefaultKeyboardHeight();

        if (!isInEditMode() && isEnabled() && enableSoftInput) {
            setFitsSystemWindows();
            //setClipToPadding(false);//未知作用
        }

        //必须放在post里面调用, 才会生效
        post(new Runnable() {
            @Override
            public void run() {
                adjustResize(getContext());
            }
        });
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        //此方法会触发 dispatchApplyWindowInsets
        insets.set(0, 0, 0, 0);
        super.fitSystemWindows(insets);
        return isEnableSoftInput();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {

        //Fragment+Fragment中使用此控件支持.
        if (!isEnableSoftInput()) {
            return super.onApplyWindowInsets(insets);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            int insetBottom = insets.getSystemWindowInsetBottom();

            if (getMeasuredWidth() <= 0 && getMeasuredHeight() <= 0) {
                return super.onApplyWindowInsets(insets);
            }

            if (isSoftKeyboardShow() && insetBottom <= 0) {
                //软件已经显示, 此时却要隐藏键盘. ViewPager中, 使用此控件支持.
                return super.onApplyWindowInsets(insets);
            }

            if (insetBottom > 0) {
                wantIntentAction = INTENT_SHOW_KEYBOARD;
            } else {
                wantIntentAction = INTENT_HIDE_KEYBOARD;
            }

            if (insetRunnable != null) {
                removeCallbacks(insetRunnable);
            }

            insetRunnable = new InsetRunnable(insetBottom);

            //键盘切换到键盘, 延迟检查. 防止是普通键盘切换到密码键盘
            if (lastIntentAction == INTENT_NONE) {
                //第一次不检查
                insetRunnable.run();
            } else {
                postDelayed(insetRunnable, switchCheckDelay);
            }

            //替换掉系统的默认处理方式(setPadding)
            //系统会使用setPadding的方式, 为键盘留出空间
            return insets.replaceSystemWindowInsets(0, 0, 0, 0);
        }
        return super.onApplyWindowInsets(insets);
    }

    private Runnable insetRunnable;

    //底部需要腾出距离
    private void insetBottom(int height) {
        //L.i("插入:" + height);
        int insetBottom = height;
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        if (insetBottom > 0) {
            //键盘弹出
            checkSizeChanged = new KeyboardRunnable(measuredWidth, measuredHeight - insetBottom,
                    measuredWidth, measuredHeight);
        } else {
            //键盘隐藏
            checkSizeChanged = new KeyboardRunnable(measuredWidth, measuredHeight,
                    measuredWidth, measuredHeight - bottomCurrentShowHeight);
        }
        checkOnSizeChanged(false);
    }

    private Runnable checkSizeChanged;

    private void checkOnSizeChanged(boolean delay) {
        if (checkSizeChanged == null) {
            return;
        }
        removeCallbacks(checkSizeChanged);
        if (delay) {
            post(checkSizeChanged);
        } else {
            checkSizeChanged.run();
        }
    }

    /**
     * 判断键盘是否显示
     */
    public boolean isSoftKeyboardShow() {
        if (isInEditMode()) {
            return false;
        }
        int screenHeight = getScreenHeightPixels();
        int keyboardHeight = getSoftKeyboardHeight();
        return screenHeight != keyboardHeight && keyboardHeight > 50 * getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * 获取键盘的高度
     */
    public int getSoftKeyboardHeight() {
        return getSoftKeyboardHeight(this);
    }

    /**
     * 屏幕高度(不包含虚拟导航键盘的高度)
     */
    private int getScreenHeightPixels() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private ValueAnimator mValueAnimator;

    private void startAnim(int bottomHeightFrom, int bottomHeightTo, long duration) {
//        L.i("动画:from:" + bottomHeightFrom + "->" + bottomHeightTo);
        cancelAnim();

        int from = bottomHeightFrom;
        int to = bottomHeightTo;
        if (animatorCallback != null) {
            int[] preStart = animatorCallback.onAnimatorPreStart(wantIntentAction, bottomHeightFrom, bottomHeightTo, duration);
            from = preStart[0];
            to = preStart[1];
            duration = preStart[2];
        }

        mValueAnimator = ObjectAnimator.ofInt(from, to);
        mValueAnimator.setDuration(duration);
        if (animatorCallback == null) {
            mValueAnimator.setInterpolator(new DecelerateInterpolator());
        } else {
            mValueAnimator.setInterpolator(animatorCallback.getInterpolator(wantIntentAction));
        }
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = animation.getAnimatedFraction();
                int animatedValue = (int) animation.getAnimatedValue();
                animProgress = animatedFraction;
                if (animatorCallback == null) {
                    bottomCurrentShowHeightAnim = animatedValue;
                } else {
                    bottomCurrentShowHeightAnim = animatorCallback.onUpdateAnimatorValue(intentAction, animatedFraction, animatedValue);
                }
                requestLayout();
            }
        });
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                clearIntentAction();
                if (animatorCallback != null) {
                    animatorCallback.onAnimatorEnd(lastIntentAction, false);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                //clearIntentAction();
                if (animatorCallback != null) {
                    animatorCallback.onAnimatorEnd(wantIntentAction, true);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mValueAnimator.start();

        if (animatorCallback != null) {
            animatorCallback.onAnimatorStart(wantIntentAction);
        }
    }

    private void cancelAnim() {
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
    }

    //动画是否执行中
    private boolean isAnimStart() {
        return mValueAnimator != null && mValueAnimator.isRunning();
    }

    //表情布局有效的高度
    private int validBottomHeight() {
        //没显示过键盘, 默认高度
        int bottomHeight = defaultKeyboardHeight;

        if (lastKeyboardHeight > 0) {
            //显示过键盘, 有键盘的高度
            bottomHeight = lastKeyboardHeight;
        }

        return bottomHeight;
    }

    private int calcAnimPaddingTop() {
        if (animPaddingTop <= 0) {
            return 0;
        }

        if (isInEditMode()) {
            return animPaddingTop;
        }

        int result = animPaddingTop;
        boolean animStart = isAnimStart();
        int statusBarHeight = getStatusBarHeight(getContext());

        boolean layoutFullScreen = isLayoutFullScreen(getContext());
        if (isSoftKeyboardShow() || isEmojiLayoutShow()) {
            if (animStart && intentAction != lastIntentAction) {
                result = (int) (animPaddingTop * (1 - animProgress));
                result = Math.max(result, statusBarHeight + animPaddingMinTop);
            } else if (lastIntentAction == INTENT_NONE ||
                    lastIntentAction == INTENT_HIDE_EMOJI ||
                    lastIntentAction == INTENT_HIDE_KEYBOARD) {

            } else if (layoutFullScreen) {
                result = statusBarHeight + animPaddingMinTop;
            } else {
                result = animPaddingMinTop;
            }
        } else {
            if (wantIntentAction == INTENT_HIDE_EMOJI ||
                    wantIntentAction == INTENT_HIDE_KEYBOARD) {

                if (animStart && wantIntentAction != lastIntentAction) {
                    result = (int) (animPaddingTop * animProgress);
                    result = Math.max(result, statusBarHeight + animPaddingMinTop);
                } else if (layoutFullScreen) {
                    result = statusBarHeight + animPaddingMinTop;
                } else {
                    result = animPaddingMinTop;
                }
            }
        }
        return result;
    }

    //</editor-fold defaultstate="collapsed" desc="辅助方法">

    //<editor-fold defaultstate="collapsed" desc="属性操作">

    public void setEnableSoftInput(boolean enableSoftInput) {
        if (this.enableSoftInput == enableSoftInput) {
            return;
        }

        this.enableSoftInput = enableSoftInput;
        if (enableSoftInput) {
            setEnabled(true);
            setFitsSystemWindows(true);
        } else {
            setFitsSystemWindows(false);
        }

        requestLayout();
    }

    public void setFitsSystemWindows() {
        setFitsSystemWindows(isEnabled() && enableSoftInput);
    }

    public boolean isEnableSoftInput() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return getFitsSystemWindows() && isEnabled() && enableSoftInput;
        } else {
            return isEnabled() && enableSoftInput;
        }
    }

    //</editor-fold defaultstate="collapsed" desc="属性操作">

    //<editor-fold defaultstate="collapsed" desc="方法控制">

    public boolean isEmojiLayoutShow() {
        boolean result = false;

        if (isSoftKeyboardShow()) {
            return false;
        }

        if (emojiLayout != null) {
            if (isAnimStart()) {
                result = bottomCurrentShowHeight > 0;
            } else if (emojiLayout.getMeasuredHeight() > 0
                    && emojiLayout.getTop() < getMeasuredHeight() - getPaddingBottom()
                    && emojiLayout.getBottom() >= getMeasuredHeight() - getPaddingBottom()
            ) {
                result = true;
            }
        }
        return result;
    }

    /**
     * 返回按键处理
     *
     * @return true 表示可以关闭界面
     */
    public boolean requestBackPressed() {
        if (isSoftKeyboardShow()) {
            if (intentAction == INTENT_HIDE_KEYBOARD) {
                return false;
            }
            setIntentAction(INTENT_HIDE_KEYBOARD);
            hideSoftInput(this);
            return false;
        }

        if (isEmojiLayoutShow()) {
            hideEmojiLayout();
            return false;
        }

        return true;
    }

    /**
     * 显示表情布局
     */
    public void showEmojiLayout() {
        showEmojiLayout(validBottomHeight(), false);
    }

    public void showEmojiLayout(int height) {
        showEmojiLayout(height, true);
    }

    public void showEmojiLayout(int height, boolean force) {
        if (force) {
            lastKeyboardHeight = height;
        } else {
            if (isEmojiLayoutShow()) {
                return;
            }

            if (intentAction == INTENT_SHOW_EMOJI) {
                return;
            }
        }

        if (isSoftKeyboardShow()) {
            hideSoftInput(this);
        }

        setIntentAction(INTENT_SHOW_EMOJI);
        insetBottom(height);
    }

    /**
     * 隐藏表情布局
     */
    public void hideEmojiLayout() {
        if (intentAction == INTENT_HIDE_EMOJI) {
            return;
        }

        if (isEmojiLayoutShow()) {
            setIntentAction(INTENT_HIDE_EMOJI);
            insetBottom(0);
        }
    }

    public void setEnableSoftInputAnim(boolean enableSoftInputAnim) {
        this.enableSoftInputAnim = enableSoftInputAnim;
    }

    public void setSwitchCheckDelay(int switchCheckDelay) {
        this.switchCheckDelay = switchCheckDelay;
    }

    public void setAnimPaddingTop(int animPaddingTop) {
        this.animPaddingTop = animPaddingTop;
        requestLayout();
    }

    public void setAnimPaddingMinTop(int animPaddingMinTop) {
        this.animPaddingMinTop = animPaddingMinTop;
    }

    public int getBottomCurrentShowHeight() {
        return bottomCurrentShowHeight;
    }

    public int getBottomCurrentShowHeightAnim() {
        return bottomCurrentShowHeightAnim;
    }

    public long getAnimDuration() {
        return animDuration;
    }

    public boolean isEnableSoftInputAnim() {
        return enableSoftInputAnim;
    }

    //</editor-fold defaultstate="collapsed" desc="方法控制">

    //<editor-fold defaultstate="collapsed" desc="事件相关">

    HashSet<OnEmojiLayoutChangeListener> mEmojiLayoutChangeListeners = new HashSet<>();

    public void addOnEmojiLayoutChangeListener(OnEmojiLayoutChangeListener listener) {
        mEmojiLayoutChangeListeners.add(listener);
        adjustResize(getContext());
    }

    public void removeOnEmojiLayoutChangeListener(OnEmojiLayoutChangeListener listener) {
        mEmojiLayoutChangeListeners.remove(listener);
    }

    private void notifyEmojiLayoutChangeListener(boolean isEmojiShow, boolean isKeyboardShow, int height) {
        Iterator<OnEmojiLayoutChangeListener> iterator = mEmojiLayoutChangeListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onEmojiLayoutChange(isEmojiShow, isKeyboardShow, height);
        }
    }

    public interface OnEmojiLayoutChangeListener {
        /**
         * @param height         EmojiLayout弹出的高度 或者 键盘弹出的高度
         * @param isEmojiShow    表情布局是否显示了
         * @param isKeyboardShow 键盘是否显示了
         */
        void onEmojiLayoutChange(boolean isEmojiShow, boolean isKeyboardShow, int height);
    }


    /**
     * 动画执行回调, 可以修改动画执行的值
     */
    public AnimatorCallback animatorCallback;

    public static class AnimatorCallback {

        /**
         * 动画需要执行的值
         *
         * @return 按照入参顺序, 返回对应修改后的值
         */
        public int[] onAnimatorPreStart(int intentAction, int bottomHeightFrom, int bottomHeightTo, long duration) {
            return new int[]{bottomHeightFrom, bottomHeightTo, (int) duration};
        }

        /**
         * 动画执行过程中的值
         */
        public int onUpdateAnimatorValue(int intentAction, float animatedFraction, int animatedValue) {
            return animatedValue;
        }

        public TimeInterpolator getInterpolator(int intentAction) {
            return new DecelerateInterpolator();
        }

        public void onAnimatorStart(int intentAction) {

        }

        public void onAnimatorEnd(int intentAction, boolean isCancel) {

        }
    }

    //</editor-fold defaultstate="collapsed" desc="事件相关">

    //<editor-fold defaultstate="collapsed" desc="静态区">

    /**
     * 状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static void hideSoftInput(@NonNull View view) {
        InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showSoftInput(@NonNull View view) {
        view.requestFocus();
        InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.showSoftInput(view, 0);
    }

    public static boolean isLayoutFullScreen(@Nullable Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = ((Activity) context).getWindow();
                int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
                return (systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static int getSoftKeyboardHeight(@NonNull View view) {
        Context context = view.getContext();
        int screenHeight = 0;
        boolean isLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        if (context instanceof Activity && isLayoutFullScreen(context)) {
            Window window = ((Activity) context).getWindow();
            view = window.getDecorView();
            screenHeight = window.findViewById(Window.ID_ANDROID_CONTENT).getMeasuredHeight();
        } else {
            screenHeight = view.getResources().getDisplayMetrics().heightPixels;
            if (isLollipop) {
                screenHeight += getStatusBarHeight(context);
            }
        }

        Rect rect = new Rect();
        view.getWindowVisibleDisplayFrame(rect);
        int visibleBottom = rect.bottom;
        return screenHeight - visibleBottom;
    }

    public static void adjustResize(Context context) {
        //resize 必备条件
        if (context instanceof Activity) {
            Window window = ((Activity) context).getWindow();
            int softInputMode = window.getAttributes().softInputMode;
            if ((softInputMode & WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                    != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
    }

    //</editor-fold defaultstate="collapsed" desc="静态区">

    private class KeyboardRunnable implements Runnable {
        int w;
        int h;
        int oldw;
        int oldh;

        private KeyboardRunnable(int w, int h, int oldw, int oldh) {
            this.w = w;
            this.h = h;
            this.oldw = oldw;
            this.oldh = oldh;
        }

        @Override
        public void run() {
            onSizeChanged(w, h, oldw, oldh);
            checkSizeChanged = null;
        }
    }

    private class DelaySizeChangeRunnable implements Runnable {
        int w;
        int h;
        int oldw;
        int oldh;
        int delayIntentAction;

        private DelaySizeChangeRunnable(int w, int h, int oldw, int oldh, int action) {
            this.w = w;
            this.h = h;
            this.oldw = oldw;
            this.oldh = oldh;
            this.delayIntentAction = action;
        }

        @Override
        public void run() {
            int oldBottomCurrentShowHeight = bottomCurrentShowHeight;

            //L.i("size:" + oldh + "->" + h + " " + oldBottomCurrentShowHeight);

            bottomCurrentShowHeight = 0;
            boolean needAnim = enableSoftInputAnim;
            if (handleSizeChange(w, h, oldw, oldh)) {
                //有可能是键盘弹出了
                int diffHeight = oldh - h;

                boolean softKeyboardShow = isSoftKeyboardShow();
                boolean emojiLayoutShow = false;

                if (softKeyboardShow) {
                    lastRestoreIntentAction2 = lastRestoreIntentAction;
                } else {
                    lastRestoreIntentAction = intentAction;
                }

                boolean layoutFullScreen = isLayoutFullScreen(getContext());

                //低版本, 普通输入框和密码输入框切换适配
                if (!layoutFullScreen) {
                    if (softKeyboardShow) {
                        diffHeight = getSoftKeyboardHeight();
                    }
                }

                if (diffHeight > 0) {
                    //当用代码调整了布局的height属性, 也会回调此方法.
                    bottomCurrentShowHeight = diffHeight;

                    if (softKeyboardShow) {
                        lastKeyboardHeight = diffHeight;
                        emojiLayoutShow = false;
                    } else {
                        emojiLayoutShow = intentAction == INTENT_SHOW_EMOJI;
                        //有可能是表情布局显示
                    }
                    notifyEmojiLayoutChangeListener(emojiLayoutShow, softKeyboardShow, diffHeight);
                    if (needAnim) {
                        if (isAnimStart()) {
                            startAnim(Math.abs(bottomCurrentShowHeightAnim), diffHeight, animDuration);
                        } else {
                            startAnim(oldBottomCurrentShowHeight, diffHeight, animDuration);
                        }
                    }
                } else {
                    if (lastRestoreIntentAction2 == INTENT_SHOW_EMOJI && enableEmojiRestore) {
                        emojiLayoutShow = true;
                        diffHeight = -diffHeight;

                        bottomCurrentShowHeight = diffHeight;
                        lastRestoreIntentAction2 = INTENT_NONE;
                    }
                    notifyEmojiLayoutChangeListener(emojiLayoutShow, softKeyboardShow, diffHeight);

                    if (isFirstLayout(oldw, oldh)) {
                        needAnim = false;
                    }

                    if (needAnim && !emojiLayoutShow) {

                        if (isAnimStart()) {
                            startAnim(Math.abs(bottomCurrentShowHeightAnim), 0, animDuration);
                        } else {
                            startAnim(Math.abs(diffHeight), 0, animDuration);
                        }
                    }
                }

                //低版本适配
                if (!layoutFullScreen) {
                    setIntentAction(delayIntentAction);
                }

                requestLayout();
            }

            if (!needAnim) {
                clearIntentAction();
            }
            delaySizeChanged = null;
        }
    }

    private class InsetRunnable implements Runnable {

        int insetBottom;

        private InsetRunnable(int insetBottom) {
            this.insetBottom = insetBottom;
        }

        @Override
        public void run() {
            if (intentAction <= INTENT_HIDE_KEYBOARD) {
                if (insetBottom > 0) {
                    setIntentAction(INTENT_SHOW_KEYBOARD);
                } else {
                    setIntentAction(INTENT_HIDE_KEYBOARD);
                }
                cancelAnim();
                insetBottom(insetBottom);
            }

            insetRunnable = null;
        }
    }
}
