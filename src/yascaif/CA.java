package yascaif;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.Channel.ConnectionState;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.cosylab.epics.caj.CAJChannel;

/** Wrapper around JCA/CAJ meant to be used
 *  in an interactive shell-like environment.
 * 
 *  Each instance of this class maintains an independent Context
 *  (set of connected PVs).
 *  
 *  All blocking operations respect setTimeout(), which defaults
 *  to 2 seconds.
 */
public class CA implements AutoCloseable {
	private static Logger L = Logger.getLogger(CA.class.getName());

	private Context ctxt;
	private long timeout = 2000;

	private static final String cajname = "com.cosylab.epics.caj.CAJContext";

	public CA()
	{
		/* CAJ makes us decide whether to look for configuration in
		 * the OS environment, or Java properties...
		 * If any CA related environment variable are set, then assume
		 * the environment is fully configured, otherwise use properties.
		 */
		boolean useenv = false;
		for(String name : System.getenv().keySet())
		{
			if(name.startsWith("EPICS_CA_")) {
				useenv = true;
				L.fine(String.format(" %s=%s", name, System.getenv(name)));
			}
		}
		if(useenv) {
			System.setProperty("jca.use_env", "true");
			L.info("Using CA settings from process environment");
		} else {
			L.info("Using CA settings from Java properties");
			if(System.getProperty(cajname+".max_array_bytes")==null) {
				L.info("Setting default max_array_bytes");
				System.setProperty(cajname+".max_array_bytes", "33554532");
			}
		}
		JCALibrary jca = JCALibrary.getInstance();
		try {
			ctxt = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
		} catch (CAException e) {
			throw new RuntimeException("Failed to create JCA/CAJ context", e);
		}
	}

	/** Set operation timeout for subsequent get*() and put*() */
	public double setTimeout(double v)
	{
		long newto = (long)(v*1000);
		if(newto<=0)
			throw new RuntimeException("Timeout out or range, must be >= 0.0");
		double ret;
		synchronized (this) {
			ret = timeout;
			timeout = newto;
		}
		return ret;
	}

	public static void setVerbose(boolean b)
	{
		if(b)
			L.setLevel(Level.ALL);
		else
			L.setLevel(Level.SEVERE);
	}

	// Get only value

	public double getDouble(String name)
	{
		double[] ret = getDouble(name, 1);
		assert ret.length==1;
		return ret[0];
	}

	public double[] getDouble(String name, int count)
	{
		DBR data = getDBR(name, DBRType.STS_DOUBLE, count);
		STS sts = (STS)data;
		if(sts.getSeverity()==Severity.INVALID_ALARM)
			throw new RuntimeException("Data is INVALID");
		return (double[])data.getValue();
	}

	// get value and metadata (alarm and timestamp)

	public DoubleWrapper getDoubleM(String name)
	{
		return getDoubleM(name, 1);
	}

	public DoubleWrapper getDoubleM(String name, int count)
	{
		DBR data = getDBR(name, DBRType.TIME_DOUBLE, count);
		STS sts = (STS)data;
		if(sts.getSeverity()==Severity.INVALID_ALARM)
			throw new RuntimeException("Data is INVALID");
		return new DoubleWrapper(data);
	}

	// put value

	public void putDouble(String name, double val)
	{
		putDouble(name, val, false);
	}

	public void putDouble(String name, double val, boolean wait)
	{
		double[] arr = new double[]{val};
		putDouble(name, arr, wait);
	}

	public void putDouble(String name, double[] val)
	{
		putDouble(name, val, false);
	}

	public void putDouble(String name, double[] val, boolean wait)
	{
		putDBR(name, DBRType.DOUBLE, val.length, val, wait);
	}

	public void putString(String name, String val)
	{
		putString(name, val, false);
	}

	public void putString(String name, String val, boolean wait)
	{
		String[] arr = new String[]{val};
		putDBR(name, DBRType.STRING, 1, arr, wait);
	}

	// Methods for full cleanup

	/** Disconnect all channels and invalidate this object
	 */
	@Override
	public void close()
	{
		ctxt.dispose();
		ctxt = null;
	}

	/** Alias for close()
	 */
	public void destroy()
	{
		ctxt.dispose();
	}

	// Some helper methods (not required)

	/** Explicitly disconnect a single PV.
	 *  Use destroy()/close() to cleanup all channels (and invalidate this object).
	 */
	public void disconnect(String name)
	{
		String[] names = new String[]{name};
		disconnect(names);
	}

	/** Explicitly disconnect a set of PVs without invalidating this object.
	 *  Use destroy()/close() to cleanup all channels (and invalidate this object).
	 */
	public void disconnect(String[] names)
	{
		for(String name : names) {
			try {
				Channel ch = ctxt.createChannel(name);
				ch.destroy();
			}catch(Exception e){
				L.log(Level.SEVERE, "Error to disconnect PV", e);
			}
		}
	}

	public void connect(String name)
	{
		String[] names = new String[]{name};
		connect(names);
	}

	/** Explicitly begin connecting a set of PVs.
	 *
	 *  Optimization if the set of required PVs is known ahead of time.
	 *
	 *  @note Not required.
	 */
	public void connect(String[] names)
	{
		for(String name : names) {
			try {
				ctxt.createChannel(name);
			}catch(Exception e){
				L.log(Level.SEVERE, "Error to disconnect PV", e);
			}
		}
	}

	// Generic versions of get*() and put*()

	/** Fetch the value of a PV
	 * 
	 * @param name PV name
	 * @param dtype Requested DBRType or null to get the native type
	 * @param count Requested # of elements or -1 to get the max
	 * @return Returned data
	 * @throws RuntimeError on timeout or server error
	 */
	public DBR getDBR(String name, DBRType dtype, int count)
	{
		try {
			CAJChannel ch = (CAJChannel)ctxt.createChannel(name);
			Getter get = new Getter(ch, dtype, count);

			long now = System.currentTimeMillis(),
					end = now+timeout;
			synchronized (get) {
				while(!get.done && now<end) {
					get.wait(end-now);
					now = System.currentTimeMillis();
				}
			}

			if(!get.done)
				throw new RuntimeException("timeout");
			else if(get.bad!=null)
				throw get.bad;
			else if(!get.status.isSuccessful())
				throw new RuntimeException("CA Error "+get.status.toString());

			assert get.data!=null;
			return get.data;

		}catch(Exception e){
			throw new RuntimeException("Failed to get PV", e);
		}
	}

	/** Fetch the value of several PVs
	 * 
	 * @param names PV names
	 * @param dtype Requested DBRType for all PVs or null to get the native type of each PV
	 * @param count Requested # of elements for all PVs or -1 to get the max of each PV
	 * @return Returned data array, entries may be null if the PV is not connected
	 * @throws RuntimeError on timeout or server error
	 */
	public DBR[] getDBRs(String[] names, DBRType dtype, int count)
	{
		CAJChannel[] chans = new CAJChannel[names.length];
		Getter[] getters = new Getter[names.length];
		DBR[] ret = new DBR[names.length];
		try {
			for(int i=0; i<names.length; i++) {
				chans[i] = (CAJChannel)ctxt.createChannel(names[i]);
				getters[i] = new Getter(chans[i], dtype, count);
			}

			long now = System.currentTimeMillis(),
					end = now+timeout;

			for(int i=0; i<names.length && now<end; i++) {
				synchronized (getters[i]) {
					while(!getters[i].done && now<end) {
						getters[i].wait(end-now);
						now = System.currentTimeMillis();
					}
				}
			}

			for(int i=0; i<names.length; i++) {
				if(getters[i].bad!=null)
					throw getters[i].bad;
				ret[i] = getters[i].data;
				//TODO, something with .status
			}

			return ret;
		}catch(Exception e){
			throw new RuntimeException("Failed to get PV", e);
		}
	}


	/** Put value to PV
	 * 
	 * @param name PV name
	 * @param dtype
	 * @param count
	 * @param val
	 * @param wait If true block until put is acknowledged by the server,
	 *             false return as soon as put request is sent.
	 */
	public void putDBR(String name, DBRType dtype, int count, Object val, boolean wait)
	{
		try {
			CAJChannel ch = (CAJChannel)ctxt.createChannel(name);
			Putter putter = new Putter(ch, dtype, count, val, wait);

			long now = System.currentTimeMillis(),
					end = now+timeout;

			synchronized (putter) {
				while(!putter.done && now<end) {
					putter.wait(end-now);
					now = System.currentTimeMillis();
				}
			}

			if(!putter.done)
				throw new RuntimeException("timeout");
			else if(putter.bad!=null)
				throw putter.bad;
			else if(!putter.status.isSuccessful())
				throw new RuntimeException("CA error : "+putter.status.toString());

		}catch(Exception e){
			throw new RuntimeException("Failed to put PV", e);
		}
	}

	// Inner classes for callbacks

	private abstract class OnConn implements ConnectionListener
	{
		protected CAJChannel chan;
		OnConn(CAJChannel ch) {
			chan = ch;
		}

		protected void doConnect()
		{
			synchronized (chan) {
				if(chan.getConnectionState()!=ConnectionState.CONNECTED) {
					try {
						chan.addConnectionListener(this);
					} catch (Exception e) {
						throw new RuntimeException("error adding listener", e);
					}
				} else {
					onConnect();
				}
			}
		}

		@Override
		public void connectionChanged(ConnectionEvent ev) {
			try {
				chan.removeConnectionListener(this);
			} catch (Exception e) {
				L.log(Level.SEVERE, "Error in connectionChanged removeConnectionListener", e);
			}
			L.info(String.format("connectionChanged '%s' %sconnected",
					chan.getName(), ev.isConnected()?"":"dis"));
			if(!ev.isConnected())
				return;
			synchronized (chan) {
				onConnect();
			}
		}

		protected abstract void onConnect();
	}

	private class Getter extends OnConn implements GetListener
	{
		private DBRType dtype;
		private int dcount;

		public CAStatus status;
		public DBR data;
		public Exception bad;
		public boolean done = false;

		public Getter(CAJChannel chan, DBRType t, int c) {
			super(chan);
			dtype=t;
			dcount=c;
			doConnect();
		}

		@Override
		public void onConnect() {
			if(dtype==null)
				dtype = chan.getFieldType();
			if(dcount<0)
				dcount = 0; // CAJ supports dynamic array size

			try {
				L.info(String.format("get request '%s' for %d", chan.getName(), dcount));
				chan.get(dtype, dcount, this);
				chan.getContext().flushIO();

			} catch (CAStatusException e) {
				synchronized (this) {
					done = true;
					status = e.getStatus();
					assert !status.isSuccessful();
					notify();
				}
			} catch (Exception e) {
				synchronized (this) {
					bad = e;
					done = true;
					notify();
				}
			}
		}

		@Override
		public void getCompleted(GetEvent ev) {
			L.info(String.format("getComplete '%s'",chan.getName()));
			synchronized (this) {
				status = ev.getStatus();
				data = ev.getDBR();
				done = true;

				notify();
			}
		}
	}

	public class DoubleWrapper {
		DBR data;
		private DoubleWrapper(DBR d) {
			data = d;
		}
		public double[] value() {
			return (double[])data.getValue();
		}
		public int severity() {
			Severity sevr = ((TIME)data).getSeverity();
			if(sevr==Severity.NO_ALARM)
				return 0;
			else if(sevr==Severity.MINOR_ALARM)
				return 1;
			else if(sevr==Severity.MAJOR_ALARM)
				return 2;
			else if(sevr==Severity.INVALID_ALARM)
				return 3;
			else
				return 3;
		}
		public double time() {
			TimeStamp ts = ((TIME)data).getTimeStamp();
			double sec = ts.secPastEpoch();
			return sec+1e-9*ts.nsec();
		}
	}

	private class Putter extends OnConn implements PutListener
	{
		private DBRType dtype;
		private int count;
		private Object val;
		private boolean wait;

		public boolean done = false;
		public Exception bad;
		public CAStatus status;

		public Putter(CAJChannel ch, DBRType d, int c, Object v, boolean w)
		{
			super(ch);
			dtype = d;
			count = c;
			val = v;
			wait = w;
			doConnect();
		}

		@Override
		public void onConnect() {
			try {
				if(wait) {
					chan.put(dtype, count, val, this);
				} else {
					chan.put(dtype, count, val);
				}
				chan.getContext().flushIO();
				if(!wait) {
					synchronized (this) {
						status = CAStatus.NORMAL;
						done = true;
						notify();
					}
				}
			} catch (CAStatusException e) {
				synchronized (this) {
					done = true;
					status = e.getStatus();
					assert !status.isSuccessful();
					notify();
				}
			} catch (Exception e) {
				synchronized (this) {
					bad = e;
					done = true;
					notify();
				}
			}

		}

		@Override
		public synchronized void putCompleted(PutEvent ev) {
			L.info(String.format("putCompleted '%s'",chan.getName()));
			status = ev.getStatus();
			done = true;
			notify();
		}
	}
}
