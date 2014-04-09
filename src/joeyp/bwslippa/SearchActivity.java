package joeyp.bwslippa;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import joeyp.bwslippa.Alarm.OnAlarmListener;
import joeyp.bwslippa.RPCHelper.RPCCallback;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class SearchActivity extends Activity implements OnItemClickListener {
	
	private static final int QUERY_DEFERRED_TIME = 300;
	
	private ListView mList;
	private ResultAdapter mAdapter;
	private EditText mTextPattern;
	private String mPattern;
	private Alarm mAlarm;
	
	private String formatAdd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		ActionBar actionBar = getActionBar();

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		View v = LayoutInflater.from(this).inflate(R.layout.actionbar_search, null); // layout which contains your button.
		actionBar.setCustomView(v, lp);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true);
		mTextPattern = (EditText) v.findViewById(R.id.text_pattern);
		
		formatAdd = getString(R.string.add_customer);
		
		mList = (ListView) findViewById(R.id.list);
		mList.setEmptyView(findViewById(R.id.empty));
		mAdapter = new ResultAdapter(this);
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(this);
		
		mAlarm = new Alarm();
		mAlarm.setAlarmListener(new OnAlarmListener() {
			
			@Override
			public void onAlarm() {
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new NameValuePair() {
					
					@Override
					public String getValue() {
						return mPattern;
					}
					
					@Override
					public String getName() {
						return "term";
					}
				});
				
				RPCHelper rpc = RPCHelper.getInstance();
				rpc.call(RPCHelper.API_QUERY_CUSTOMER, params, new RPCCallback() {
					
					@Override
					public void onFail() {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void onCallback(JSONObject obj) {
						
					}

					@Override
					public void onCallback(JSONArray jAry) {
						try {
							final List<CustomerResult> data = new ArrayList<CustomerResult>();
							if(!TextUtils.isEmpty(mPattern)) {
								CustomerResult rNew = new CustomerResult();
								CustomerInfo add = new CustomerInfo();
								add.name = mPattern;
								rNew.customer = add;
								rNew.textPrimary = String.format(formatAdd, add.name);
								data.add(rNew);
							}
							for (int i = 0; i < jAry.length(); i++) {
								CustomerResult r = new CustomerResult();
								CustomerInfo c = CustomerInfo.parse(jAry.getJSONObject(i));
								r.customer = c;
								r.textPrimary = c.name;
								r.textSecondary = c.phone;
								data.add(r);
							}
							
							runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									mAdapter.setData(data);
									mAdapter.notifyDataSetChanged();
								}
							});
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
			}
		});

		
		mTextPattern.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				mPattern = s.toString();
				mAlarm.cancel();
				mAlarm.start(QUERY_DEFERRED_TIME);
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}
		});
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
	
	private static class CustomerResult {
		CustomerInfo customer;
		String textPrimary;
		String textSecondary;
	}
	
	private static class ResultAdapter extends BaseAdapter {
		
		private static final int TYPE_1_LINE = 0;
		private static final int TYPE_2_LINE = 1;
		private static final int TYPE_COUNT = 2;
		
		private Context mContext;
		private List<CustomerResult> mData;
		
		public ResultAdapter(Context context) {
			mContext = context;
			mData = new ArrayList<CustomerResult>();
		}
		
		public void setData(List<CustomerResult> data) {
			mData = data;
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public Object getItem(int position) {
			return mData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public int getItemViewType(int position) {
			CustomerResult d = mData.get(position);
			boolean hasPhone = !TextUtils.isEmpty(d.customer.phone);
			return hasPhone ? TYPE_2_LINE : TYPE_1_LINE;
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_COUNT;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			CustomerResult r = mData.get(position);
			boolean hasPhone = !TextUtils.isEmpty(r.customer.phone);
			if(convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
				        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				if(hasPhone)
					v = inflater.inflate(R.layout.list_item_2_line, parent, false);
				else
					v = inflater.inflate(R.layout.list_item_1_line, parent, false);
			} else
				v = convertView;
			TextView textPrimary = (TextView) v.findViewById(R.id.text_primary);
			textPrimary.setText(mData.get(position).textPrimary);
			if(hasPhone) {
				TextView textSecondary = (TextView) v.findViewById(R.id.text_secondary);
				textSecondary.setText(mData.get(position).textSecondary);
			}
			return v;
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		CustomerResult r = (CustomerResult) parent.getItemAtPosition(position);
		CustomerInfo c = r.customer;
		Intent intent = new Intent();
		intent.putExtra(BWSlippa.EXTRA_CUSTOMER_KEY, c.key);
		intent.putExtra(BWSlippa.EXTRA_CUSTOMER_NAME, c.name);
		intent.putExtra(BWSlippa.EXTRA_CUSTOMER_PHONE, c.phone);
		setResult(RESULT_OK, intent);     
		finish();
	}
	
}
