package joeyp.bwslippa;

import java.util.ArrayList;
import java.util.List;

public class ItemDetail {
	public ItemInfo item;
	public List<ReservedInfo> reservations;
	
	public ItemDetail() {
		reservations = new ArrayList<ReservedInfo>();
	}
}
