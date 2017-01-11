package test;
/* Run against a softIoc with the following .db

record(ao, "yascaif:flt") {}

record(longout, "yascaif:int") {}

record(waveform, "yascaif:fltarr") {
  field(FTVL, "DOUBLE")
  field(NELM, "10")
}

record(waveform, "yascaif:intarr") {
  field(FTVL, "LONG")
  field(NELM, "10")
}

record(waveform, "yascaif:bytearr") {
  field(FTVL, "CHAR")
  field(NELM, "10")
}

 */

import java.util.logging.Level;
import java.util.logging.Logger;

import yascaif.CA;
import yascaif.PValue;
import yascaif.TimeMe;

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
			
			ca.write("yascaif:flt", 4.2, true);
			double[] darr = (double[])ca.read("yascaif:flt");
			if(darr.length!=1 || Math.abs(darr[0]-4.2)>=1e-5) {
				System.err.printf("float readback mis-match %s%n", Double.toString(darr[0]));
			} else {
				System.out.println("Match flt");
			}

			ca.write("yascaif:int", 42, true);
			int[] iarr = (int[])ca.read("yascaif:int");
			if(iarr.length!=1 || iarr[0]!=42) {
				System.err.printf("int readback mis-match %s%n", Integer.toString(iarr[0]));
			} else {
				System.out.println("Match int");
			}

			ca.write("yascaif:fltarr", new double[]{1.1, 2.2});
			darr = (double[])ca.read("yascaif:fltarr");
			if(darr.length!=2 || Math.abs(darr[0]-1.1)>=1e-5 || Math.abs(darr[1]-2.2)>=1e-5) {
				System.err.printf("floatarr readback mis-match %s%n", Double.toString(darr[0]));
			} else {
				System.out.println("Match fltarr");
			}

			ca.write("yascaif:intarr", new int[]{1, 2}, true);
			iarr = (int[])ca.read("yascaif:intarr");
			if(iarr.length!=2 || iarr[0]!=1 || iarr[1]!=2) {
				System.err.printf("intarr readback mis-match %s%n", Integer.toString(iarr[0]));
			} else {
				System.out.println("Match intarr");
			}

			ca.write("yascaif:bytearr", new byte[]{1, 2}, true);
			byte[] barr = (byte[])ca.read("yascaif:bytearr");
			if(barr.length!=2 || barr[0]!=1 || barr[1]!=2) {
				System.err.printf("bytearr readback mis-match %s%n", Integer.toString(barr[0]));
			} else {
				System.out.println("Match bytearr");
			}
			
			System.out.println("End");
		} catch(Exception e){
			L.log(Level.SEVERE, "error", e);
			System.exit(1);
		}
	}

}
