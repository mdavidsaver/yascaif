package yascaif;

import java.util.logging.Logger;

/** Helper for execution timing
 * 
 * try(TimeMe unused = new TimeMe("some msg")) {
 * 	 // code to be timed
 * }
 *
 * Logs to a Logger named "TimeMe".
 */
public class TimeMe implements AutoCloseable {
	private static Logger L = Logger.getLogger("TimeMe");

	private String name = "";
	private long start = System.currentTimeMillis();

	public TimeMe() {}
	public TimeMe(String name) {
		this.name = name; 
	}
	
	@Override
	public void close() {
		long end = System.currentTimeMillis();
		double rt = end-start;
		L.info(String.format("Runtime '%s' = %f ms", name, rt));
	}
}
