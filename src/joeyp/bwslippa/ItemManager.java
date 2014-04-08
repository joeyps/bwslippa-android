package joeyp.bwslippa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import joeyp.bwslippa.RPCHelper.RPCCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ItemManager {
	
	private static ItemManager sItemManager;
	private Date mDate;
	private List<TagInfo> mTags;
	private List<ItemDetail> mItems;
	private List<ItemDetail> mFilteredItems;
	private Handler mHandler;
	
	private ItemFilter mFilter;
	
	private Set<OnItemDataChangedListener> mListeners;
	
	public interface OnItemDataChangedListener {
		public void onSyncStarted();
		public void onItemDataChanged(List<ItemDetail> items);
	}
	
	private ItemManager() {
		mHandler = new Handler(Looper.getMainLooper());
		mTags = new ArrayList<TagInfo>();
		mItems = new ArrayList<ItemDetail>();
		mFilteredItems = new ArrayList<ItemDetail>();
		mListeners = new HashSet<OnItemDataChangedListener>();
		Calendar cal = Calendar.getInstance();
		mDate = cal.getTime();
		
		mFilter = new ItemFilter(ItemFilter.TAG_RESERVED, null);
	}
	
	public static ItemManager getInstance() {
		if(sItemManager == null) {
			sItemManager = new ItemManager();
		}
		return sItemManager;
	}
	
	public void registerListener(final OnItemDataChangedListener listener) {
		mListeners.add(listener);
		final List<ItemDetail> items = new ArrayList<ItemDetail>(mItems);
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				listener.onItemDataChanged(items);				
			}
			
		});
	}
	
	public void setFilter(ItemFilter filter) {
		mFilter = filter;
		
		mFilteredItems.clear();
		if(mFilter.type == ItemFilter.TAG_ALL)
			mFilteredItems.addAll(new ArrayList<ItemDetail>(mItems));
		else if(mFilter.type == ItemFilter.TAG_AVAILABLE)
			mFilteredItems.addAll(getAvailableItems());
		else if(mFilter.type == ItemFilter.TAG_RESERVED)
			mFilteredItems.addAll(getReservedItems());
		
		notifyAllListeners();
	}
	
	private List<ItemDetail> getReservedItems() {
		List<ItemDetail> data = new ArrayList<ItemDetail>();
		for(ItemDetail d : mItems) {
			if(d.reservations.size() > 0)
				data.add(d);
		}
		return data;
	}
	
	private List<ItemDetail> getAvailableItems() {
		List<ItemDetail> data = new ArrayList<ItemDetail>();
		for(ItemDetail d : mItems) {
			if(d.reservations.size() == 0)
				data.add(d);
		}
		return data;
	}
	
	public void setDate(Date date) {
		mDate = date;
		onDateChanged();
	}
	
	public Date getDate() {
		return mDate;
	}
	
	public List<TagInfo> getTags() {
		List<TagInfo> tags = new ArrayList<TagInfo>(mTags);
		return tags;
	}
	
	private void init() {
		RPCHelper rpc = RPCHelper.getInstance();
        rpc.call(RPCHelper.API_BIND_WORKSPACE, new RPCCallback() {

			@Override
			public void onCallback(JSONObject jObj) {
				try {
					mTags.clear();
					mItems.clear();
					final List<ReservedInfo> data = new ArrayList<ReservedInfo>();
					JSONArray jAry = jObj.getJSONArray("result");
					for(int i = 0; i < jAry.length(); i++) {
						JSONObject obj = jAry.getJSONObject(i);
						TagInfo info = TagInfo.parse(obj.getJSONObject("tag"));
						JSONArray items = obj.getJSONArray("items");
						for (int j=0; j < items.length(); j++) {
							ItemInfo item = ItemInfo.parse(items.getJSONObject(j));
							ItemDetail detail = new ItemDetail();
							detail.item = item;
							mItems.add(detail);
						}
						mTags.add(info);
					}
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							query();
							notifyAllListeners();
						}
						
					});
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFail() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onCallback(JSONArray obj) {
				// TODO Auto-generated method stub
				
			}
        	
        });
	}
	
	public void forceSync() {
		init();
	}
	
	public void sync() {
		query();
	}
	
	private void onDateChanged() {
		query();
	}
	
	private void query() {
		RPCHelper rpc = RPCHelper.getInstance();
        rpc.call(RPCHelper.API_GET_RESERVED, new RPCCallback() {

			@Override
			public void onCallback(JSONObject jObj) {
				try {
					String date = jObj.getString("date");
					JSONArray jAry = jObj.getJSONArray("result");
					for(int i = 0; i < jAry.length(); i++) {
						ReservedInfo info = ReservedInfo.parse(jAry.getJSONObject(i));
						ItemInfo item = info.item;
						ItemDetail d = getItemDetail(item);
						if(d != null) {
							d.reservations.clear();
							d.reservations.add(info);
						}
					}
					
					mHandler.post(new Runnable() {

						@Override
						public void run() {
							//FIXME handle filtering data in worker thread
							setFilter(mFilter);
							notifyAllListeners();
						}
						
					});
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onFail() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onCallback(JSONArray obj) {
				// TODO Auto-generated method stub
				
			}
        	
        }, Settings.DATE_FORMAT.format(mDate));
	}
	
	private ItemDetail getItemDetail(ItemInfo item) {
		for(ItemDetail d : mItems) {
			if(d.item.key.equals(item.key)) {
				return d;
			}
		}
		return null;
	}
	
	private void notifyAllListeners() {
//		Iterator<OnItemDataChangedListener> iterator =  mListeners.iterator();
//		while(iterator.hasNext()) {
//			OnItemDataChangedListener l = iterator.next();
//			List<ItemInfo> items = new ArrayList<ItemInfo>(mItems);
//			l.onItemDataChanged(items);
//		}
		
		Set<OnItemDataChangedListener> listeners = new HashSet<ItemManager.OnItemDataChangedListener>(mListeners);
		for(OnItemDataChangedListener l : listeners) {
			List<ItemDetail> items = new ArrayList<ItemDetail>(mFilteredItems);
			l.onItemDataChanged(items);
		}
	}
}
