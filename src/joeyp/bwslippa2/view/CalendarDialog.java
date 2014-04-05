package joeyp.bwslippa2.view;

import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog.OnDateSetListener;
import android.os.Bundle;
import android.widget.DatePicker;

public class CalendarDialog extends DialogFragment implements OnDateSetListener {
	
	private OnDateChangedListener mListener;
	
	public static interface OnDateChangedListener {
		public void onDateChanged(Date date);
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

	@Override
	public void onDateSet(DatePicker view, int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day);
		if(mListener != null)
			mListener.onDateChanged(calendar.getTime());
	}
	
	public void setOnDateChangedListener(OnDateChangedListener listener) {
		mListener = listener;
	}
}