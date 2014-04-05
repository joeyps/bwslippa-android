package joeyp.bwslippa;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class DBInfo {
	
	protected String key;

	protected void unmarshal(JSONObject obj) {
		try {
			key = obj.getString("key");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
