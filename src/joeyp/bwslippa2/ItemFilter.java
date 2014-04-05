package joeyp.bwslippa2;

public class ItemFilter {
    	
	public static final int TAG_ALL = 1;
	public static final int TAG_RESERVED = 2;
	public static final int TAG_AVAILABLE = 3;
	public static final int TAG = 4;
	
	public int type;
	public String name;
	public TagInfo tag;
	
	public ItemFilter(int type, String name) {
		this.type = type;
		this.name = name;
	}
	
	public ItemFilter(int type, String name, TagInfo tag) {
		this.type = type;
		this.name = name;
		this.tag = tag;
	}
}
