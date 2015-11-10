package test;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import yascaif.CA;

public class test3 {

	public static void main(String[] args) {
		System.out.println("Starting");
		CA e = new CA();
		e.setVerbose(false);

		DBR[] data = e.getDBRs(args, DBRType.STRING, 1);

		for(int i=0; i<args.length; i++) {
			System.out.print(args[i]+" : ");
			if(data[i]==null) {
				System.out.println("<Timeout>");				
			} else {
				String[] ret = (String[])data[i].getValue();
				System.out.println(ret[0]);
			}
		}

		System.out.println("Done");
		
		e.close();
	}

}
