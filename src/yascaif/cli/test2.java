package yascaif.cli;

import java.util.logging.Level;
import java.util.logging.Logger;

import yascaif.CA;
import yascaif.CA.DoubleWrapper;

public class test2 {

	public static void main(String[] args) {
		Logger L = Logger.getLogger("");

		try(CA ca = new CA()) {
			ca.setVerbose(false);

			DoubleWrapper val;
			
			val = ca.getDoubleM("TST:Raw0:Data-I", -1);
			System.out.printf("Before time %f count %d%n", val.time(), val.value().length);

			System.out.println("Set RAW mode");
			ca.putString("TST:Mode:Samp-Sel", "Raw");
			System.out.println("Software Trigger source (single acquisition)");
			ca.putString("TST:Src:Trg-Sel", "Soft");
			System.out.println("Trigger acquisition");
			ca.putDouble("TST:Mode:Trg-Sel", 0, true);

			System.out.println("Wait");
			Thread.sleep(1000); // ugly, will do better

			val = ca.getDoubleM("TST:Raw0:Data-I", -1);
			System.out.printf("After time %f count %d%n", val.time(), val.value().length);
		} catch(Exception e){
			L.log(Level.SEVERE, "error", e);
			System.exit(1);
		}
	}
}