package yascaif.cli;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import yascaif.CA;

public class Get implements Command {
	private static final Logger L = Logger.getLogger("Get");

	@Override
	public void process(CA ca, List<String> PVs) {
		String[] names = PVs.toArray(new String[0]);

		DBR[] results = ca.getDBRs(names, null, -1);

		for(int i=0; i<names.length; i++)
		{
			System.out.printf("%s : ", names[i]);
			if(results[i]==null) {
				System.out.println("<Timeout>");
				
			} else {
				try {
					if(results[i].getCount()==1 || results[i].isSTRING()) {
						DBR x = results[i].convert(DBRType.STRING);
						String[] arr = (String[])x.getValue();
						System.out.println(arr[0]);
						
					} else if(results[i].isDOUBLE() || results[i].isFLOAT()) {
						DBR x = results[i].convert(DBRType.DOUBLE);
						double[] arr = (double[])x.getValue();
						System.out.printf("(%d) [", arr.length);
						for(double v : arr) {
							System.out.printf("%f, ", v);
						}
						System.out.println("]");

					} else {
						DBR x = results[i].convert(DBRType.INT);
						int[] arr = (int[])x.getValue();
						System.out.printf("(%d) [", arr.length);
						for(int v : arr) {
							System.out.printf("%d, ", v);
						}
						System.out.println("]");
						
					}
				}catch(Exception e) {
					System.out.println("<Error>");
					L.log(Level.SEVERE, "oops", e);
				}
			}
		}
	}

}
