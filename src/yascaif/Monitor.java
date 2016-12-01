package yascaif;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.MonitorEvent;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cosylab.epics.caj.CAJChannel;

public class Monitor implements AutoCloseable {
	private static Logger L = Logger.getLogger(Monitor.class.getName());

	public interface MonitorListener extends EventListener
	{
		void Monitor(PValue data);
	}
	
	final CAJChannel chan;
	MListen delegate;
	PValue last_update;
	
	final List<MonitorListener> listeners  = new ArrayList<>();

	Monitor(CAJChannel ch)
	{
		chan = ch;
		try {
			delegate = new MListen();

			chan.addConnectionListenerAndFireIfConnected(delegate);
		} catch (Exception e) {
			throw new RuntimeException("Failed to listen for "+chan.getName(), e);
		}
	}
	
	@Override
	public void close() throws Exception {
		MListen d;
		synchronized (this) {
			d = delegate;
			delegate = null;
		}
		if(d!=null) {
			try {
				chan.removeConnectionListener(d);
				d.clear();
			} catch (Exception e) {
				throw new RuntimeException("Failed to unlisten for "+chan.getName(), e);
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
	};

	public synchronized void addMonitorListener(MonitorListener l) {
		listeners.add(l);
	}

	public synchronized void removeMonitorListener(MonitorListener l) {
		listeners.remove(l);
	}

	private void notifyEvent(PValue evt) {
		final List<MonitorListener> temp;
		synchronized (this) {
			temp  = new ArrayList<>(listeners);
		}
		for(MonitorListener l : temp) {
			try {
				l.Monitor(evt);
			} catch(Exception e) {
				L.log(Level.WARNING, "Unhandled error from Monitor callback", e);
			}
		}
	}
	
	private class MListen implements ConnectionListener,
									 gov.aps.jca.event.MonitorListener {
		gov.aps.jca.Monitor mon;

		void clear() {
			gov.aps.jca.Monitor m;
			synchronized (this) {
				m = mon;
				mon = null;
			}
			if(m!=null) {
				try {
					m.clear();
					if(last_update!=null)
						notifyEvent(PValue.makeDisconnect(this, last_update));
				} catch (Exception e) {
					Monitor.L.log(Level.WARNING, "Failed to clear subscription for "+chan.getName(), e);
				}
			}
		}
		
		@Override
		public void connectionChanged(ConnectionEvent ev) {
			Monitor.L.fine("Connection state changed "+chan.getName()+" "+Boolean.toString(ev.isConnected()));
			if(ev.isConnected()) {
				DBRType dt = CA.promotemap.get(chan.getFieldType());
				if(dt==null) {
					Monitor.L.warning("Channel "+chan.getName()+" has unsupported DBR ");
					if(last_update!=null)
						notifyEvent(PValue.makeDisconnect(this, last_update));
				}

				try {
					Monitor.L.fine("Subscribe to "+chan.getName());
					mon = chan.addMonitor(dt, 0,
							gov.aps.jca.Monitor.VALUE|gov.aps.jca.Monitor.ALARM, 
							this);
					chan.getContext().flushIO();
				} catch (Exception e) {
					Monitor.L.log(Level.WARNING, "Failed to create subscription for "+chan.getName(), e);
				}
			} else if(mon!=null) {
				clear();
			}
			
		}
	
		@Override
		public void monitorChanged(MonitorEvent ev) {
			Monitor.L.fine("Monitor event for "+chan.getName());
			try {
				PValue pev = new PValue(this, ev.getDBR());
				last_update = pev;
				notifyEvent(pev);
			} catch (Exception e) {
				Monitor.L.log(Level.WARNING, "Failed to translate/notify for "+chan.getName(), e);
			}
		}
		
	}

}
