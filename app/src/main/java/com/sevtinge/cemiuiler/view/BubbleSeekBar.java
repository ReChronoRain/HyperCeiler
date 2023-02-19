package com.sevtinge.cemiuiler.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import moralnorm.annotation.Nullable;

import com.sevtinge.cemiuiler.R;

import java.util.Collection;

import moralnorm.animation.Folme;
import moralnorm.animation.IStateStyle;
import moralnorm.animation.base.AnimConfig;
import moralnorm.animation.controller.AnimState;
import moralnorm.animation.listener.TransitionListener;
import moralnorm.animation.listener.UpdateInfo;
import moralnorm.animation.property.IntValueProperty;
import moralnorm.animation.property.ValueProperty;
import moralnorm.animation.utils.EaseManager;
import moralnorm.animation.utils.EaseManager.EaseStyleDef;

public class BubbleSeekBar extends View {

    public int mBsbNormalWidth;
    public int mBsbSliderToFillGap;
    public int mBsbVisibilityHeight;
    public int mBsbVisibilityWidth;
    public PopupWindow mBubble;
    public int mBubbleSeekbarDistance;
    public int mBubbleWidthNormal;
    public int mBubbleY;
    public int mColorEmpty;
    public int mColorFill;
    public int mColorSlider;
    public float mCurrentEmptyProgressHeight;
    public float mCurrentEmptyProgressWidth;
    public float mCurrentFillProgressHeight;
    public float mCurrentProgress;
    public float mCurrentSliderWidth;
    public float mDeviationProgress;
    public int mEmptyProgressHeight;
    public EnlargeAnimListener mEnlargeAnimListener;
    public int mFillProgressHeight;
    public Handler mHandler = new Handler();
    public int mHeight;
    public boolean mHideBubble;
    public AnimConfig mHideBubbleConfig;
    public int mIdentityHashCode;
    public boolean mIsPress;
    public int mMaxProgress;
    public int mMinProgress;
    public float mMoveOffset;
    public int mOffsetX;
    public int mOffsetY;
    public Paint mPaintEmpty;
    public Paint mPaintFill;
    public Paint mPaintSlider;
    public int mPopWidowPadding;
    public int mPopWidowSize;
    public View mPopWindowContentView;
    public float mPopWindowContentViewAlpha;
    public int mPopWindowContentViewSize;
    public int mPopX;
    public int mPopY;
    public float mPressX;
    public int mPreviousProgress;
    public ProgressListener mProgressListener;
    public float mProgressPosition;
    public TextView mProgressTv;
    public AnimConfig mShowBubbleConfig;
    public boolean mSliderEenlargeHide;
    public int mSliderWidth;
    public int mSliderWidthHighlight;
    public int mStartPointProgress;
    public float mStartPointProportion;
    public boolean mTrackingAndNotMove;
    public int mWidth;

    public static final ValueProperty ANIM_ALPHA = new ValueProperty("ANIM_ALPHA");
    public static final ValueProperty ANIM_EMPTY_HEIGHT = new ValueProperty("ANIM_EMPTY_HEIGHT");
    public static final ValueProperty ANIM_EMPTY_WIDTH = new ValueProperty("ANIM_EMPTY_WIDTH");
    public static final ValueProperty ANIM_FILL_HEIGHT = new ValueProperty("ANIM_FILL_HEIGHT");
    public static final ValueProperty ANIM_SLIDER_WIDTH = new ValueProperty("ANIM_SLIDER_WIDTH");
    public static final IntValueProperty ANIM_WIDTH = new IntValueProperty("ANIM_WIDTH");
    public static final IntValueProperty ANIM_Y = new IntValueProperty("ANIM_Y");
    public Runnable mInteractiveRunnable = new Runnable() {
        @Override
        public void run() {
            setEnabled(true);
        }
    };

    public TransitionListener mTransitionListener = new TransitionListener() {

        @Override
        public void onUpdate(Object o, Collection<UpdateInfo> collection) {
            super.onUpdate(o, collection);

            UpdateInfo var4 = UpdateInfo.findBy(collection, ANIM_Y);
            if (var4 != null) {
                mBubbleY = var4.getIntValue();
            }

            var4 = UpdateInfo.findBy(collection, ANIM_WIDTH);
            if (var4 != null) {
                mPopWindowContentViewSize = var4.getIntValue();
            }

            var4 = UpdateInfo.findBy(collection, ANIM_ALPHA);
            if (var4 != null) {
                float var3 = var4.getFloatValue();
                if (var3 >= 0.0F && var3 <= 1.0F) {
                    mPopWindowContentViewAlpha = var4.getFloatValue();
                }
            }

            var4 = UpdateInfo.findBy(collection, ANIM_EMPTY_HEIGHT);
            if (var4 != null) {
                mCurrentEmptyProgressHeight = (float)var4.getIntValue();
            }

            var4 = UpdateInfo.findBy(collection, ANIM_FILL_HEIGHT);
            if (var4 != null) {
                mCurrentFillProgressHeight = (float)var4.getIntValue();
            }

            var4 = UpdateInfo.findBy(collection, ANIM_EMPTY_WIDTH);
            if (var4 != null) {
                mCurrentEmptyProgressWidth = (float)var4.getIntValue();
            }

            var4 = UpdateInfo.findBy(collection, ANIM_SLIDER_WIDTH);
            if (var4 != null) {
                mCurrentSliderWidth = (float)var4.getIntValue();
            }
            invalidate();
        }

        @Override
        public void onCancel(Object o, UpdateInfo updateInfo) {
            super.onCancel(o, updateInfo);
        }

        @Override
        public void onComplete(Object o, UpdateInfo updateInfo) {
            super.onComplete(o, updateInfo);
        }
    };

    public BubbleSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public float getCurrentFillHeight() {
        return mCurrentFillProgressHeight;
    }

    public float getCurrentHeight() {
        return mCurrentEmptyProgressHeight;
    }

    public float getCurrentProgress() {
        return mCurrentProgress + mDeviationProgress;
    }

    public float getCurrentSliderWidth() {
        return mCurrentSliderWidth;
    }

    public float getCurrentWidth() {
        return mCurrentEmptyProgressWidth;
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public int getMinProgress() {
        return mMinProgress;
    }

    public final void init(Context context, AttributeSet attrs) {
        initDimensionValues(context, attrs);
        initPaint();
        initPopupWindow();
        mBsbNormalWidth = mBsbNormalWidth == 0 ? mBsbVisibilityHeight : mBsbNormalWidth;
        mCurrentEmptyProgressHeight = (float) mEmptyProgressHeight;
        mCurrentFillProgressHeight = (float) mFillProgressHeight;
        mCurrentEmptyProgressWidth = (float) mBsbNormalWidth;
        mCurrentSliderWidth = (float) mSliderWidth;
        mBubbleWidthNormal = getResources().getDimensionPixelSize(R.dimen.magic_px_100);
        mSliderWidthHighlight = getResources().getDimensionPixelOffset(R.dimen.magic_px_50);
        mPopWidowSize = getResources().getDimensionPixelSize(R.dimen.magic_seek_bar_bubble_shape_bg_solid_radius);
        mPopWidowPadding = (mPopWidowSize - mBubbleWidthNormal) / 2;
        mIdentityHashCode = System.identityHashCode(this);
        setEnabled(false);
    }

    public final void initDimensionValues(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MagicBubbleSeekBar);

        mColorEmpty = a.getColor(R.styleable.MagicBubbleSeekBar_empty_color, 268435456);
        mColorFill = a.getColor(R.styleable.MagicBubbleSeekBar_fill_color, 1073741824);
        mColorSlider = a.getColor(R.styleable.MagicBubbleSeekBar_slider_color, 0);
        mMaxProgress = a.getInteger(R.styleable.MagicBubbleSeekBar_max_progress, 100);
        mMinProgress = a.getInteger(R.styleable.MagicBubbleSeekBar_min_progress, 0);
        mSliderWidth = a.getDimensionPixelSize(R.styleable.MagicBubbleSeekBar_slider_width, 0);
        mCurrentProgress = a.getFloat(R.styleable.MagicBubbleSeekBar_current_progress, 0.0F);
        mEmptyProgressHeight = a.getDimensionPixelSize(R.styleable.MagicBubbleSeekBar_empty_progress_height, 50);
        mFillProgressHeight = a.getDimensionPixelSize(R.styleable.MagicBubbleSeekBar_fill_progress_height, mEmptyProgressHeight);
        mHideBubble = a.getBoolean(R.styleable.MagicBubbleSeekBar_hide_bubble, true);
        mSliderEenlargeHide = a.getBoolean(R.styleable.MagicBubbleSeekBar_slider_enlarge_hide, false);
        mBsbNormalWidth = a.getDimensionPixelSize(R.styleable.MagicBubbleSeekBar_normal_width, 100);
        mBsbVisibilityWidth = a.getDimensionPixelSize(R.styleable.MagicBubbleSeekBar_visibility_width, 100);
        mBubbleSeekbarDistance = a.getDimensionPixelSize(R.styleable.MagicBubbleSeekBar_bubble_seekbar_distance, 0);
        mBsbVisibilityHeight = a.getDimensionPixelSize(R.styleable.MagicBubbleSeekBar_visibility_height, 100);
        a.recycle();
    }

    public final void initPaint() {
        mPaintEmpty = new Paint();
        mPaintEmpty.setAntiAlias(true);
        mPaintEmpty.setColor(mColorEmpty);
        mPaintEmpty.setStyle(Paint.Style.FILL);

        mPaintFill = new Paint();
        mPaintFill.setAntiAlias(true);
        mPaintFill.setColor(mColorFill);
        mPaintFill.setStyle(Paint.Style.FILL);

        mPaintSlider = new Paint();
        mPaintSlider.setAntiAlias(true);
        mPaintSlider.setColor(mColorSlider);
        mPaintSlider.setStyle(Paint.Style.FILL);
    }

    public final void initPopupWindow() {
        mShowBubbleConfig = new AnimConfig();
        mShowBubbleConfig.setMinDuration(200L);
        mShowBubbleConfig.setEase(EaseManager.getStyle(EaseStyleDef.SPRING_PHY,0.9f, 0.3f));
        mShowBubbleConfig.setSpecial(ANIM_Y, EaseManager.getStyle(EaseStyleDef.SPRING_PHY, 0.9f, 0.2f), 0);
        mShowBubbleConfig.setSpecial(ANIM_WIDTH, EaseManager.getStyle(EaseStyleDef.SPRING_PHY, 0.9f, 0.2f), 0);
        mShowBubbleConfig.setSpecial(ANIM_ALPHA, EaseManager.getStyle(EaseStyleDef.CUBIC_OUT, 100f), 0);

        mHideBubbleConfig = new AnimConfig();
        mHideBubbleConfig.setMinDuration(200L);
        mHideBubbleConfig.setEase(EaseManager.getStyle(EaseStyleDef.SPRING_PHY, 0.9f, 0.3f));
        mHideBubbleConfig.setSpecial(ANIM_Y, EaseManager.getStyle(EaseStyleDef.CUBIC_IN, 250f), 0);
        mHideBubbleConfig.setSpecial(ANIM_WIDTH, EaseManager.getStyle(EaseStyleDef.CUBIC_IN, 250f), 0);
        mHideBubbleConfig.setSpecial(ANIM_ALPHA, EaseManager.getStyle(EaseStyleDef.CUBIC_OUT, 100f), 150L, 0);

        View contentView = LayoutInflater.from(getContext()).inflate(R.layout.bubble_seek_bar_window, (ViewGroup)null);
        mPopWindowContentView = contentView.findViewById(R.id.rl_content_view);
        mProgressTv = (TextView) mPopWindowContentView.findViewById(R.id.tv_progress);
        mBubble = new Bubble(contentView, mPopWidowSize, mPopWidowSize, false);
        mPopWindowContentViewSize = mPopWindowContentView.getLayoutParams().width;
        mBubble.setTouchable(false);
        mPopWindowContentView.setAlpha(mPopWindowContentViewAlpha);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mBubble.isShowing() && !mHideBubble) {
            mBubble.showAsDropDown(this);
        }
        mHandler.postDelayed(mInteractiveRunnable, 300L);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBubble.dismiss();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float f5 = ((float)mBsbVisibilityHeight - mCurrentFillProgressHeight) / 2.0F;
        if (mBubble.isShowing() && !mHideBubble) {
            ViewGroup.LayoutParams lp = mPopWindowContentView.getLayoutParams();
            mPopX = (int)((float)mOffsetX + ((((float)(mBsbVisibilityWidth - mBsbVisibilityHeight)) * (mCurrentProgress + mDeviationProgress - (float)mMinProgress) / (float)(mMaxProgress - mMinProgress)) + (float) (mBsbVisibilityHeight / 2)) - (float)(mPopWidowSize / 2));
            mPopY = -(mBsbVisibilityHeight + mOffsetY + mPopWidowSize + mBubbleY - mPopWidowPadding);
            mBubble.update(this, mPopX, mPopY, mPopWidowSize, mPopWidowSize);
            lp.width = mPopWindowContentViewSize;
            lp.height = mPopWindowContentViewSize;
            mPopWindowContentView.setLayoutParams(lp);
            mPopWindowContentView.setAlpha(mPopWindowContentViewAlpha);
        }

        mBsbSliderToFillGap = getContext().getResources().getDimensionPixelSize(mIsPress ? R.dimen.magic_bubble_seek_bar_slider_to_fill_gap_highlight : R.dimen.magic_bubble_seek_bar_slider_to_fill_gap_normal);
        float var8 = (mCurrentProgress + mDeviationProgress - (float)mMinProgress) / (float)(mMaxProgress - mMinProgress);
        mProgressPosition = (float)(mBsbVisibilityWidth - mBsbVisibilityHeight) * var8 + (float)(this.mEmptyProgressHeight / 2) + (float) mBsbSliderToFillGap;

        RectF r = new RectF((float)mOffsetX + (((float) mBsbVisibilityWidth - mCurrentEmptyProgressWidth) / 2f), (float)mOffsetY + (((float)mBsbVisibilityHeight - mCurrentEmptyProgressHeight) / 2f), (float)this.mBsbVisibilityWidth - (((float) mBsbVisibilityWidth - mCurrentEmptyProgressWidth) / 2f) + (float)mOffsetX, (float)this.mBsbVisibilityHeight - (((float)mBsbVisibilityHeight - mCurrentEmptyProgressHeight) / 2f) + (float)mOffsetY);
        canvas.drawRoundRect(r, mCurrentEmptyProgressHeight, mCurrentEmptyProgressHeight, mPaintEmpty);

        mProgressPosition = Math.min(mProgressPosition, (float)mBsbVisibilityWidth - f5 - mCurrentFillProgressHeight / 2.0F);
        mProgressPosition = Math.max(mProgressPosition, mCurrentFillProgressHeight / 2f + f5);
        mStartPointProportion = (float)(mStartPointProgress - mMinProgress) / (float)(mMaxProgress - mMinProgress);
        RectF rectF;
        if (mStartPointProportion == 0f) {
            rectF = new RectF((float) mOffsetX + f5, (float)mOffsetY + f5, (float)mOffsetX + mProgressPosition + mCurrentFillProgressHeight / 2.0F + 4.0F, (float)(mOffsetY + mBsbVisibilityHeight) - f5);
        } else {

            float f;
            float f2;
            if (var8 > mStartPointProportion) {
                f = (float)(mWidth / 2);
            } else {
                f = (float)mOffsetX + mProgressPosition;
            }

            if (var8 < mStartPointProportion) {
                f2 = (float)(mWidth / 2);
            } else {
                f2 = (float) mOffsetX + mProgressPosition;
            }

            rectF = new RectF(f - (mCurrentFillProgressHeight / 2f), (float) mOffsetY + f5, f2 + mCurrentFillProgressHeight / 2.0F, (float)(mOffsetY + mBsbVisibilityHeight) - f5);
        }
        canvas.drawRoundRect(rectF, mCurrentFillProgressHeight, mCurrentFillProgressHeight, mPaintFill);

        if (!mSliderEenlargeHide) {
            float f = mCurrentSliderWidth == 0f ? (mCurrentFillProgressHeight - (float)(mBsbSliderToFillGap * 2)) / 2f : mCurrentSliderWidth / 2f;
            canvas.drawCircle((float) mOffsetX + mProgressPosition, (float)mOffsetY + f5 + (mCurrentFillProgressHeight / 2.0F), f, mPaintSlider);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        mOffsetY = (mHeight - mBsbVisibilityHeight) / 2;
        mOffsetX = (mWidth - mBsbVisibilityWidth) / 2;
    }

    public boolean onTouchEvent(MotionEvent var1) {
        if (!this.isEnabled()) {
            return false;
        } else {
            int var2 = var1.getAction();
            ValueProperty var5;
            double var6;
            ValueProperty var9;
            ValueProperty var13;
            AnimState var17;
            AnimState var18;
            AnimState var22;
            TransitionListener var32;
            IStateStyle var33;
            if (var2 == 0) {
                this.mIsPress = true;
                this.mPressX = var1.getX();
                this.mProgressTv.setText(String.valueOf((int)this.mCurrentProgress));
                if (mProgressListener != null) {
                    mProgressListener.onStartTrackingTouch(this);
                }

                AnimState var16 = new AnimState("showFrom");
                var5 = ANIM_EMPTY_HEIGHT;
                var6 = (double)this.mCurrentEmptyProgressHeight;
                var16 = var16.add(var5, var6);
                ValueProperty var23 = ANIM_FILL_HEIGHT;
                var6 = (double)this.mCurrentFillProgressHeight;
                var16 = var16.add(var23, var6);
                ValueProperty var30 = ANIM_EMPTY_WIDTH;
                var6 = (double)this.mCurrentEmptyProgressWidth;
                var16 = var16.add(var30, var6);
                var13 = ANIM_SLIDER_WIDTH;
                var6 = (double)this.mCurrentSliderWidth;
                var16 = var16.add(var13, var6);
                IntValueProperty var24 = ANIM_Y;
                var6 = (double)this.mBubbleY;
                var16 = var16.add(var24, var6);
                IntValueProperty var19 = ANIM_WIDTH;
                var6 = (double)this.mPopWindowContentViewSize;
                var16 = var16.add(var19, var6);
                var9 = ANIM_ALPHA;
                var6 = (double)this.mPopWindowContentViewAlpha;
                var16 = var16.add(var9, var6);
                AnimState var20 = new AnimState("showTo");
                var6 = (double)((float)this.mBsbVisibilityHeight);
                var18 = var20.add(var5, var6);
                var6 = (double)((float)this.mBsbVisibilityHeight);
                AnimState var25 = var18.add(var23, var6);
                var6 = (double)((float)this.mBsbVisibilityWidth);
                AnimState var31 = var25.add(var30, var6);
                var6 = (double)((float)this.mSliderWidthHighlight);
                AnimState var34 = var31.add(var13, var6);
                var6 = (double)(this.mBubbleSeekbarDistance + this.mPopWidowPadding);
                var22 = var34.add(var24, var6);
                var6 = (double)this.mPopWidowSize;
                var17 = var22.add(var19, var6);
                var17 = var17.add(var9, 1.0D);
                Folme.useValue(new Object[]{this.mIdentityHashCode}).cancel();
                IStateStyle var26 = Folme.useValue(new Object[]{this.mIdentityHashCode});
                var32 = this.mTransitionListener;
                var33 = var26.addListener(var32);
                AnimConfig var28 = this.mShowBubbleConfig;
                var33.fromTo(var16, var17, new AnimConfig[]{var28});
                this.mTrackingAndNotMove = true;
                return true;
            } else {
                if (var2 != 1) {
                    if (var2 == 2) {
                        float var3 = var1.getX() - this.mPressX;
                        this.mMoveOffset = var3;
                        var3 = (float)(this.mMaxProgress - this.mMinProgress) * var3 / (float)(this.mBsbVisibilityWidth - this.mBsbVisibilityHeight);
                        this.mDeviationProgress = var3;
                        var3 = Math.min(var3, (float)this.getMaxProgress() - this.mCurrentProgress);
                        this.mDeviationProgress = var3;
                        var3 = Math.max(var3, (float)getMinProgress() - this.mCurrentProgress);
                        this.mDeviationProgress = var3;
                        var2 = (int)(this.mCurrentProgress + var3);
                        this.mProgressTv.setText(String.valueOf(var2));
                        if (this.mProgressListener != null) {
                            if (this.mPreviousProgress == var2 && (var2 == this.getMaxProgress() || var2 == this.getMinProgress() || var2 == 0)) {
                                return true;
                            }

                            if (this.mTrackingAndNotMove) {
                                this.mProgressListener.onProgressStartChange(this, var2);
                                this.mTrackingAndNotMove = false;
                            }

                            this.mProgressListener.onProgressChanged(this, var2);
                            this.mPreviousProgress = var2;
                        }

                        this.invalidate();
                        return true;
                    }

                    if (var2 != 3) {
                        return super.onTouchEvent(var1);
                    }
                }

                if (mProgressListener != null) {
                    mProgressListener.onStopTrackingTouch(this);
                }

                this.mIsPress = false;
                this.mCurrentProgress += this.mDeviationProgress;
                this.mDeviationProgress = 0.0F;
                Folme.useValue(new Object[]{this.mIdentityHashCode}).cancel();
                var17 = new AnimState("hideFrom");
                var5 = ANIM_EMPTY_HEIGHT;
                var6 = (double)this.mCurrentEmptyProgressHeight;
                var17 = var17.add(var5, var6);
                ValueProperty var8 = ANIM_FILL_HEIGHT;
                var6 = (double)this.mCurrentFillProgressHeight;
                var17 = var17.add(var8, var6);
                var9 = ANIM_EMPTY_WIDTH;
                var6 = (double)this.mCurrentEmptyProgressWidth;
                var17 = var17.add(var9, var6);
                ValueProperty var10 = ANIM_SLIDER_WIDTH;
                var6 = (double)this.mCurrentSliderWidth;
                var17 = var17.add(var10, var6);
                IntValueProperty var11 = ANIM_Y;
                var6 = (double)this.mBubbleY;
                var17 = var17.add(var11, var6);
                IntValueProperty var12 = ANIM_WIDTH;
                var6 = (double)this.mPopWindowContentViewSize;
                var17 = var17.add(var12, var6);
                var13 = ANIM_ALPHA;
                var6 = (double)this.mPopWindowContentViewAlpha;
                var17 = var17.add(var13, var6);
                AnimState var14 = new AnimState("hideTo");
                var6 = (double)((float)this.mEmptyProgressHeight);
                var18 = var14.add(var5, var6);
                var6 = (double)((float)this.mFillProgressHeight);
                var18 = var18.add(var8, var6);
                var6 = (double)((float)this.mBsbNormalWidth);
                AnimState var21 = var18.add(var9, var6);
                var6 = (double)((float)this.mSliderWidth);
                var22 = var21.add(var10, var6);
                var22 = var22.add(var11, 0.0D);
                var6 = (double)this.mBubbleWidthNormal;
                var22 = var22.add(var12, var6);
                var22 = var22.add(var13, 0.0D);
                IStateStyle var27 = Folme.useValue(new Object[]{this.mIdentityHashCode});
                var32 = this.mTransitionListener;
                var33 = var27.addListener(var32);
                AnimConfig var29 = this.mHideBubbleConfig;
                var33.fromTo(var17, var22, new AnimConfig[]{var29});
                return super.onTouchEvent(var1);
            }
        }
    }

    public void setCurrentFillHeight(float currentFillHeight) {
        mCurrentFillProgressHeight = currentFillHeight;
    }

    public void setCurrentHeight(float currentHeight) {
        mCurrentEmptyProgressHeight = currentHeight;
        if (mEnlargeAnimListener != null) {
            mEnlargeAnimListener.onAnimProgressChanged((int)((float) mMaxProgress * (currentHeight - (float) mEmptyProgressHeight) / (float)(this.mBsbVisibilityHeight - mEmptyProgressHeight)));
        }
        invalidate();
    }

    public void setCurrentProgress(float currentProgress) {
        float progress = currentProgress;
        if (currentProgress > (float) mMaxProgress) {
            progress = (float) mMaxProgress;
        }
        mCurrentProgress = progress;
        postInvalidate();
    }

    public void setCurrentSliderWidth(float sliderWidth) {
        mCurrentSliderWidth = sliderWidth;
    }

    public void setCurrentWidth(float currentWidth) {
        mCurrentEmptyProgressWidth = currentWidth;
    }

    public void setEnlargeListener(EnlargeAnimListener listener) {
        mEnlargeAnimListener = listener;
    }

    public void setHideBubble(boolean hideBubble) {
        mHideBubble = hideBubble;
    }

    public void setMaxProgress(int progress) {
        mMaxProgress = progress;
        postInvalidate();
    }

    public void setMinProgress(int progress) {
        mMinProgress = progress;
        postInvalidate();
    }

    public void setProgressListener(ProgressListener listener) {
        mProgressListener = listener;
    }

    public class Bubble extends PopupWindow {

        public Bubble(View contentView, int width, int height, boolean focusable) {
            super(contentView, width, height, focusable);
        }
    }

    public interface EnlargeAnimListener {
        void onAnimProgressChanged(int progress);
    }

    public interface ProgressListener {

        default void onProgressStartChange(BubbleSeekBar seekBar, int progress) {}

        void onProgressChanged(BubbleSeekBar seekBar, int progress);

        void onStartTrackingTouch(BubbleSeekBar seekBar);

        void onStopTrackingTouch(BubbleSeekBar seekBar);
    }
}
