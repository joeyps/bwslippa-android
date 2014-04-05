package joeyp.bwslippa2;

import android.os.Handler;

public class Alarm implements Runnable {
	
	private Handler mHandler = new Handler();
	private OnAlarmListener mListener;
	
	public interface OnAlarmListener {
		public void onAlarm();
	}
	
	public void setAlarmListener(OnAlarmListener listener) {
		mListener = listener;
	}
	
	public void start(long delayMillis) {
		mHandler.postDelayed(this, delayMillis);
	}
	
	public void cancel() {
		mHandler.removeCallbacks(this);
	}

	@Override
	public void run() {
		if(mListener != null)
			mListener.onAlarm();
	}
}
