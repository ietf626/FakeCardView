/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.proton626.library;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

/**
 * A FrameLayout with a rounded corner background and shadow.
 *
 * @attr ref R.styleable#FakeCardView_cardBackgroundColor
 * @attr ref R.styleable#FakeCardView_cardCornerRadius
 * @attr ref R.styleable#FakeCardView_cardElevation
 * @attr ref R.styleable#FakeCardView_cardMaxElevation
 * @attr ref R.styleable#FakeCardView_cardPreventCornerOverlap
 * @attr ref R.styleable#FakeCardView_contentPadding
 * @attr ref R.styleable#FakeCardView_contentPaddingLeft
 * @attr ref R.styleable#FakeCardView_contentPaddingTop
 * @attr ref R.styleable#FakeCardView_contentPaddingRight
 * @attr ref R.styleable#FakeCardView_contentPaddingBottom
 */
public class FakeCardView extends FrameLayout {

    private static final int[] COLOR_BACKGROUND_ATTR = {android.R.attr.colorBackground};
    private static final CardViewImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new FakeCardViewApi21();
        } else if (Build.VERSION.SDK_INT >= 17) {
            IMPL = new FakeCardViewJellybeanMr1();
        } else {
            IMPL = new CardViewGingerbread();
        }
        IMPL.initStatic();
    }

    private boolean mPreventCornerOverlap;

    /**
     * CardView requires to have a particular minimum size to draw shadows before API 21. If
     * developer also sets min width/height, they might be overridden.
     * <p>
     * CardView works around this issue by recording user given parameters and using an internal
     * method to set them.
     */
    int mUserSetMinWidth, mUserSetMinHeight;

    final Rect mContentPadding = new Rect();

    final Rect mShadowBounds = new Rect();

    public FakeCardView(Context context) {
        super(context);
        initialize(context, null, 0);
    }

    public FakeCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0);
    }

    public FakeCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        // NO OP
    }

    public void setPaddingRelative(int start, int top, int end, int bottom) {
        // NO OP
    }

    /**
     * Sets the padding between the Card's edges and the children of CardView.
     *
     * @param left   The left padding in pixels
     * @param top    The top padding in pixels
     * @param right  The right padding in pixels
     * @param bottom The bottom padding in pixels
     * @attr ref R.styleable#FakeCardView_contentPadding
     * @attr ref R.styleable#FakeCardView_contentPaddingLeft
     * @attr ref R.styleable#FakeCardView_contentPaddingTop
     * @attr ref R.styleable#FakeCardView_contentPaddingRight
     * @attr ref R.styleable#FakeCardView_contentPaddingBottom
     */
    public void setContentPadding(int left, int top, int right, int bottom) {
        mContentPadding.set(left, top, right, bottom);
        IMPL.updatePadding(mCardViewDelegate);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!(IMPL instanceof FakeCardViewApi21)) {
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            switch (widthMode) {
                case MeasureSpec.EXACTLY:
                case MeasureSpec.AT_MOST:
                    final int minWidth = (int) Math.ceil(IMPL.getMinWidth(mCardViewDelegate));
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(minWidth,
                            MeasureSpec.getSize(widthMeasureSpec)), widthMode);
                    break;
            }

            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            switch (heightMode) {
                case MeasureSpec.EXACTLY:
                case MeasureSpec.AT_MOST:
                    final int minHeight = (int) Math.ceil(IMPL.getMinHeight(mCardViewDelegate));
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(minHeight,
                            MeasureSpec.getSize(heightMeasureSpec)), heightMode);
                    break;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private ColorStateList backgroundColor;

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FakeCardView, defStyleAttr,
                R.style.FakeCardView);

        if (a.hasValue(R.styleable.FakeCardView_cardBackgroundColor)) {
            backgroundColor = a.getColorStateList(R.styleable.FakeCardView_cardBackgroundColor);
        } else {
            // 没有设置背景则从当前主题中提取
            final TypedArray aa = getContext().obtainStyledAttributes(COLOR_BACKGROUND_ATTR);
            final int themeColorBackground = aa.getColor(0, 0);
            aa.recycle();

            //若主题中的colorBackground是浅色,使用cardview_light_background,否则使用cardview_dark_background
            final float[] hsv = new float[3];
            Color.colorToHSV(themeColorBackground, hsv);
            backgroundColor = ColorStateList.valueOf(hsv[2] > 0.5f
                    ? getResources().getColor(R.color.cardview_light_background)
                    : getResources().getColor(R.color.cardview_dark_background));
        }
        float radius = a.getDimension(R.styleable.FakeCardView_cardCornerRadius, 0);
        float elevation = a.getDimension(R.styleable.FakeCardView_cardElevation, 0);
        float maxElevation = a.getDimension(R.styleable.FakeCardView_cardMaxElevation, 0);
        mPreventCornerOverlap = a.getBoolean(R.styleable.FakeCardView_cardPreventCornerOverlap, true);
        int defaultPadding = a.getDimensionPixelSize(R.styleable.FakeCardView_contentPadding, 0);
        mContentPadding.left = a.getDimensionPixelSize(R.styleable.FakeCardView_contentPaddingLeft,
                defaultPadding);
        mContentPadding.top = a.getDimensionPixelSize(R.styleable.FakeCardView_contentPaddingTop,
                defaultPadding);
        mContentPadding.right = a.getDimensionPixelSize(R.styleable.FakeCardView_contentPaddingRight,
                defaultPadding);
        mContentPadding.bottom = a.getDimensionPixelSize(R.styleable.FakeCardView_contentPaddingBottom,
                defaultPadding);
        if (elevation > maxElevation) {
            maxElevation = elevation;
        }
        mUserSetMinWidth = a.getDimensionPixelSize(R.styleable.FakeCardView_android_minWidth, 0);
        mUserSetMinHeight = a.getDimensionPixelSize(R.styleable.FakeCardView_android_minHeight, 0);
        a.recycle();

        IMPL.initialize(mCardViewDelegate, context, backgroundColor, radius,
                elevation, maxElevation);
    }

    @Override
    public void setMinimumWidth(int minWidth) {
        mUserSetMinWidth = minWidth;
        super.setMinimumWidth(minWidth);
    }

    @Override
    public void setMinimumHeight(int minHeight) {
        mUserSetMinHeight = minHeight;
        super.setMinimumHeight(minHeight);
    }

    /**
     * Updates the background color of the CardView
     *
     * @param color The new color to set for the card background
     * @attr ref R.styleable#FakeCardView_cardBackgroundColor
     */
    public void setCardBackgroundColor( int color) {
        IMPL.setBackgroundColor(mCardViewDelegate, ColorStateList.valueOf(color));
    }

    /**
     * Updates the background ColorStateList of the CardView
     *
     * @param color The new ColorStateList to set for the card background
     * @attr ref R.styleable#FakeCardView_cardBackgroundColor
     */
    public void setCardBackgroundColor( ColorStateList color) {
        IMPL.setBackgroundColor(mCardViewDelegate, color);
    }

    /**
     * Returns the background color state list of the CardView.
     *
     * @return The background color state list of the CardView.
     */
    public ColorStateList getCardBackgroundColor() {
        return IMPL.getBackgroundColor(mCardViewDelegate);
    }

    /**
     * Returns the inner padding after the Card's left edge
     *
     * @return the inner padding after the Card's left edge
     */
    public int getContentPaddingLeft() {
        return mContentPadding.left;
    }

    /**
     * Returns the inner padding before the Card's right edge
     *
     * @return the inner padding before the Card's right edge
     */
    public int getContentPaddingRight() {
        return mContentPadding.right;
    }

    /**
     * Returns the inner padding after the Card's top edge
     *
     * @return the inner padding after the Card's top edge
     */
    public int getContentPaddingTop() {
        return mContentPadding.top;
    }

    /**
     * Returns the inner padding before the Card's bottom edge
     *
     * @return the inner padding before the Card's bottom edge
     */
    public int getContentPaddingBottom() {
        return mContentPadding.bottom;
    }

    /**
     * Updates the corner radius of the CardView.
     *
     * @param radius The radius in pixels of the corners of the rectangle shape
     * @attr ref R.styleable#FakeCardView_cardCornerRadius
     * @see #setRadius(float)
     */
    public void setRadius(float radius) {
        IMPL.setRadius(mCardViewDelegate, radius);
    }

    /**
     * Returns the corner radius of the CardView.
     *
     * @return Corner radius of the CardView
     * @see #getRadius()
     */
    public float getRadius() {
        return IMPL.getRadius(mCardViewDelegate);
    }

    /**
     * Updates the backward compatible elevation of the CardView.
     *
     * @param elevation The backward compatible elevation in pixels.
     * @attr ref R.styleable#FakeCardView_cardElevation
     * @see #getCardElevation()
     * @see #setMaxCardElevation(float)
     */
    public void setCardElevation(float elevation) {
        IMPL.setElevation(mCardViewDelegate, elevation);
    }

    /**
     * Returns the backward compatible elevation of the CardView.
     *
     * @return Elevation of the CardView
     * @see #setCardElevation(float)
     * @see #getMaxCardElevation()
     */
    public float getCardElevation() {
        return IMPL.getElevation(mCardViewDelegate);
    }

    /**
     * Updates the backward compatible maximum elevation of the CardView.
     *
     * @param maxElevation The backward compatible maximum elevation in pixels.
     * @attr ref R.styleable#FakeCardView_cardMaxElevation
     * @see #setCardElevation(float)
     * @see #getMaxCardElevation()
     */
    public void setMaxCardElevation(float maxElevation) {
        IMPL.setMaxElevation(mCardViewDelegate, maxElevation);
    }

    /**
     * Returns the backward compatible maximum elevation of the CardView.
     *
     * @return Maximum elevation of the CardView
     * @see #setMaxCardElevation(float)
     * @see #getCardElevation()
     */
    public float getMaxCardElevation() {
        return IMPL.getMaxElevation(mCardViewDelegate);
    }

    /**
     * Returns whether CardView should add extra padding to content to avoid overlaps with rounded
     * corners on pre-Lollipop platforms.
     *
     * @return True if FakeCardView prevents overlaps with rounded corners on platforms before Lollipop.
     * Default value is <code>true</code>.
     */
    public boolean getPreventCornerOverlap() {
        if (IMPL instanceof FakeCardViewApi21)
            return false;
        return mPreventCornerOverlap;
    }

    /**
     * On pre-Lollipop platforms, FakeCardView does not clip the bounds of the Card for the rounded
     * corners. Instead, it adds padding to content so that it won't overlap with the rounded
     * corners. You can disable this behavior by setting this field to <code>false</code>.
     * <p>
     * Setting this value on Lollipop and above does not have any effect unless you have enabled
     * compatibility padding.
     *
     * @param preventCornerOverlap Whether FakeCardView should add extra padding to content to avoid
     *                             overlaps with the FakeCardView corners.
     * @attr ref R.styleable#FakeCardView_cardPreventCornerOverlap
     */
    public void setPreventCornerOverlap(boolean preventCornerOverlap) {
        if (preventCornerOverlap != mPreventCornerOverlap) {
            mPreventCornerOverlap = preventCornerOverlap;
            IMPL.onPreventCornerOverlapChanged(mCardViewDelegate);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        clip(changed, left, top, right, bottom);
    }

    private void clip(boolean changed, final int left, final int top, final int right, final int bottom) {
        if (changed) {
            View view = getChildAt(0);
            if (view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    view.setOutlineProvider(new ViewOutlineProvider() {
                        @Override
                        public void getOutline(View view, Outline outline) {
                            Rect bounds = getBackground().getBounds();
                            bounds.inset(mShadowBounds.left, mShadowBounds.top);
                            bounds.left = bounds.left + mContentPadding.left;
                            bounds.right = bounds.right - mContentPadding.right;
                            bounds.top = bounds.top + mContentPadding.top;
                            bounds.bottom = bounds.bottom - mContentPadding.bottom;
                            bounds.offsetTo(0, 0);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                outline.setRoundRect(bounds, getRadius());
                            }

                        }
                    });
                    view.setClipToOutline(true);
                }
            }
        }
    }

    private final CardViewDelegate mCardViewDelegate = new CardViewDelegate() {
        private Drawable mCardBackground;

        @Override
        public void setCardBackground(Drawable drawable) {
            mCardBackground = drawable;
            setBackgroundDrawable(drawable);
        }


        @Override
        public boolean getPreventCornerOverlap() {

            return FakeCardView.this.getPreventCornerOverlap();
        }

        @Override
        public void setShadowPadding(int left, int top, int right, int bottom) {
            mShadowBounds.set(left, top, right, bottom);
            FakeCardView.super.setPadding(left + mContentPadding.left, top + mContentPadding.top,
                    right + mContentPadding.right, bottom + mContentPadding.bottom);
        }

        @Override
        public void setMinWidthHeightInternal(int width, int height) {
            if (width > mUserSetMinWidth) {
                FakeCardView.super.setMinimumWidth(width);
            }
            if (height > mUserSetMinHeight) {
                FakeCardView.super.setMinimumHeight(height);
            }
        }

        @Override
        public Drawable getCardBackground() {
            return mCardBackground;
        }


        @Override
        public View getCardView() {
            return FakeCardView.this;
        }
    };
}
