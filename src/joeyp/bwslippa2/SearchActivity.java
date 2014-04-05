package joeyp.bwslippa2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import joeyp.bwslippa2.Alarm.OnAlarmListener;
import joeyp.bwslippa2.RPCHelper.RPCCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class SearchActivity extends Activity {
	
	private static final int QUERY_DEFERRED_TIME = 500;
	
	private ListView mList;
	private ResultAdapter mAdapter;
	private EditText mTextPattern;
	private String mPattern;
	private Alarm mAlarm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		mList = (ListView) findViewById(R.id.list);
		mAdapter = new ResultAdapter(this);
		mList.setAdapter(mAdapter);
		mList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				CustomerInfo c = (CustomerInfo) parent.getItemAtPosition(position);
				Intent intent = new Intent();
				intent.putExtra(BWSlippa.EXTRA_CUSTOMER_KEY, c.key);
				intent.putExtra(BWSlippa.EXTRA_CUSTOMER_NAME, c.name);
				intent.putExtra(BWSlippa.EXTRA_CUSTOMER_PHONE, c.phone);
				setResult(RESULT_OK, intent);     
				finish();
			}
		});
		
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
							final List<CustomerInfo> data = new ArrayList<CustomerInfo>();
							for (int i = 0; i < jAry.length(); i++) {
								CustomerInfo c = CustomerInfo.parse(jAry.getJSONObject(i));
								data.add(c);
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

		mTextPattern = (EditText) findViewById(R.id.text_pattern);
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
	
	
	private static class ResultAdapter extends BaseAdapter {
		
		private Context mContext;
		private List<CustomerInfo> mData;
		
		public ResultAdapter(Context context) {
			mContext = context;
			mData = new ArrayList<CustomerInfo>();
		}
		
		public void setData(List<CustomerInfo> data) {
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
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv;
			if(convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
				        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				tv = (TextView) inflater.inflate(R.layout.search_list_item, parent, false);
			} else
				tv = (TextView) convertView;
			tv.setText(mData.get(position).name);
			return tv;
		}
		
	}
	
}
