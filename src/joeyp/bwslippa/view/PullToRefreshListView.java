package joeyp.bwslippa.view;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import joeyp.bwslippa.R;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;

public class PullToRefreshListView extends ListView {
	
	private static final boolean DEBUG = false;
    private static final String LOG_TAG = "PullToRefreshListView";
	
	private static final float DEFAULT_REFRESH_SCROLL_DISTANCE = 10f;
	
	private int mTouchSlop;
	private boolean mIsBeingDragged = false;
	private boolean mIsRefreshing = false;
	private boolean mHandlingTouchEventFromDown = false;
	private float mInititalX = -1;
	private float mInititalY = -1;
	private float mPullBeginY = -1;
	
	private View mHeaderView;
	private SmoothProgressBar mProgressBar;
	private OnRefreshListener mListener;
	
	public interface OnRefreshListener {
		public void onRefresh();
	}

	public PullToRefreshListView(Context context) {
		super(context);
		setupViews(context);
	}
	
	public PullToRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupViews(context);
	}
	
	public PullToRefreshListView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setupViews(context);
	}
	
	private void setupViews(Context context) {
		ViewConfiguration vc = ViewConfiguration.get(getContext());
	    mTouchSlop = vc.getScaledTouchSlop();
	    Log.d(LOG_TAG, "mTouchSlop=" + mTouchSlop);
	    
		final Activity activity = (Activity) context;
		final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

        // Create Header view and then add to Decor View
        mHeaderView = LayoutInflater.from(context).inflate(
                R.layout.pulldown_header, decorView, false);
        mHeaderView.setVisibility(View.GONE);
        mProgressBar = (SmoothProgressBar) mHeaderView.findViewById(R.id.progress);
        
        // Now HeaderView to Activity
        decorView.post(new Runnable() {
            @Override
            public void run() {
                if (decorView.getWindowToken() != null) {
                    // The Decor View has a Window Token, so we can add the HeaderView!

                 // Get the Display Rect of the Decor View
                	Rect rect = new Rect();
                    activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

                    // Honour the requested layout params
                    int width = WindowManager.LayoutParams.MATCH_PARENT;
                    int height = WindowManager.LayoutParams.WRAP_CONTENT;
                    ViewGroup.LayoutParams requestedLp = mHeaderView.getLayoutParams();
                    if (requestedLp != null) {
                        width = requestedLp.width;
                        height = requestedLp.height;
                    }

                    // Create LayoutParams for adding the View as a panel
                    WindowManager.LayoutParams wlp = new WindowManager.LayoutParams(width, height,
                            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                            PixelFormat.TRANSLUCENT);
                    wlp.x = 0;
                    wlp.y = rect.top;
                    wlp.gravity = Gravity.TOP;

                    //FIXME
                    mHeaderView.setTag(wlp);
                    activity.getWindowManager().addView(mHeaderView, wlp);
                } else {
                    // The Decor View doesn't have a Window Token yet, post ourselves again...
                    decorView.post(this);
                }
            }
        });
	}
	
	public boolean onInterceptTouchEvent (MotionEvent ev) {
//		Log.d(LOG_TAG, "onInterceptTouchEvent=" + ev.getAction());
		
		if(mIsRefreshing)
			return super.onInterceptTouchEvent(ev);
		
		final float x = ev.getX();
		final float y = ev.getY();
		
		switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
        	if(isReadyForPull()) {
	        	mInititalX = x;
	        	mInititalY = y;
        	}
        	break;
        case MotionEvent.ACTION_MOVE:
        	if(!mIsBeingDragged && mInititalY > 0) {
            	float diffX = Math.abs(x - mInititalX);
            	float diffY = y - mInititalY;
            	if(diffY > diffX && diffY > mTouchSlop) {
            		onPullStarted(y);
            	} else if (diffY < -mTouchSlop) {
            		resetPullState();
            	}
            }
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
        	if(mIsBeingDragged && !mIsRefreshing)
        		resetPullState();
        	break;
		}
		
		if(mIsBeingDragged)
			return mIsBeingDragged;
		
		return super.onInterceptTouchEvent(ev);
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent ev) {
//		Log.d(LOG_TAG, "onTouchEvent=" + ev.getAction());
		final float x = ev.getX();
		final float y = ev.getY();
		
		if(mIsRefreshing)
			return super.onTouchEvent(ev);
		
		// Record whether our handling is started from ACTION_DOWN
        if (ev.getAction() == MotionEvent.ACTION_DOWN && isReadyForPull()) {
            mHandlingTouchEventFromDown = true;
        }

        // If we're being called from ACTION_DOWN then we must call through to
        // onInterceptTouchEvent until it sets mIsBeingDragged
        if (mHandlingTouchEventFromDown && !mIsBeingDragged) {
        	Log.d(LOG_TAG, "from onTouchEvent=" + ev.getAction());
        	onInterceptTouchEvent(ev);
            return true;
        }
		
		switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            break;
        
        case MotionEvent.ACTION_MOVE:
        	if(mIsRefreshing)
        		break;
        	float diffY = y - mInititalY;
            if(mIsBeingDragged && diffY > -mTouchSlop) {
            	onPull(y);
            } else {
            	resetPullState();
            }
            break;
        case MotionEvent.ACTION_UP:
        	if(mIsBeingDragged && !mIsRefreshing)
        		resetPullState();
            break;
		}
		
        return super.onTouchEvent(ev);
    }
	
	private boolean isReadyForPull() {
		boolean ready = false;
		// First we check whether we're scrolled to the top
        if (getCount() == 0) {
            ready = true;
        } else if (getFirstVisiblePosition() == 0) {
            final View firstVisibleChild = getChildAt(0);
            ready = firstVisibleChild != null && firstVisibleChild.getTop() >= 0;
        }
        
        Log.d(LOG_TAG, "isReadyForPull=" + ready);
        return ready;
	}
	
	private void onPullStarted(float y) {
		Log.d(LOG_TAG, "onPullStarted y=" + y);
		mPullBeginY = y;
		mIsBeingDragged = true;
		mHeaderView.setVisibility(View.VISIBLE);
	}
	
	private void onPull(float y) {
		float diffY = y - mPullBeginY;
		
		float pxScrollForRefresh = mTouchSlop * DEFAULT_REFRESH_SCROLL_DISTANCE;
		Log.d(LOG_TAG, "onPull d=" + diffY + " pxScrollForRefresh=" + pxScrollForRefresh);
		if(diffY > pxScrollForRefresh) {
			onRefreshStarted();
		} else {
			float progress = diffY / pxScrollForRefresh;			
			mProgressBar.setProgress(Math.round(mProgressBar.getMax() * progress));
		}
	}
	
	public void startRefresh() {
		onRefreshStarted();
	}
	
	private void onRefreshStarted() {
		Log.d(LOG_TAG, "onRefresh");
		mProgressBar.setProgress(mProgressBar.getMax());
		mProgressBar.setIndeterminate(true);
		mIsRefreshing = true;
		onRefresh();
	}
	
	protected void onRefresh() {
		if(mListener != null) {
			mListener.onRefresh();
		} else {
			postDelayed(new Runnable() {
				
				@Override
				public void run() {
					onRefreshCompleted();
				}
			}, 3000);
		}
	}
	
	public void setRefreshComplete() {
		onRefreshCompleted();
	}
	
	private void onRefreshCompleted() {
		resetPullState();
	}
	
	private void resetPullState() {
		Log.d(LOG_TAG, "resetPullState");
		if(mProgressBar == null)
			return;
		mIsBeingDragged = false;
		mIsRefreshing = false;
		mHandlingTouchEventFromDown = false;
		mInititalX = mInititalY = mPullBeginY = -1;
		mProgressBar.setProgress(0);
		mProgressBar.setIndeterminate(false);
		mHeaderView.setVisibility(View.GONE);
	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		mListener = listener;
	}
}
