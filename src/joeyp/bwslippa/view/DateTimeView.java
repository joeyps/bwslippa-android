package joeyp.bwslippa.view;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import joeyp.bwslippa.R;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DateTimeView extends LinearLayout {
	
	private Date mDate;
	private TextView mTextAction;
	private TextView mTextDate;

	public DateTimeView(Context context) {
		super(context, null);
		setupViews(context);
	}
	
	public DateTimeView(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
		setupViews(context);
	}
	
	public DateTimeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setupViews(context);
	}
	
	private void setupViews(Context context) {
		setOrientation(VERTICAL);
		LayoutInflater.from(context).inflate(R.layout.datetime, this);
		
		mTextAction = (TextView) findViewById(R.id.text_action);
		mTextDate = (TextView) findViewById(R.id.text_date);
		
		Calendar cal = Calendar.getInstance();
		setDate(cal.getTime());
	}
	
	public void setActionText(String text) {
		mTextAction.setText(text);
	}
	
	public void setDate(Date date) {
		mDate = date;
		String pattern = Settings.System.getString(getContext().getContentResolver(), Settings.System.DATE_FORMAT);
		Format format = null;
		if (TextUtils.isEmpty(pattern)) {
			format = android.text.format.DateFormat.getMediumDateFormat(getContext());
		} else {
			pattern = pattern.replaceFirst("[,/ ]*y+[/]?", "");
			format = new SimpleDateFormat(pattern);
		}
		mTextDate.setText(format.format(mDate));
	}
	
	public Date getDate() {
		return mDate;
	}

}
