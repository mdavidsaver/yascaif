package test;

import yascaif.CA;
import yascaif.PValue;

public class test3 {

	public static void main(String[] args) {
		System.out.println("Starting");
		CA.setVerbose(false);
		CA e = new CA();

		PValue[] data = e.readManyM(args);

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
