package joeyp.bwslippa;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomerInfo extends DBInfo {
	
	public String name;
	public String phone;
	
	public CustomerInfo() {
		
	}
	
	private CustomerInfo(JSONObject obj) {
		unmarshal(obj);
	}

	@Override
	protected void unmarshal(JSONObject obj) {
		super.unmarshal(obj);
		try {
			name = obj.getString("name");
			phone = obj.getString("phone");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static CustomerInfo parse(JSONObject obj) {
		return new CustomerInfo(obj);
	}
}
