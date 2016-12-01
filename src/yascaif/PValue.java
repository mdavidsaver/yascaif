package yascaif;

import java.lang.reflect.Array;
import java.util.EventObject;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;

/** Holder for value, time stamp, and alarm severity
 */
public class PValue extends EventObject {
	private static final long serialVersionUID = -6262861871811251881L;

	Object value;
	int severity;
	TimeStamp time;

	public PValue(Object src, Object val, int sevr, TimeStamp ts) {
		super(src);
		value = val;
		severity = sevr;
		time = ts;
	}
	
	public PValue(Object src, DBR dbr)
	{
		super(src);
		if(dbr==null) {
			value = null;
			time = new TimeStamp(); // now
			severity = 3;
		} else if(dbr.isTIME()) {
			TIME tdbr = (TIME)dbr;
			value = dbr.getValue();
			time = tdbr.getTimeStamp();
			severity = tdbr.getSeverity().getValue();
		} else if(dbr.isSTS()) {
			STS tdbr = (STS)dbr;
			value = dbr.getValue();
			time = new TimeStamp(); // now
			severity = tdbr.getSeverity().getValue();
		} else {
			value = dbr.getValue();
			time = new TimeStamp(); // now
			severity = 0; // assume valid
		}
		if(value!=null && !value.getClass().isArray())
			throw new RuntimeException("PValue must be built around null or array, not "+value.getClass().getName());
	}
	
	public Object getValue() { return value; }
	public int getSevr() { return severity; }
	public double getTime() { return time.asDouble(); }
	public long[] getTimeInt() {
		return new long[]{time.secPastEpoch()+631152000, time.nsec()};
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(time.toMMDDYY());
		b.append(" ");
		int nelem = Array.getLength(value);
		if(nelem==1) {
			b.append(Array.get(value,0).toString());

			if(severity!=0) {
				b.append(" ");
				b.append(Severity.forValue(severity).getName());
			}

		} else {
			if(severity!=0) {
				b.append(Severity.forValue(severity).getName());
				b.append(" ");
			}

			b.append("[");
			for(int i=0; i<nelem; i++) {
				b.append(Array.get(value, i));
				if(i!=nelem-1)
					b.append(", ");
			}
			b.append("]");
		}
		return b.toString();
	}

	static PValue makeDisconnect(Object src, PValue prev)
	{
		TimeStamp dtime = new TimeStamp(); // now
		if(prev.time.GE(dtime)) {
			// prevent non-monotonic disconnect time, which archivers might ignore...
			dtime = TimeStamp.add(prev.time, 1e-9);
		}
		return new PValue(src, prev.value, 3, dtime);
	}
}
