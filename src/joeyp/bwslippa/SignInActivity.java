package joeyp.bwslippa;

import java.io.IOException;

import joeyp.bwslippa.RPCHelper.RPCListener;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SignInActivity extends Activity implements RPCListener,
									OnItemClickListener {
	
	private static final String EXTRA_ACCOUNT_TYPES = "account_types";
	private static final int REQUEST_ADD_ACCOUNT = 1001;
	
	private ListView mListView;
	private View mLoadingView;
	private AccountAdapter mAdapter;
	private AccountManager mAccountManager;
	
	private static String mAccountNotFound;
	
	private class AuthTask implements Runnable {
		
		Account mAccount;
		
		AuthTask(Account account) {
			mAccount = account;
		}
		
		public void run() {
			AccountManagerFuture<Bundle> amf = mAccountManager.getAuthToken(mAccount, "ah", null, false, null, null);
			try {
				Bundle bundle = amf.getResult();
				Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
				if(intent != null) {
                    //permission required
					onInitFailed(-1);
					startActivity(intent);
					return;
				}
				String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				//avoid token expired, invalidate latest token first
				mAccountManager.invalidateAuthToken(mAccount.type, auth_token);
				//get token again
				amf = mAccountManager.getAuthToken(mAccount, "ah", null, false, null, null);
				bundle = amf.getResult();
				auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				
				RPCHelper helper = RPCHelper.getInstance();
		        helper.registerListenter(SignInActivity.this);
		        helper.init(auth_token);
		        
			} catch (OperationCanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.signin);
		
		Resources res = getResources();
		
		mAccountManager = AccountManager.get(getApplicationContext());
		
        mAccountNotFound = res.getString(R.string.account_not_found);
        mLoadingView = findViewById(R.id.loading);
        int color = res.getColor(R.color.main_color);
        ProgressBar progress = (ProgressBar) mLoadingView.findViewById(R.id.progress);
        progress.getIndeterminateDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
        
		mListView = (ListView) findViewById(R.id.list);
		mAdapter = new AccountAdapter(this);
		queryAccounts();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ADD_ACCOUNT) {
			queryAccounts();
		}
	}
	
	private void queryAccounts() {
		final Account[] accounts = mAccountManager.getAccountsByType("com.google");
        mAdapter.setData(accounts);
        mAdapter.notifyDataSetChanged();
	}
	
	private static class AccountAdapter extends BaseAdapter {
		
		private Context mContext;
		private Account[] mAccounts;
		
		public AccountAdapter(Context context) {
			mContext = context;
			mAccounts = new Account[0];
		}
		
		public void setData(Account[] accounts) {
			mAccounts = accounts;
		}

		@Override
		public int getCount() {
			return mAccounts.length + 1;
		}

		@Override
		public Object getItem(int position) {
			return mAccounts[position];
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
				tv = (TextView) inflater.inflate(R.layout.account_list_item, parent, false);
			} else
				tv = (TextView) convertView;
			if(position == (getCount() -1)) {
				tv.setText(mAccountNotFound);
			} else {
				tv.setText(mAccounts[position].name);
			}
			return tv;
		}
		
	}

	@Override
	public void onInit() {
		Toast.makeText(SignInActivity.this, "Welcome.", Toast.LENGTH_SHORT).show();
    	Intent intent = new Intent();
    	intent.setClass(SignInActivity.this, BWSlippa.class);
    	startActivity(intent);
    	finish();
	}

	@Override
	public void onInitFailed(int errorCode) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				mLoadingView.setVisibility(View.GONE);
			}
		});
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//add new account
		if(position >= (parent.getCount() - 1)) {
			Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
			intent.putExtra(EXTRA_ACCOUNT_TYPES, new String[] { "com.google" });
			startActivityForResult(intent, REQUEST_ADD_ACCOUNT);
			return;
		}
		
		mLoadingView.setVisibility(View.VISIBLE);
		
		final Account account = (Account) parent.getItemAtPosition(position);
		Worker.get().post(new AuthTask(account));
	}
	
}
