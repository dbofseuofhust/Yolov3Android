package com.jacky.finalexam.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class Card extends RecyclerView.LayoutManager{

    private float onceWholeScrollLength = -1;

    private float firstChildOffsetLength = -1;

    private int mFirstViewPos;

    private int mLastViewPos;

    private long mHorizontalOffset;

    private float normalViewLength = 30;

    private int childWidth = 0;

    private boolean isAutoSelected = true;
    private ValueAnimator selectedAnimator;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
    }

    public Card(Context context, int length) {
        normalViewLength = dp2px(context, length);
    }

    public Card(Context context) {
        this(context, 0);
    }

    public static float dp2px(Context context, float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
//        super.onLayoutChildren(recycler, state);
        if(state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }

        onceWholeScrollLength = -1;

        detachAndScrapAttachedViews(recycler);

        fill(recycler, state, 0);
    }

    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        int resultDelta = dx;
        resultDelta = fillHorizongtalLeft(recycler, state, dx);
        recycleChildren(recycler);
        return resultDelta;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (0 == dx || 0 == getChildCount()) {
            return 0;
        }

        float realDx = dx / 1.0f;
        if(Math.abs(realDx) < 0.00000001f) {
            return 0;
        }

        mHorizontalOffset += dx;
        dx = fill(recycler, state, dx);
        return dx;
    }

    private float getMaxOffset() {
        if(0 == childWidth || getItemCount() == 0) return 0;
        return (childWidth + normalViewLength) * (getItemCount() - 1);
    }

    private float getMinOffset() {
        if(childWidth == 0) return 0;
        return (getWidth() - childWidth) / 2;
    }

    private int fillHorizongtalLeft(RecyclerView.Recycler recycler, RecyclerView.State state, int dx) {
        if(dx < 0) {
            if(mHorizontalOffset < 0) {
                mHorizontalOffset = dx = 0;
            }
        }

        if(dx > 0) {
            if(mHorizontalOffset >= getMaxOffset()) {
                mHorizontalOffset -= dx;
                mHorizontalOffset = (long) getMaxOffset();
                dx = 0;
            }
        }

        detachAndScrapAttachedViews(recycler);

        float startX = 0;
        float fraction = 0f;
        boolean isChildLayoutLeft = true;

        View tempView = null;
        int tempPosition = -1;

        if(onceWholeScrollLength == -1) {
            tempPosition = mFirstViewPos;
            tempView = recycler.getViewForPosition(tempPosition);
            measureChildWithMargins(tempView, 0 ,0);
            childWidth = getDecoratedMeasurementHorizontal(tempView);
        }

        firstChildOffsetLength = getWidth() / 2 + childWidth / 2;
        if(mHorizontalOffset >= firstChildOffsetLength) {
            startX = normalViewLength;
            onceWholeScrollLength = childWidth + normalViewLength;
            mFirstViewPos = (int) (Math.floor(Math.abs(mHorizontalOffset - firstChildOffsetLength) / onceWholeScrollLength) + 1);
            fraction = (Math.abs(mHorizontalOffset - firstChildOffsetLength) % onceWholeScrollLength) / (onceWholeScrollLength * 1.0f);
        } else {
            mFirstViewPos = 0;
            startX = getMinOffset();
            onceWholeScrollLength = firstChildOffsetLength;
            fraction = (Math.abs(mHorizontalOffset) % onceWholeScrollLength) /(onceWholeScrollLength / 1.0f);
        }

        mLastViewPos = getItemCount() - 1;

        float normalViewOffset = onceWholeScrollLength * fraction;
        boolean isNormalViewOffset = false;

        for (int i = mFirstViewPos; i <= mLastViewPos; i++) {
            View item;
            if(i == tempPosition && tempView != null) {
                item = tempView;
            } else {
                item = recycler.getViewForPosition(i);
            }

            int focusPosition = (int) (Math.abs(mHorizontalOffset) / (childWidth + normalViewLength));
            if (i < focusPosition) {
                addView(item);
            } else {
                addView(item, 0);
            }
            measureChildWithMargins(item, 0, 0);

            if(!isNormalViewOffset) {
                startX -= normalViewOffset;
                isNormalViewOffset = true;
            }

            int l, t, r, b;
            l = (int) startX;
            t = getPaddingTop();
            r = l + getDecoratedMeasurementHorizontal(item);
            b = t + getDecoratedMeasurementVertical(item);

            final float minScale = 0.6f;
            float currentScale = 0f;
            final int childCenterX = (r + l) / 2;
            final int parentCenterX = getWidth() / 2;

            isChildLayoutLeft = childCenterX <= parentCenterX;

            if(isChildLayoutLeft) {
                final float fractionScale = (parentCenterX - childCenterX) / (parentCenterX * 1.0f);
                currentScale = 1.0f - (1.0f - minScale) * fractionScale;
            } else {
                final float fractionScale = (childCenterX - parentCenterX) / (parentCenterX * 1.0f);
                currentScale = 1.0f  - (1.0f - minScale) * fractionScale;
            }
            item.setScaleX(currentScale);
            item.setScaleY(currentScale);

            layoutDecoratedWithMargins(item, l, t, r, b);

            startX += (childWidth + normalViewLength);

            if(startX > getWidth() - getPaddingRight()) {
                mLastViewPos = i;
                break;
            }
        }
        return dx;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        switch (state) {
            case RecyclerView.SCROLL_STATE_DRAGGING:
                cancelAnimator();
                break;
            case RecyclerView.SCROLL_STATE_IDLE:
                if(isAutoSelected) {
                    smoothScrollToPosition(findShouldSelectPosition(), null);
                }
                break;
            default:
                break;
        }
    }

    public int findShouldSelectPosition() {
        if (onceWholeScrollLength == -1 || mFirstViewPos == -1) {
            return -1;
        }
        int position = (int) (Math.abs(mHorizontalOffset) / (childWidth + normalViewLength));
        int remainder = (int) (Math.abs(mHorizontalOffset) % (childWidth + normalViewLength));
        // 超过一半，应当选中下一项
        if (remainder >= (childWidth + normalViewLength) / 2.0f) {
            if (position + 1 <= getItemCount() - 1) {
                return position + 1;
            }
        }
        return position;
    }

    public void smoothScrollToPosition(int position, onMimoListener listener) {
        if (position > -1 && position < getItemCount()) {
            startValueAnimator(position, listener);
        }
    }

    private void startValueAnimator(int position, final onMimoListener listener) {
        cancelAnimator();

        final float distance = getScrollToPositionOffset(position);

        long minDuration = 100;
        long maxDuration = 300;
        long duration;

        float distanceFraction = (Math.abs(distance) / (childWidth + normalViewLength));

        if (distance <= (childWidth + normalViewLength)) {
            duration = (long) (minDuration + (maxDuration - minDuration) * distanceFraction);
        } else {
            duration = (long) (maxDuration * distanceFraction);
        }
        selectedAnimator = ValueAnimator.ofFloat(0.0f, distance);
        selectedAnimator.setDuration(duration);
        selectedAnimator.setInterpolator(new LinearInterpolator());
        final float startedOffset = mHorizontalOffset;
        selectedAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                mHorizontalOffset = (long) (startedOffset + value);
                requestLayout();
            }
        });
        selectedAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (listener != null) {
                    listener.onFocusAnimEnd();
                }
            }
        });
        selectedAnimator.start();
    }

    private float getScrollToPositionOffset(int position) {
        return position * (childWidth + normalViewLength) - Math.abs(mHorizontalOffset);
    }

    public void cancelAnimator() {
        if (selectedAnimator != null && (selectedAnimator.isStarted() || selectedAnimator.isRunning())) {
            selectedAnimator.cancel();
        }
    }

    private void recycleChildren(RecyclerView.Recycler recycler) {
        List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        for (int i = 0; i < scrapList.size(); i++) {
            RecyclerView.ViewHolder holder = scrapList.get(i);
            removeAndRecycleView(holder.itemView, recycler);
        }
    }

    public int getDecoratedMeasurementHorizontal(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + params.leftMargin
                + params.rightMargin;
    }

    public int getDecoratedMeasurementVertical(View view) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)
                view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + params.topMargin
                + params.bottomMargin;
    }

    public int getVerticalSpace() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    public int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    public interface onMimoListener {
        void onFocusAnimEnd();
    }
}
