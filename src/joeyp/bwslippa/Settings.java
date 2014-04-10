package joeyp.bwslippa;

import java.text.Format;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.text.TextUtils;

public class Settings {
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
	
	public static Format getSettingsDateFormat(Context context, boolean excludeYear) {
		String pattern = android.provider.Settings.System.getString(context.getContentResolver(), android.provider.Settings.System.DATE_FORMAT);
		Format format = null;
		if (TextUtils.isEmpty(pattern)) {
			format = android.text.format.DateFormat.getMediumDateFormat(context);
		} else {
			if(excludeYear)
				pattern = pattern.replaceFirst("[,/ ]*y+[/]?", "");
			format = new SimpleDateFormat(pattern);
		}
		return format;
	}
}
