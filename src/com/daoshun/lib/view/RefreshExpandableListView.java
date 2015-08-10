package com.daoshun.lib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RefreshExpandableListView extends ExpandableListView implements OnScrollListener {

    private static final int TAP_TO_REFRESH = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;

    private boolean mLoadFlag;

    private OnScrollListener mOnScrollListener;
    private LayoutInflater mInflater;

    private RefreshViewInfo mRefreshHeaderViewInfo;
    private OnRefreshListener mOnRefreshHeaderListener;
    private LinearLayout mRefreshHeaderView;
    private TextView mRefreshHeaderViewText;
    private ImageView mRefreshHeaderViewImage;
    private ProgressBar mRefreshHeaderViewProgress;
    private TextView mRefreshHeaderViewLastRefresh;
    private int mRefreshHeaderViewHeight;
    private int mRefreshOriginalTopPadding;
    private int mRefreshHeaderState;
    private boolean mHeaderBounceHack;

    private RefreshViewInfo mRefreshFooterViewInfo;
    private OnRefreshListener mOnRefreshFooterListener;
    private LinearLayout mRefreshFooterView;
    private TextView mRefreshFooterViewText;
    private ImageView mRefreshFooterViewImage;
    private ProgressBar mRefreshFooterViewProgress;
    private int mRefreshFooterViewHeight;
    private int mRefreshOriginalBottomPadding;
    private int mRefreshFooterState;
    private boolean mFooterBounceHack;

    private int mCurrentScrollState;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    private int mLastMotionY;

    public RefreshExpandableListView(Context context) {
        super(context);
        init(context);
    }

    public RefreshExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RefreshExpandableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mFlipAnimation =
                new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);

        mReverseFlipAnimation =
                new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                        RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);

        super.setOnScrollListener(this);
    }

    private void initHeader() {
        mRefreshHeaderView =
                (LinearLayout) mInflater.inflate(mRefreshHeaderViewInfo.layoutResId, this, false);
        mRefreshHeaderViewText =
                (TextView) mRefreshHeaderView.findViewById(mRefreshHeaderViewInfo.messageResId);
        mRefreshHeaderViewImage =
                (ImageView) mRefreshHeaderView.findViewById(mRefreshHeaderViewInfo.drawableResId);
        mRefreshHeaderViewProgress =
                (ProgressBar) mRefreshHeaderView.findViewById(mRefreshHeaderViewInfo.progressResId);
        mRefreshHeaderViewLastRefresh =
                (TextView) mRefreshHeaderView.findViewById(mRefreshHeaderViewInfo.lastRefreshResId);

        mRefreshHeaderViewText.setText(mRefreshHeaderViewInfo.tabMessage);
        mRefreshHeaderView.setOnClickListener(new OnClickHeaderListener());
        mRefreshHeaderViewImage.setVisibility(View.GONE);
        mRefreshHeaderViewProgress.setVisibility(View.GONE);
        if (mRefreshHeaderViewLastRefresh != null)
            mRefreshHeaderViewLastRefresh.setVisibility(View.GONE);
        mRefreshOriginalTopPadding = mRefreshHeaderView.getPaddingTop();

        mRefreshHeaderState = TAP_TO_REFRESH;

        addHeaderView(mRefreshHeaderView);

        measureView(mRefreshHeaderView);
        mRefreshHeaderViewHeight = mRefreshHeaderView.getMeasuredHeight();
    }

    private void initFooter() {
        mRefreshFooterView =
                (LinearLayout) mInflater.inflate(mRefreshFooterViewInfo.layoutResId, this, false);
        mRefreshFooterViewText =
                (TextView) mRefreshFooterView.findViewById(mRefreshFooterViewInfo.messageResId);
        mRefreshFooterViewImage =
                (ImageView) mRefreshFooterView.findViewById(mRefreshFooterViewInfo.drawableResId);
        mRefreshFooterViewProgress =
                (ProgressBar) mRefreshFooterView.findViewById(mRefreshFooterViewInfo.progressResId);

        mRefreshFooterViewText.setText(mRefreshFooterViewInfo.tabMessage);
        mRefreshFooterView.setOnClickListener(new OnClickFooterListener());
        mRefreshFooterViewImage.setVisibility(View.GONE);
        mRefreshFooterViewProgress.setVisibility(View.GONE);
        mRefreshOriginalBottomPadding = mRefreshFooterView.getPaddingBottom();

        mRefreshFooterState = TAP_TO_REFRESH;

        addFooterView(mRefreshFooterView);

        measureView(mRefreshFooterView);
        mRefreshFooterViewHeight = mRefreshFooterView.getMeasuredHeight();
    }

    public void setRefreshHeaderViewInfo(RefreshViewInfo viewInfo) {
        if ((mRefreshHeaderViewInfo == null && viewInfo != null)
                || (mRefreshHeaderViewInfo != null && viewInfo == null)) {
            mRefreshHeaderViewInfo = viewInfo;
            if (viewInfo != null) {
                initHeader();
            } else {
                removeHeaderView(mRefreshHeaderView);
            }
        }
    }

    public void setRefreshFooterViewInfo(RefreshViewInfo viewInfo) {
        if ((mRefreshFooterViewInfo == null && viewInfo != null)
                || (mRefreshFooterViewInfo != null && viewInfo == null)) {
            mRefreshFooterViewInfo = viewInfo;
            if (viewInfo != null) {
                initFooter();
            } else {
                removeFooterView(mRefreshFooterView);
            }
        }
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
    }

    public void setOnRefreshHeaderListener(OnRefreshListener onRefreshListener) {
        mOnRefreshHeaderListener = onRefreshListener;
    }

    public void setOnRefreshFooterListener(OnRefreshListener onRefreshListener) {
        mOnRefreshFooterListener = onRefreshListener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();

        mHeaderBounceHack = false;
        mFooterBounceHack = false;

        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            if (!isVerticalScrollBarEnabled()) {
                setVerticalScrollBarEnabled(true);
            }
            if (getFirstVisiblePosition() == 0
                    && mRefreshHeaderState != REFRESHING && mRefreshHeaderViewInfo != null) {
                if ((mRefreshHeaderView.getBottom() >= mRefreshHeaderViewHeight || mRefreshHeaderView
                        .getTop() >= 0) && mRefreshHeaderState == RELEASE_TO_REFRESH) {
                    prepareForRefreshHeader();
                    onRefreshHeader();
                } else if (mRefreshHeaderView.getBottom() < mRefreshHeaderViewHeight
                        || mRefreshHeaderView.getTop() < 0) {
                    resetHeader();
                    setSelection(1);
                }
            }
            if (getLastVisiblePosition() == getCount() - 1
                    && mRefreshFooterState != REFRESHING && mRefreshFooterViewInfo != null) {
                if ((getMeasuredHeight() - mRefreshFooterView.getTop() >= mRefreshFooterViewHeight || getMeasuredHeight()
                        - mRefreshFooterView.getBottom() >= 0)
                        && mRefreshFooterState == RELEASE_TO_REFRESH) {
                    prepareForRefreshFooter();
                    onRefreshFooter();
                } else if (getMeasuredHeight() - mRefreshFooterView.getTop() < mRefreshFooterViewHeight
                        || getMeasuredHeight() - mRefreshFooterView.getBottom() < 0) {
                    resetFooter();
                    setSelectionFromTop(getCount()
                            + getHeaderViewsCount() + getFooterViewsCount() - 1,
                            getMeasuredHeight());
                }
            }
            break;
        case MotionEvent.ACTION_DOWN:
            mLastMotionY = y;
            break;
        case MotionEvent.ACTION_MOVE:
            applyPadding(event);
            break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void applyPadding(MotionEvent ev) {
        for (int p = 0; p < ev.getHistorySize(); p++) {
            if ((mRefreshHeaderState == RELEASE_TO_REFRESH && mRefreshHeaderViewInfo != null)
                    || (mRefreshFooterState == RELEASE_TO_REFRESH && mRefreshFooterViewInfo != null)) {
                if (isVerticalScrollBarEnabled()) {
                    setVerticalScrollBarEnabled(false);
                }

                int historicalY = (int) ev.getHistoricalY(p);

                if (mRefreshHeaderState == RELEASE_TO_REFRESH && mRefreshHeaderViewInfo != null) {

                    int topPadding =
                            (int) ((historicalY - mLastMotionY - mRefreshHeaderViewHeight) / 2);

                    mRefreshHeaderView.setPadding(mRefreshHeaderView.getPaddingLeft(), topPadding
                            + mRefreshOriginalTopPadding, mRefreshHeaderView.getPaddingRight(),
                            mRefreshHeaderView.getPaddingBottom());
                }

                if (mRefreshFooterState == RELEASE_TO_REFRESH && mRefreshFooterViewInfo != null) {

                    int bottomPadding =
                            (int) ((mLastMotionY - historicalY + mRefreshFooterViewHeight) / 2);

                    mRefreshFooterView.setPadding(mRefreshFooterView.getPaddingLeft(),
                            mRefreshFooterView.getPaddingTop(),
                            mRefreshFooterView.getPaddingRight(), bottomPadding
                                    + mRefreshOriginalBottomPadding);
                }
            }
        }
    }

    private void resetHeaderPadding() {
        mRefreshHeaderView.setPadding(mRefreshHeaderView.getPaddingLeft(),
                mRefreshOriginalTopPadding, mRefreshHeaderView.getPaddingRight(),
                mRefreshHeaderView.getPaddingBottom());
    }

    private void resetFooterPadding() {
        mRefreshFooterView.setPadding(mRefreshFooterView.getPaddingLeft(),
                mRefreshFooterView.getPaddingTop(), mRefreshFooterView.getPaddingRight(),
                mRefreshOriginalBottomPadding);
    }

    private void resetHeader() {
        if (mRefreshHeaderState != TAP_TO_REFRESH) {
            mRefreshHeaderState = TAP_TO_REFRESH;

            if (mRefreshHeaderViewInfo != null) {
                resetHeaderPadding();

                mRefreshHeaderViewText.setText(mRefreshHeaderViewInfo.tabMessage);
                mRefreshHeaderViewImage.clearAnimation();
                mRefreshHeaderViewImage.setVisibility(View.GONE);
                mRefreshHeaderViewProgress.setVisibility(View.GONE);
            }
        }
    }

    private void resetFooter() {
        if (mRefreshFooterState != TAP_TO_REFRESH) {
            mRefreshFooterState = TAP_TO_REFRESH;

            if (mRefreshFooterViewInfo != null) {
                resetFooterPadding();

                mRefreshFooterViewText.setText(mRefreshFooterViewInfo.tabMessage);
                mRefreshFooterViewImage.clearAnimation();
                mRefreshFooterViewImage.setVisibility(View.GONE);
                mRefreshFooterViewProgress.setVisibility(View.GONE);
            }
        }
    }

    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p =
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (mRefreshHeaderViewInfo != null) {
            if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL
                    && mRefreshHeaderState != REFRESHING) {
                if (firstVisibleItem == 0) {
                    mRefreshHeaderViewImage.setVisibility(View.VISIBLE);
                    if ((mRefreshHeaderView.getBottom() > mRefreshHeaderViewHeight - 10 || mRefreshHeaderView
                            .getTop() >= 0) && mRefreshHeaderState != RELEASE_TO_REFRESH) {
                        mRefreshHeaderViewText.setText(mRefreshHeaderViewInfo.releaseMessage);
                        mRefreshHeaderViewImage.clearAnimation();
                        mRefreshHeaderViewImage.startAnimation(mFlipAnimation);
                        mRefreshHeaderState = RELEASE_TO_REFRESH;
                    } else if (mRefreshHeaderView.getBottom() < mRefreshHeaderViewHeight - 10
                            && mRefreshHeaderState != PULL_TO_REFRESH) {
                        mRefreshHeaderViewText.setText(mRefreshHeaderViewInfo.pullMessage);
                        if (mRefreshHeaderState != TAP_TO_REFRESH) {
                            mRefreshHeaderViewImage.clearAnimation();
                            mRefreshHeaderViewImage.startAnimation(mReverseFlipAnimation);
                        }
                        mRefreshHeaderState = PULL_TO_REFRESH;
                    }
                } else {
                    resetHeader();
                }
            } else if (mCurrentScrollState == SCROLL_STATE_FLING
                    && firstVisibleItem == 0 && mRefreshHeaderState != REFRESHING) {
                setSelection(1);
                mHeaderBounceHack = true;
            } else if (mHeaderBounceHack && mCurrentScrollState == SCROLL_STATE_FLING) {
                setSelection(1);
            }
        }

        if (mRefreshFooterViewInfo != null) {
            if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL
                    && mRefreshFooterState != REFRESHING) {
                if (firstVisibleItem + visibleItemCount == totalItemCount) {
                    mRefreshFooterViewImage.setVisibility(View.VISIBLE);
                    if ((getMeasuredHeight() - mRefreshFooterView.getTop() > mRefreshFooterViewHeight - 10 || getMeasuredHeight()
                            - mRefreshFooterView.getBottom() >= 0)
                            && mRefreshFooterState != RELEASE_TO_REFRESH) {
                        mRefreshFooterViewText.setText(mRefreshFooterViewInfo.releaseMessage);
                        mRefreshFooterViewImage.clearAnimation();
                        mRefreshFooterViewImage.startAnimation(mFlipAnimation);
                        mRefreshFooterState = RELEASE_TO_REFRESH;
                    } else if (getMeasuredHeight() - mRefreshFooterView.getTop() < mRefreshFooterViewHeight - 10
                            && mRefreshFooterState != PULL_TO_REFRESH) {
                        mRefreshFooterViewText.setText(mRefreshFooterViewInfo.pullMessage);
                        if (mRefreshFooterState != TAP_TO_REFRESH) {
                            mRefreshFooterViewImage.clearAnimation();
                            mRefreshFooterViewImage.startAnimation(mReverseFlipAnimation);
                        }
                        mRefreshFooterState = PULL_TO_REFRESH;
                    }
                } else {
                    resetFooter();
                }
            } else if (mCurrentScrollState == SCROLL_STATE_FLING
                    && (firstVisibleItem + visibleItemCount == totalItemCount)
                    && mRefreshFooterState != REFRESHING) {
                setSelectionFromTop(totalItemCount - 1, getMeasuredHeight());
                mFooterBounceHack = true;
            } else if (mFooterBounceHack && mCurrentScrollState == SCROLL_STATE_FLING) {
                setSelectionFromTop(totalItemCount - 1, getMeasuredHeight());
            }
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        mCurrentScrollState = scrollState;

        if (mCurrentScrollState == SCROLL_STATE_IDLE) {
            mHeaderBounceHack = false;
            mFooterBounceHack = false;
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    public void prepareForRefreshHeader() {
        resetHeaderPadding();

        mRefreshHeaderViewImage.setVisibility(View.GONE);
        mRefreshHeaderViewImage.clearAnimation();
        mRefreshHeaderViewProgress.setVisibility(View.VISIBLE);
        mRefreshHeaderViewText.setText(mRefreshHeaderViewInfo.loadingMessage);

        mRefreshHeaderState = REFRESHING;
    }

    public void prepareForRefreshFooter() {
        resetFooterPadding();

        mRefreshFooterViewImage.setVisibility(View.GONE);
        mRefreshFooterViewImage.clearAnimation();
        mRefreshFooterViewProgress.setVisibility(View.VISIBLE);
        mRefreshFooterViewText.setText(mRefreshFooterViewInfo.loadingMessage);

        mRefreshFooterState = REFRESHING;
    }

    public void onRefreshHeader() {
        if (mOnRefreshHeaderListener != null) {
            mOnRefreshHeaderListener.onRefresh();
        }
    }

    public void onRefreshFooter() {
        if (mOnRefreshFooterListener != null) {
            mOnRefreshFooterListener.onRefresh();
        }
    }

    public void setLastRefresh(CharSequence lastRefresh) {
        if (mRefreshHeaderViewLastRefresh != null) {
            if (lastRefresh != null) {
                mRefreshHeaderViewLastRefresh.setVisibility(View.VISIBLE);
                mRefreshHeaderViewLastRefresh.setText(lastRefresh);
            } else {
                mRefreshHeaderViewLastRefresh.setVisibility(View.GONE);
            }
        }
    }

    public void onRefreshHeaderComplete(CharSequence lastRefresh) {
        setLastRefresh(lastRefresh);
        onRefreshHeaderComplete();
    }

    public void onRefreshHeaderComplete() {
        resetHeader();

        if (mLoadFlag) {
            if (mRefreshFooterViewInfo != null && getFooterViewsCount() == 0)
                addFooterView(mRefreshFooterView);
        }

        if (mRefreshHeaderViewInfo != null && (mRefreshHeaderView.getBottom() > 0)) {
            invalidateViews();
            post(new Runnable() {

                @Override
                public void run() {
                    setSelection(1);
                }
            });
        }

        if (mLoadFlag)
            mLoadFlag = false;
    }

    public void onRefreshFooterComplete() {
        resetFooter();

        if (mRefreshFooterViewInfo != null
                && (getMeasuredHeight() - mRefreshFooterView.getTop() > 0)) {
            invalidateViews();
            if (mLoadFlag)
                post(new Runnable() {

                    @Override
                    public void run() {
                        setSelection(1);
                    }
                });
            else
                setSelectionFromTop(getCount() + getHeaderViewsCount() + getFooterViewsCount() - 1,
                        getMeasuredHeight());
        }

        if (mLoadFlag)
            mLoadFlag = false;
    }

    public void instantLoad() {
        mLoadFlag = true;

        if (mRefreshHeaderViewInfo != null) {
            if (mLoadFlag) {
                if (mRefreshFooterViewInfo != null)
                    removeFooterView(mRefreshFooterView);
            }
            prepareForRefreshHeader();
            onRefreshHeader();
        } else if (mRefreshFooterViewInfo != null) {
            mRefreshFooterState = REFRESHING;
            prepareForRefreshFooter();
            onRefreshFooter();
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        if (mRefreshHeaderViewInfo != null)
            setSelection(1);
    }

    private class OnClickHeaderListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mRefreshHeaderViewInfo != null && mRefreshHeaderState != REFRESHING) {
                prepareForRefreshHeader();
                onRefreshHeader();
            }
        }
    }

    private class OnClickFooterListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if (mRefreshFooterViewInfo != null && mRefreshFooterState != REFRESHING) {
                prepareForRefreshFooter();
                onRefreshFooter();
            }
        }
    }
}