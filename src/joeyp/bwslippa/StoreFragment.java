package joeyp.bwslippa;

import java.util.ArrayList;
import java.util.List;

import joeyp.bwslippa.ItemManager.OnItemDataChangedListener;
import joeyp.bwslippa.view.PullToRefreshListView;
import joeyp.bwslippa.view.PullToRefreshListView.OnRefreshListener;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class StoreFragment extends Fragment implements
								OnItemDataChangedListener, OnRefreshListener {
	
	public static final int REQUEST_BOOKING = 1002;
	
	private PullToRefreshListView mList;
	private ItemAdapter mAdapter;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.store, container, false);
        
        mList = (PullToRefreshListView) rootView.findViewById(R.id.list);
        mList.setEmptyView(rootView.findViewById(R.id.empty));
        mList.setOnRefreshListener(this);
//        mList.setOnItemClickListener(new OnItemClickListener() {
//
//			@Override
//			public void onItemClick(AdapterView<?> list, View arg1, int position,
//					long arg3) {
//				
//				ItemDetail d = (ItemDetail) list.getItemAtPosition(position);
//				if(d.reservations.size() > 0) {
//					
//				}
//			}
//		});
        mAdapter = new ItemAdapter(this);
        mList.setAdapter(mAdapter);

        ItemManager im = ItemManager.getInstance();
        im.registerListener(this);
        return rootView;
    }
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_BOOKING) {
			if(resultCode == Activity.RESULT_OK) {
				ItemManager.getInstance().sync();
			}
		}
	}
	
	private static class ViewHolder {
		public TextView text;
//	    public ImageView image;
	}
	
	private static class ItemAdapter extends BaseAdapter {
		
		private static final int TYPE_AVAILABLE = 0;
		private static final int TYPE_RESERVED = 1;
		private static final int TYPE_COUNT = 2;
		
		private Fragment mFragment;
		private List<ItemDetail> mData;
		
		public ItemAdapter(Fragment fragment) {
			mFragment = fragment;
			mData = new ArrayList<ItemDetail>();
		}
		
		public void setData(List<ItemDetail> data) {
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
			ItemDetail d = mData.get(position);
			boolean isAvailable = d.reservations.size() == 0;
			return isAvailable ? TYPE_AVAILABLE : TYPE_RESERVED;
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_COUNT;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			final ItemDetail d = mData.get(position);
			int viewType = getItemViewType(position);
			if(convertView == null) {
				LayoutInflater inflater = (LayoutInflater) mFragment.getActivity()
				        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				if(viewType == TYPE_AVAILABLE)
					v = inflater.inflate(R.layout.store_list_item_available, parent, false);
				else
					v = inflater.inflate(R.layout.store_list_item, parent, false);
			} else
				v = convertView;
			
			TextView txtItemName = (TextView) v.findViewById(R.id.text_item_name);
			TextView txtCustomerName = (TextView) v.findViewById(R.id.text_customer_name);
			TextView txtCustomerPhone = (TextView) v.findViewById(R.id.text_customer_phone);
			
			
			txtItemName.setText(d.item.name);
			if(d.reservations.size() > 0) {
				txtCustomerName.setText(d.reservations.get(0).customer.name);
				View btnCall = v.findViewById(R.id.button_call);
				btnCall.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						String phone = d.reservations.get(0).customer.phone;
						if(phone != null && !phone.isEmpty()) {
							Intent intent = new Intent(Intent.ACTION_DIAL);          
							intent.setData(Uri.parse("tel:"+phone));          
							mFragment.startActivity(intent); 
						}
					}
				});
			} else {	
				View btnReserve = v.findViewById(R.id.button_reserve);
				btnReserve.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Intent intent = new Intent();
						intent.setClass(mFragment.getActivity(), BookingActivity.class);
						intent.putExtra(BWSlippa.EXTRA_ITEM_KEY, d.item.key);
						intent.putExtra(BWSlippa.EXTRA_ITEM_NAME, d.item.name);
						intent.putExtra(BWSlippa.EXTRA_START_DATE, ItemManager.getInstance().getDate());
						mFragment.startActivityForResult(intent, REQUEST_BOOKING);	
					}
				});
			}
			return v;
		}
		
	}
	
	@Override
	public void onSyncStarted() {
		mList.startRefresh();
	}

	@Override
	public void onItemDataChanged(List<ItemDetail> items) {
		mAdapter.setData(items);
		mAdapter.notifyDataSetChanged();
		
		mList.setRefreshComplete();
	}

	@Override
	public void onRefresh() {
		ItemManager.getInstance().sync();
	}
	
}
