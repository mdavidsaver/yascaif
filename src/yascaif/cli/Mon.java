package yascaif.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import yascaif.CA;
import yascaif.Monitor;
import yascaif.Monitor.MonitorListener;
import yascaif.PValue;

public class Mon implements Command {
	private static final Logger L = Logger.getLogger("Monitor");
	
	private class MonPrint implements MonitorListener {
		final String name;
		MonPrint(String n) { name=n; }

		@Override
		public void Monitor(PValue data) {
			System.out.println(name+": "+data.toString());
		}
		
	}

	@Override
	public void process(CA ca, List<String> PVs) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				synchronized(Mon.this) {
					Mon.this.notify();
				}
			}
		});

		List<Monitor> mons = new ArrayList<>();

		for(String pv : PVs) {
			L.info("Subscribe to "+pv);
			Monitor M = ca.monitor(pv);
			M.addMonitorListener(new MonPrint(pv));
			mons.add(M);
		}
		
		synchronized (this) {
			try {
				wait();
			} catch (InterruptedException e) {
				// continue
			}
		}

		for(Monitor mon : mons) {
			mon.close();
		}

		L.info("Done");
	}

}
