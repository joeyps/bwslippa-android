package joeyp.bwslippa;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReservedInfo extends DBInfo {
	ItemInfo item;
	CustomerInfo customer;

	private ReservedInfo() {
		
	}
	
	private ReservedInfo(JSONObject obj) {
		unmarshal(obj);
	}
	
	@Override
	protected void unmarshal(JSONObject obj) {
		super.unmarshal(obj);
		try {
			item = ItemInfo.parse(obj.getJSONObject("item"));
			JSONArray jAry = obj.getJSONArray("records");
			if(jAry.length() > 0) {
				JSONObject record = jAry.getJSONObject(0);
				customer = CustomerInfo.parse(record.getJSONObject("customer"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static ReservedInfo parse(JSONObject obj) {
		return new ReservedInfo(obj);
	}
}
