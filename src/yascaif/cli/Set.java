package yascaif.cli;

import java.util.List;

import yascaif.CA;

public class Set implements Command {

	@Override
	public void process(CA ca, List<String> PVs) {
		if(PVs.size()%2!=0) {
			System.out.println("Must provide value for each pvs ('<name1> <value1> <name2> <value2> ...");
			System.exit(1);
		}
		for(int i=0; i<PVs.size(); i+=2) {
			ca.putString(PVs.get(i), PVs.get(i+1));
		}
	}

}
