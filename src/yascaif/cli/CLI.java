package yascaif.cli;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import yascaif.CA;
import yascaif.CA.Config;

public class CLI {
	private static final Logger L = Logger.getLogger("");

	private static final Map<String,Class<? extends Command>> commands = new HashMap<>();
	static {
		commands.put("get", Get.class);
		commands.put("put", Set.class);
		commands.put("putarr", SetArray.class);
		commands.put("monitor", Mon.class);
	}

	private static String command;
	private static List<String> PVs = new LinkedList<>();
	private static double timeout = 2.0;
	private static Config conf = new Config().useEnv(true);

	private static void usage()
	{
		StringBuilder cmb = new StringBuilder();
		Iterator<String> iter = commands.keySet().iterator();
		while(iter.hasNext()) {
			cmb.append(iter.next());
			if(iter.hasNext())
				cmb.append("|");
		}
		
		
		System.out.println("Usage: <prog> [-v] [-t timeout] ["+cmb.toString()+"] <PV names...>");
		System.exit(1);
	}

	private static void procArgs(String[] args) {
		boolean settimo = false, setmax = false, setname = false;
		for(String arg : args)
		{
			if(settimo) {
				settimo = false;
				timeout = Double.parseDouble(arg);
				continue;
				
			} else if(setmax) {
				setmax = false;
				conf.useEnv(false);
				conf.maxArrayBytes(Integer.parseInt(arg));
				continue;

			} else if(setname) {
				setname = false;
				conf.useEnv(false);
				conf.nameServers(arg);
				continue;

			} else if(arg.equals("--help")) {
				usage();
				System.exit(1);

			} else if(arg.equals("-h") || arg.equals("--max-array-bytes")) {
				setmax = true;
				continue;

			} else if(arg.equals("--name-servers")) {
				setname = true;
				continue;

			} else if(arg.equals("-v")) {
				L.setLevel(Level.FINEST);
				continue;

			} else if(arg.equals("-t")) {
				settimo = true;
				continue;

			} else if(arg.startsWith("-")) {
				System.err.printf("Invalid argument '%s'%n", arg);
				System.exit(1);

			} else if(command==null) {
				command = arg;
				continue;

			} else {
				PVs.add(arg);
				continue;
			}
		}

		if(settimo || setname || setmax) {
			System.err.println("Missing expected argument");
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

		Class<? extends Command> cls = commands.get(command);
		if(cls==null) {
			System.err.printf("Unknown command '%s'%n", command);
			System.exit(1);
		}

		Command cmd = null;
		try {
			cmd = cls.getConstructor().newInstance();
			assert cmd!=null;
		} catch (Exception e) {
			L.log(Level.SEVERE, "Failed to instanciate", e);
			System.exit(1);
		}

		if(PVs.size()==0) {
			System.err.println("Empty PV list");
			System.exit(1);
		}

		try(CA ca = new CA(conf)) {
			ca.setTimeout(timeout);

			cmd.process(ca, PVs);
		}
	}
}
