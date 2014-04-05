package joeyp.bwslippa2;

import org.json.JSONException;
import org.json.JSONObject;

public class TagInfo extends DBInfo {

	public String name;
	
	private TagInfo() {
		
	}
	
	private TagInfo(JSONObject obj) {
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
	
	public static TagInfo parse(JSONObject obj) {
		return new TagInfo(obj);
	}
}
