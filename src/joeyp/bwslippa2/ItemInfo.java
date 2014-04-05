package joeyp.bwslippa2;

import org.json.JSONException;
import org.json.JSONObject;

public class ItemInfo extends DBInfo {
	public String name;
	
	private ItemInfo() {
		
	}
	
	private ItemInfo(JSONObject obj) {
		unmarshal(obj);
	}

	@Override
	protected void unmarshal(JSONObject obj) {
		super.unmarshal(obj);
		try {
			name = obj.getString("name");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static ItemInfo parse(JSONObject obj) {
		return new ItemInfo(obj);
	}
}
