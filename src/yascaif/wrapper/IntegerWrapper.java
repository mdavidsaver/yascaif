package yascaif.wrapper;

import gov.aps.jca.dbr.DBR;

public class IntegerWrapper extends XWrapper {

	private static final long serialVersionUID = 3L;

	public IntegerWrapper(Object src, DBR d) {
		super(src, d);
		assert d.isINT();
	}
	public int[] value() {
		return (int[])data.getValue();
	}
}
