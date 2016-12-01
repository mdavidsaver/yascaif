package yascaif.demo;

import java.util.ArrayList;
import java.util.List;

/** Check how much introspection matlab does.
 *
 */
public class Dynamic {
	public abstract class Base {
		public abstract double getTime();
	}
	
	public class DDouble extends Base {
		public double getTime() { return 5.2; }
		public double getValue() { return 4.2; }
	}
	
	public class DString extends Base {
		public double getTime() { return 5.2; }
		public String getValue() { return "hello"; }
	}

	public Base make(String t) {
		if(t.equals("num")) {
			return new DDouble();
		} else if(t.equals("str")) {
			return new DString();
		} else {
			throw new RuntimeException("Unknown type");
		}
	}
	
	public Object string() { return "Test"; }
	public Object real() { return new Double(4.2); }
	public Object integer() { return new Integer(42); }
	public Object realarr() { 
		List<Double> ret = new ArrayList<Double>();
		ret.add(1.1);
		ret.add(2.2);
		return ret;
	}
	public Object realarr2() { 
		return new int[]{1};
	}
	
	public Object scalar1() { return 1; }
	public Object scalar2() { return new int[]{1};}
	
	public String classify(Object inp)
	{
		Class<? extends Object> klass = inp.getClass();
		
		StringBuilder B = new StringBuilder();
		B.append(klass.getName());
		
		if(klass.isPrimitive())
			B.append(" is primative");

		if(klass.isArray()) {
			B.append(" is array of "+klass.getComponentType().getName());
		}

		if(inp instanceof String)
			B.append(" is string");
		
		if(inp instanceof double[])
			B.append(" is double[]");

		if(inp instanceof Double)
			B.append(" is double");

		return B.toString();
	}
}
