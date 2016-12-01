package yascaif.wrapper;

import gov.aps.jca.CAStatusException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;

import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public abstract class XWrapper extends EventObject {

	static private final Map<Severity, Integer> sevlut = new HashMap<>();
	static {
		sevlut.put(Severity.NO_ALARM, 0);
		sevlut.put(Severity.MINOR_ALARM, 1);
		sevlut.put(Severity.MAJOR_ALARM, 2);
		sevlut.put(Severity.INVALID_ALARM, 3);
	}

	private static final long serialVersionUID = 1L;
	
	protected DBR data;
	
	public XWrapper(Object src, DBR d) {
		super(src);
		data = d;
		assert d.isTIME();
	}

	public int severity() {
		Integer I=3;
		if(data!=null) {
			Severity sevr = ((TIME)data).getSeverity();
			I = sevlut.get(sevr);
		}
		return I==null ? 3 : I;
	}
	public double time() {
		double ret = 0;
		if(data!=null) {
			TimeStamp ts = ((TIME)data).getTimeStamp();
			double sec = ts.secPastEpoch()+631152000;
			ret = sec+1e-9*ts.nsec();
		}
		return ret;
	}
	@Override
	public String toString()
	{
		StringBuilder build = new StringBuilder("Date: ");
		Date ts = new Date((long)(time()*1000));
		build.append(ts.toString());
		build.append("\nAlarm: ");
		build.append(severity());
		build.append("\nValue: ");
		if(data==null) {
			build.append("<no data>");
		} else {
			try {
				DBR sdata = data.convert(DBRType.STRING);
				String[] arr = (String[])sdata.getValue();
				if(arr.length==1) {
					build.append(arr[0]);
				} else {
					build.append(String.format("(%d) [", arr.length));
					for(String e: arr) {
						build.append(e);
						build.append(", ");
					}
					build.append("]\n");
				}
			} catch (CAStatusException e) {
				build.append("<Error>\n");
			}
		}
		return build.toString();
	}
}
