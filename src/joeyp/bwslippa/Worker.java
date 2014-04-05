package joeyp.bwslippa;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class Worker extends Handler {
	
	private static Worker mWorker;
	
	private static HandlerThread mThread;

	private Worker(Looper looper) {
		super(looper);
	}
	
	public static Worker get() {
		if(mWorker == null) {
			mThread = new HandlerThread("worker");
			mThread.start();
			mWorker = new Worker(mThread.getLooper());
		}
		return mWorker;
	}
	
	public void kill() {
		if(mThread != null)
			mThread.getLooper().quit();
	}
}
