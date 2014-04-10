package joeyp.bwslippa.view;

import java.util.Calendar;
import java.util.Date;

import joeyp.bwslippa.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CalendarSelector extends LinearLayout {

	private Date mDate;
	private Button mButtonNext;
	private Button mButtonPrev;
	private TextView mTextDate;
	private GridView mGridView;

	public CalendarSelector(Context context) {
		super(context);
		setupViews(context);
	}
	
	public CalendarSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupViews(context);
	}
	
	public CalendarSelector(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setupViews(context);
	}
	
	private void setupViews(Context context) {
		setOrientation(VERTICAL);
		LayoutInflater.from(context).inflate(R.layout.calendar, this);
		
		mButtonNext = (Button) findViewById(R.id.button_next);
		mButtonPrev = (Button) findViewById(R.id.button_prev);
		mTextDate = (TextView) findViewById(R.id.text_date);
		mGridView = (GridView) findViewById(R.id.grid);
		
		Calendar cal = Calendar.getInstance();
		setDate(cal.getTime());
	}
	
	public void setDate(Date date) {
		
	}
	
	public void prevMonth() {
		
	}
	
	public void nextMonth() {
		
	}
	
	private class DateAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
