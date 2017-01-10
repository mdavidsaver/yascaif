package yascaif.test;

import java.util.LinkedList;

import yascaif.CA;
import junit.framework.TestCase;

public class TestCoerceWritable extends TestCase {
	
	public void testInt() {
		int[] arr;

		arr = (int[])CA.coerceWritable(42);
		assertEquals(42, arr[0]);
		assertEquals(1, arr.length);
		
		arr = (int[])CA.coerceWritable(new Integer(42));
		assertEquals(42, arr[0]);
		assertEquals(1, arr.length);
		
		arr = (int[])CA.coerceWritable(new int[]{42});
		assertEquals(42, arr[0]);
		assertEquals(1, arr.length);
		
		arr = (int[])CA.coerceWritable(new Integer[]{42});
		assertEquals(42, arr[0]);
		assertEquals(1, arr.length);
	}
	
	public void testDouble() {
		double[] arr;

		arr = (double[])CA.coerceWritable(4.2);
		assertEquals(4.2, arr[0]);
		assertEquals(1, arr.length);
		
		arr = (double[])CA.coerceWritable(new Double(4.2));
		assertEquals(4.2, arr[0]);
		assertEquals(1, arr.length);
		
		arr = (double[])CA.coerceWritable(new double[]{4.2});
		assertEquals(4.2, arr[0]);
		assertEquals(1, arr.length);
		
		arr = (double[])CA.coerceWritable(new Double[]{4.2});
		assertEquals(4.2, arr[0]);
		assertEquals(1, arr.length);
	}
	
	public void testString() {
		String[] arr;

		arr = (String[])CA.coerceWritable("hello");
		assertEquals("hello", arr[0]);
		assertEquals(1, arr.length);
		
		arr = (String[])CA.coerceWritable(new String[]{"hello"});
		assertEquals("hello", arr[0]);
		assertEquals(1, arr.length);
	}

	public void testFail() {
		try {
			Object x = CA.coerceWritable(new LinkedList<Double>());
			fail("Unexpected success w/ "+x.getClass().getName()+" "+x.toString());
		} catch(RuntimeException e) {
			// success
		} catch(Throwable e) {
			fail("Failure w/ unexpected exception "+e.toString());
		}
	}
}
