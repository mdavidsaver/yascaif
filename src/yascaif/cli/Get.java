package yascaif.cli;

import java.util.List;
import java.util.logging.Logger;

import yascaif.CA;
import yascaif.PValue;

public class Get implements Command {
	@SuppressWarnings("unused")
	private static final Logger L = Logger.getLogger("Get");

	@Override
	public void process(CA ca, List<String> PVs) {
		String[] names = PVs.toArray(new String[0]);

		PValue[] results = ca.readManyM(names);

		for(int i=0; i<names.length; i++)
		{
			System.out.printf("%s : ", names[i]);
			if(results[i]==null) {
				System.out.println("<Timeout>");
				
			} else {
				System.out.println(results[i]);
			}
		}
	}

}
