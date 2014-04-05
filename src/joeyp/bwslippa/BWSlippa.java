package joeyp.bwslippa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import joeyp.bwslippa.ItemManager.OnItemDataChangedListener;
import joeyp.bwslippa.RPCHelper.RPCCallback;
import joeyp.bwslippa.RPCHelper.RPCListener;
import joeyp.bwslippa.view.CalendarDialog;
import joeyp.bwslippa.view.MessageDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BWSlippa extends Activity implements OnItemDataChangedListener,
										RPCListener, OnNavigationListener {
	
	public static final String TAG = BWSlippa.class.getSimpleName();
	
	public static final String PREFS_SETTINGS = "settings";
	public static final String KEY_PRIMARY_ACCOUNT = "primary_account";
	
	public static final String EXTRA_ACCOUNT = "extra_account";
	//reserve
	public static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";
	public static final String EXTRA_CUSTOMER_KEY = "extra_customer_key";
	public static final String EXTRA_CUSTOMER_PHONE = "extra_customer_phone";
	public static final String EXTRA_ITEM_NAME = "extra_item_name";
	public static final String EXTRA_ITEM_KEY = "extra_item_key";
	public static final String EXTRA_START_DATE = "extra_start_date";
	
	public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	
	public static final int REQUEST_ID_QUERY_CUSTOMER = 1001;
	
	/**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "406462527679";
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
	//navigation drawer
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private SpinnerAdapter mSpinnerAdapter;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private CalendarDialog mDatePicker;
    
    private List<TagInfo> mTags;
    
    private class AuthTask implements Runnable {
		
		Account mAccount;
		AccountManager mAccountManager;
		
		AuthTask(AccountManager manager, Account account) {
			mAccountManager = manager;
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
				String temp_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				//avoid token expired, invalidate latest token first
				mAccountManager.invalidateAuthToken(mAccount.type, temp_token);
				//get token again
				amf = mAccountManager.getAuthToken(mAccount, "ah", null, false, null, null);
				bundle = amf.getResult();
				final String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						onGetToken(auth_token);
					}
				});
		        
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
	}
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		RPCHelper rpc = RPCHelper.getInstance();
		rpc.registerListenter(this);
		
		Bundle bundle = getIntent().getExtras();
		if(bundle != null) {
			Log.d("joey", "select account");
			Account account = bundle.getParcelable(EXTRA_ACCOUNT);
			if(account != null) {
				AccountManager accountManager = AccountManager.get(getApplicationContext());
				Worker.get().post(new AuthTask(accountManager, account));
				setPrimaryAccount(this, account);
			}
		} else {
			Log.d("joey", "load default account");
			loadDefaultAccount();
		}
		
		// Check device for Play Services APK.
	    if (checkPlayServices()) {
	    	gcm = GoogleCloudMessaging.getInstance(this);
            //regid = getRegistrationId(context);

            //if (regid.isEmpty()) {
//                registerInBackground();
            //}
	    }
		
		mTitle = mDrawerTitle = getTitle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerAdapter = new DrawerAdapter(this);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        
        Resources res = getResources();
        mSpinnerAdapter = new SpinnerAdapter(this);
        List<ItemFilter> data = new ArrayList<ItemFilter>();
		data.add(new ItemFilter(ItemFilter.TAG_ALL, res.getString(R.string.tag_all)));
		data.add(new ItemFilter(ItemFilter.TAG_RESERVED, res.getString(R.string.tag_reserved)));
		data.add(new ItemFilter(ItemFilter.TAG_AVAILABLE, res.getString(R.string.tag_available)));
		mSpinnerAdapter.setData(data);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
//            selectItem(0);
        }
        
        mTags = new ArrayList<TagInfo>();
        
        ItemManager im = ItemManager.getInstance();
        im.registerListener(this);
        
        mDatePicker = new CalendarDialog();
        
        loadFragment();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkPlayServices();
	}
	
    @Override
	protected void onDestroy() {
		super.onDestroy();
		RPCHelper rpc = RPCHelper.getInstance();
		rpc.unregisterListenter(this);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
         // The action bar home/up action should open or close the drawer.
         // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch(item.getItemId()) {
        case 1:
        	
//        case R.id.action_add:
//        	Intent intent = new Intent();
//        	intent.setClass(BWSlippa.this, BookingActivity.class);
//        	startActivity(intent);
//        	return true;
//        case R.id.action_date:
//        	mDatePicker.show(getFragmentManager(), "datePicker");
//        	AlertDialog.Builder builder = new AlertDialog.Builder(BWSlippa.this);
//        	LayoutInflater inflater = getLayoutInflater();
//        	View v = inflater.inflate(R.layout.date_picker, null);
//        	builder.setView(v)
//        	       .setTitle("test");
//
//        	AlertDialog dialog = builder.create();
//        	dialog.show();
        	return true;
//        case R.id.action_websearch:
//            // create intent to perform web search for this planet
//            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
//            intent.putExtra(SearchManager.QUERY, getActionBar().getTitle());
//            // catch event that there's no activity to handle intent
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                startActivity(intent);
//            } else {
//                Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show();
//            }
//            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
	/* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
//        setTitle(mTags.get(position).name);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
    
    private void onGetToken(String token) {
    	RPCHelper helper = RPCHelper.getInstance();
        helper.init(token);
    }
    
    private void loadFragment() {
    	// update the main content by replacing fragments
    	Fragment fragment = new StoreFragment();
//        Bundle args = new Bundle();
//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
//        fragment.setArguments(args);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Fragment that appears in the "content_frame", shows a planet
     */
    public static class PlanetFragment extends Fragment {
        public static final String ARG_PLANET_NUMBER = "planet_number";

        public PlanetFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.store, container, false);

//            int i = getArguments().getInt(ARG_PLANET_NUMBER);
//            String planet = getResources().getStringArray(R.array.planets_array)[i];
//
//            int imageId = getResources().getIdentifier(planet.toLowerCase(Locale.getDefault()),
//                            "drawable", getActivity().getPackageName());
//            ((ImageView) rootView.findViewById(R.id.image)).setImageResource(imageId);
//            getActivity().setTitle(planet);
            return rootView;
        }
    }

    
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    
    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }
    
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(BWSlippa.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    
    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    
    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
    	
        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(BWSlippa.this);
                    }
                    String regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    //storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
            	Toast.makeText(BWSlippa.this, msg, Toast.LENGTH_LONG).show();
                //mDisplay.append(msg + "\n");
            	((EditText)findViewById(R.id.text_reg_id)).setText(msg);
            }
        }.execute(null, null, null);
        
    }
    
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
    }
    
    private static class DrawerAdapter extends BaseAdapter {
		
		private Context mContext;
		private List<ItemFilter> mData;
		
		public DrawerAdapter(Context context) {
			mContext = context;
			mData = new ArrayList<ItemFilter>();
		}
		
		public void setData(List<ItemFilter> data) {
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
				tv = (TextView) inflater.inflate(R.layout.drawer_list_item, parent, false);
			} else
				tv = (TextView) convertView;
			tv.setText(mData.get(position).name);
			return tv;
		}
		
	}

	@Override
	public void onItemDataChanged(List<ItemDetail> items) {
		Resources res = getResources();
		ItemManager im = ItemManager.getInstance();
		List<ItemFilter> data = new ArrayList<ItemFilter>();
		data.add(new ItemFilter(ItemFilter.TAG_ALL, res.getString(R.string.tag_all)));
		List<TagInfo> tags = im.getTags();
		for(TagInfo t : tags) {
			data.add(new ItemFilter(ItemFilter.TAG, t.name, t));
		}
		mDrawerAdapter.setData(data);
		mDrawerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onInit() {
		Toast.makeText(this, "Welcome.", Toast.LENGTH_SHORT).show();
		ItemManager.getInstance().forceSync();
	}

//	@Override
//	public void onInitFailed(int errorCode) {
//		runOnUiThread(new Runnable() {
//			
//			@Override
//			public void run() {
//				mLoadingView.setVisibility(View.GONE);
//			}
//		});
//		
//	}

	@Override
	public void onInitFailed(int errorCode) {
			Log.w(TAG, "failed=" + errorCode);
			MessageDialog dialog = new MessageDialog();
			dialog.show(getFragmentManager(), "message dialog");
			Toast.makeText(this, "error:" + errorCode, Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		ItemFilter filter = (ItemFilter) mSpinnerAdapter.getItem(position);
		ItemManager.getInstance().setFilter(filter);
		return true;
	}
    
	private static class SpinnerAdapter extends BaseAdapter {
		
		private Context mContext;
		private List<ItemFilter> mData;
		
		public SpinnerAdapter(Context context) {
			mContext = context;
			mData = new ArrayList<ItemFilter>();
		}
		
		public void setData(List<ItemFilter> data) {
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
				tv = (TextView) inflater.inflate(R.layout.spinner, parent, false);
			} else
				tv = (TextView) convertView;
			tv.setText(mData.get(position).name);
			return tv;
		}
		
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			TextView tv;
			if(convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext
				        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				tv = (TextView) inflater.inflate(R.layout.drawer_list_item, parent, false);
			} else
				tv = (TextView) convertView;
			tv.setText(mData.get(position).name);
			return tv;
		}
		
	}
	
	private void loadDefaultAccount() {
		String accountName = getPrimaryAccountName(this);
		if(accountName != null) {
			AccountManager accountManager = AccountManager.get(getApplicationContext());
			final Account[] accounts = accountManager.getAccountsByType("com.google");
			for(Account account : accounts) {
				if(account.name.equals(accountName)) {
					Worker.get().post(new AuthTask(accountManager, account));
					return;
				}
			}
		}
		
		Log.i(TAG, "account not exists");
		//acount not exists
		Intent intent = new Intent();
		intent.setClass(this, SignInActivity.class);
		startActivity(intent);
		finish();
	}
	
	public static void setPrimaryAccount(Context context, Account account) {
		SharedPreferences settings = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.putString(KEY_PRIMARY_ACCOUNT, account.name);
		editor.apply();
	}
	
	public static String getPrimaryAccountName(Context context) {
		SharedPreferences settings = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
		return settings.getString(KEY_PRIMARY_ACCOUNT, null);
	}
}
