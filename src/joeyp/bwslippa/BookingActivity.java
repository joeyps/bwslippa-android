package joeyp.bwslippa;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import joeyp.bwslippa.RPCHelper.RPCCallback;
import joeyp.bwslippa.view.CalendarDialog;
import joeyp.bwslippa.view.DateTimeView;
import joeyp.bwslippa.view.CalendarDialog.OnDateChangedListener;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class BookingActivity extends Activity {
	private String mItemKey;
	private DateTimeView mDateFrom;
	private DateTimeView mDateTo;
	private int mPersonCount = 1;
	
	private String mCustomerKey;
	private TextView mCustomerName;
	private EditText mCustomerPhone;
	private TextView mNumberOfPerson;
	private String mFormatHowManyPerson;
	
	private CalendarDialog mDatePicker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.booking);
	    
		Bundle extra = getIntent().getExtras();
		if(extra == null)
			finish();
		
		ActionBar actionBar = getActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);
		
		Resources res = getResources();
		
		mFormatHowManyPerson = getString(R.string.how_many_person);
		
		mItemKey = extra.getString(BWSlippa.EXTRA_ITEM_KEY);
		TextView txtItemName = (TextView) findViewById(R.id.text_item_name);
		String itemName = String.format(res.getString(R.string.reserve_item), extra.getString(BWSlippa.EXTRA_ITEM_NAME)); 
		txtItemName.setText(itemName);
		Date date = (Date) extra.getSerializable(BWSlippa.EXTRA_START_DATE);
		
		mCustomerName = (TextView) findViewById(R.id.text_customer_name);
		mCustomerPhone = (EditText) findViewById(R.id.text_customer_phone);
		
		Button btnIncrease = (Button) findViewById(R.id.button_increase);
		Button btnDecrease = (Button) findViewById(R.id.button_decrease);
		mNumberOfPerson = (TextView) findViewById(R.id.text_person_count);
		setPersonCount(mPersonCount);
		mDatePicker = new CalendarDialog();
		
		mDateFrom = (DateTimeView) findViewById(R.id.date_start);
		mDateTo = (DateTimeView) findViewById(R.id.date_end);
		mDateFrom.setDate(date);
		mDateTo.setDate(date);
		mDateTo.setActionText(res.getString(R.string.check_out));
		
		mDateFrom.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDatePicker.setOnDateChangedListener(new OnDateChangedListener() {
					
					@Override
					public void onDateChanged(Date date) {
						mDateFrom.setDate(date);
					}
				});
				mDatePicker.setDate(mDateFrom.getDate());
				mDatePicker.show(getFragmentManager(), "DatePicker");
			}
		});
		
		mDateTo.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDatePicker.setOnDateChangedListener(new OnDateChangedListener() {
					
					@Override
					public void onDateChanged(Date date) {
						mDateTo.setDate(date);
					}
				});
				mDatePicker.setDate(mDateTo.getDate());
				mDatePicker.show(getFragmentManager(), "DatePicker");
			}
		});
		
		btnIncrease.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setPersonCount(mPersonCount + 1);
			}
		});
		
		btnDecrease.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				setPersonCount(mPersonCount - 1);
			}
		});
		
		ImageButton btnFindCustomer = (ImageButton) findViewById(R.id.button_find_customer);
		btnFindCustomer.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(BookingActivity.this, SearchActivity.class);
				startActivityForResult(intent, BWSlippa.REQUEST_ID_QUERY_CUSTOMER);
			}
		});
		
		Button btnSend = (Button) findViewById(R.id.button_send);
		btnSend.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				RPCHelper rpc = RPCHelper.getInstance();
				rpc.post(RPCHelper.API_RESERVE, new RPCCallback() {
					
					@Override
					public void onFail() {
						Toast.makeText(BookingActivity.this, "failed", Toast.LENGTH_SHORT).show();
					}
					
					@Override
					public void onCallback(JSONArray obj) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onCallback(JSONObject obj) {
						Toast.makeText(BookingActivity.this, "success", Toast.LENGTH_SHORT).show();
						setResult(RESULT_OK, null);
						finish();
					}
				}, true,
				null,
				mCustomerKey == null,
				mCustomerKey == null ? mCustomerName.getText() : mCustomerKey,
				"",
				mCustomerPhone.getText().toString(),
				"",
				0,
				Settings.DATE_FORMAT.format(mDateFrom.getDate()),
				Settings.DATE_FORMAT.format(mDateTo.getDate()),
				mItemKey,
				"0",
				0,
				"");
			}
		});
		
		Button btnCancel = (Button) findViewById(R.id.button_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();	
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == BWSlippa.REQUEST_ID_QUERY_CUSTOMER) {
			if(resultCode == RESULT_OK) {
				mCustomerName.setText(data.getStringExtra(BWSlippa.EXTRA_CUSTOMER_NAME));
				mCustomerPhone.setText(data.getStringExtra(BWSlippa.EXTRA_CUSTOMER_PHONE));
				mCustomerKey = data.getStringExtra(BWSlippa.EXTRA_CUSTOMER_KEY);
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	   switch (item.getItemId()) {
	     case android.R.id.home:
	        finish();
	        return true;
	     default:
	        return super.onOptionsItemSelected(item);
	   }
	}
	
	private void setPersonCount(int count) {
		count = Math.max(1, count);
		mPersonCount = count;
		mNumberOfPerson.setText(String.format(mFormatHowManyPerson, mPersonCount));
	}
	
	
}
