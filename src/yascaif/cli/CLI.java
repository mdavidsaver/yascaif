package yascaif.cli;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import yascaif.CA;

public class CLI {
	private static final Logger L = Logger.getLogger("");

	private static String command;
	private static List<String> PVs = new LinkedList<>();
	private static double timeout = 2.0;
	
	private static void usage()
	{
		System.out.printf("Usage: <prog> [-v] [-t timeout] [get|put|info] <PV names...>");
		System.exit(1);
	}

	private static void procArgs(String[] args) {
		boolean settimo = false;
		for(String arg : args)
		{
			if(settimo) {
				settimo = false;
				timeout = Double.parseDouble(arg);
				continue;
				
			} else if(arg.startsWith("--")) {
				arg = arg.substring(2);
				
				if(arg=="help")
					usage();

			} else if(arg.startsWith("-") && arg.length()==2) {
				char O = arg.charAt(1);
				if(O=='v') {
					L.setLevel(Level.ALL);
					continue;
					
				} else if(O=='h') {
					usage();
					
				} else if(O=='t') {
					settimo = true;
				}

			} else if(command==null) {
				command = arg;
				continue;

			} else {
				PVs.add(arg);
				continue;
			}
			
			System.err.printf("Invalid argument '%s'%n", arg);
			System.exit(1);
		}

		if(command==null)
			usage();
	}
	
	/** Command Line test program for CA
	 */
	public static void main(String[] args) {
		L.setLevel(Level.SEVERE);
		procArgs(args);

		Command cmd = null;
		if(command.equals("get")) {
			cmd = new Get();
		} else {
			System.err.printf("Unknown command '%s'%n", command);
			System.exit(1);
		}
		
		if(PVs.size()==0) {
			System.err.println("Empty PV list");
			System.exit(1);
		}
		
		try(CA ca = new CA()) {
			ca.setTimeout(timeout);

			ca.connect(PVs.toArray(new String[0]));
			
			cmd.process(ca, PVs);
		}
	}

}