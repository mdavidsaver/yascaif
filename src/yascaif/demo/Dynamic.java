package yascaif.demo;

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
	public Object integer() { return new Integer(42); }
}
