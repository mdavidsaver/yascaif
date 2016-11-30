package yascaif.wrapper;

import gov.aps.jca.dbr.DBR;

public class DoubleWrapper extends XWrapper {

	private static final long serialVersionUID = 2L;
	
	public DoubleWrapper(Object src, DBR d) {
		super(src, d);
		assert d.isDOUBLE();
	}
	public double[] value() {
		return (double[])data.getValue();
	}

}
