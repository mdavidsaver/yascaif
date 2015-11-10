package yascaif.cli;

import java.util.logging.Level;
import java.util.logging.Logger;

import yascaif.CA;
import yascaif.TimeMe;
import yascaif.CA.DoubleWrapper;

public class test1 {

	public static void main(String[] args) {
		
		// Default log level
		Logger L = Logger.getLogger("");
		L.setLevel(Level.SEVERE);
		//L.setLevel(Level.ALL);
		
		// Could also use this
		//ca.setVerbose(false);

		{
			// Log level for a specific source
			Logger t = Logger.getLogger("TimeMe");
			t.setLevel(Level.ALL);
		}

		try(CA ca = new CA()) {
			double val;
			try(TimeMe x = new TimeMe("Fetch 1")) {
				val = ca.getDouble("TST:N:Samp-SP");
			}
			System.out.printf("Value is %f%n", val);
			
			try(TimeMe x = new TimeMe("Fetch 2")) {
				val = ca.getDouble("TST:N:Samp-SP");
			}
			System.out.printf("Value is still %f%n", val);
			
			DoubleWrapper v = ca.getDoubleM("TST:N:Samp-SP");
			System.out.printf("Value: %f%nAlarm: %d%nTime: %f%n",
					v.value()[0], v.severity(), v.time());
			
		} catch(Exception e){
			L.log(Level.SEVERE, "error", e);
			System.exit(1);
		}
	}

}
