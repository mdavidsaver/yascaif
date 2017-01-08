package yascaif.cli;

import java.util.List;

import yascaif.CA;

public class SetArray implements Command {

	@Override
	public void process(CA ca, List<String> PVs) {
		if(PVs.size()==0) {
			System.out.println("Must provide a PV name and zero or more element values");
			System.exit(1);
		}
		String name = PVs.remove(0);
		String arr[] = new String[PVs.size()];
		for(int i=0; i<arr.length; i++)
			arr[i] = PVs.get(i);
		ca.write(name, arr);
	}

}
