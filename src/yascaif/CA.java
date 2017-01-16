/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * See LICENSE
 */
package yascaif;

import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.CAStatusException;
import gov.aps.jca.Context;
import gov.aps.jca.JCALibrary;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.PutEvent;
import gov.aps.jca.event.PutListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
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
	public static class Config {
		private boolean _useenv = false;
		private boolean _auto_addr_list = true;
		private String _addr_list;
		private String _name_servers;
		private long _max_bytes = 33554532;
		public Config() {}
		public Config useEnv(boolean v) { _useenv = v; return this; }
		public Config autoAddrList(boolean v) { _auto_addr_list = v; return this; }
		public Config addrList(String v) { _addr_list = v; return this; }
		public Config nameServers(String v) { _name_servers = v; return this; }
		public Config maxArrayBytes(long v) { _max_bytes = v; return this; }
	}
	private static Logger L = Logger.getLogger(CA.class.getName());

	private Context ctxt;
	private long timeout = 2000;

	private static final String cajname = "com.cosylab.epics.caj.CAJContext";

	static {
		// Silence CARepeater start failures logging by default
		// it will still print to stderr :P
		Logger rep = Logger.getLogger("com.cosylab.epics.caj.CARepeater");
		rep.setUseParentHandlers(false);
	}

	private final Map<String, CAJChannel> channels = new HashMap<>();

	// Create or re-use Channel
	// returned CAJChannel may already be connected
	private CAJChannel lookup(String pvname)
	{
		CAJChannel chan;
		synchronized (this) {
			chan = channels.get(pvname);
		}
		if(chan==null) {
			try {
				chan = (CAJChannel) ctxt.createChannel(pvname);
				L.info("Create connection to "+pvname);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create channel", e);
			}
		}
		CAJChannel other;
		synchronized (this) {
			other = channels.get(pvname);
			if(other==null)
				channels.put(pvname, chan);
		}
		if(other!=null && chan!=other) {
			try {
				chan.destroy();
			} catch (Exception e) {
				L.log(Level.WARNING, "Destruction of incidental channel fails", e);
			}
			chan = other;
		}
		return chan;
	}

	public CA(Config c)
	{
		// the following is inherently racy as configuration is done
		// indirectly through global environment or properties. 

		/* CAJ makes us decide whether to look for configuration in
		 * the OS environment, or Java properties (but not both...)
		 */
		if(c._useenv) {
			System.setProperty("jca.use_env", "true");
			L.info("Using CA settings from process environment");
		} else {
			System.setProperty("jca.use_env", "false");
			L.info("Using CA settings from Java properties");

			if(c._max_bytes<128) {
				L.warning("Ignoring max_array_bytes < 128 bytes");
			} else {
				System.setProperty(cajname+".max_array_bytes",
						Long.toString(c._max_bytes));
			}

			if(c._addr_list!=null)
				System.setProperty(cajname+".addr_list", c._addr_list);

			System.setProperty(cajname+".auto_addr_list", c._auto_addr_list ? "true" : "false");

			if(c._name_servers!=null)
				System.setProperty(cajname+".name_servers", c._name_servers);
		}

		JCALibrary jca = JCALibrary.getInstance();
		try {
			ctxt = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA);
		} catch (CAException e) {
			throw new RuntimeException("Failed to create JCA/CAJ context", e);
		}
	}

	/** Construct a new CA client context.
	 *
	 * Configuration is taken from the process environment if any
	 * environment variables beginning with "EPICS_CA_" are set.
	 * Otherwise the CAJ/JCA Java properties are used.
	 */
	public CA()
	{
		this(new Config());
	}

	/** Construct a new CA client context with the given configuration
	 *
	 * Ignores any CA environment variables.  Overrides java property settings
	 * for max array bytes, address list, and auto address list.
	 * Other java properties will be used.
	 */
	public CA(long max_array_bytes, String addr_list, boolean auto_addr_list)
	{
		this(new Config()
			.maxArrayBytes(max_array_bytes)
			.addrList(addr_list)
			.autoAddrList(auto_addr_list)
		);
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
		return ret/1000.0;
	}

	public static void setVerbose(boolean b)
	{
		if(b)
			L.setLevel(Level.ALL);
		else
			L.setLevel(Level.SEVERE);
	}

	public void printInfo()
	{
		ctxt.printInfo();
	}

	public void printInfo(PrintStream strm)
	{
		ctxt.printInfo(strm);
	}

	public String confInfo()
	{
		try(ByteArrayOutputStream bs = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(bs))
		{
			ctxt.printInfo(ps);
			return bs.toString();
		} catch (IOException e) {
			return e.toString();
		}
	}

	// Get only value

	public final Object getDouble(String name)            { return read(name); }
	public final Object getDouble(String name, int count) { return read(name); }
	public final Object getInt(String name)               { return read(name); }
	public final Object getInt(String name, int count)    { return read(name); }

	public long getUInt(String name)
	{
		// long -> int does sign extension, which we don't want here
		DBR dbr = getDBR(name, DBRType.TIME_INT, 1);
		int[] sv = (int[])dbr.getValue();
		return 0xffffffffl & (long)sv[0];
	}

	public long[] getUInt(String name, int count)
	{
		DBR dbr = getDBR(name, DBRType.TIME_INT, count);
		int[] sv = (int[])dbr.getValue();
		long[] ret = new long[sv.length];
		for(int i=0; i<sv.length; i++) {
			ret[i] = 0xffffffffl & (long)sv[i];
		}
		return ret;
	}

	// get value and metadata (alarm and timestamp)

	public PValue getDoubleM(String name)            { return readM(name); }
	public PValue getDoubleM(String name, int count) { return readM(name); }
	public PValue getIntM(String name)               { return readM(name); }
	public PValue getIntM(String name, int count)    { return readM(name); }


	// put value

	public void putDouble(String name, Object val)               { write(name, val, false); }
	public void putDouble(String name, Object val, boolean wait) { write(name, val, wait); }
	public void putInt(String name, Object val)                  { write(name, val, false); }
	public void putInt(String name, Object val, boolean wait)    { write(name, val, wait); }

	public void putUInt(String name, long val)                   { write(name, (int)val, false); }

	public void putUInt(String name, long val, boolean wait)     { write(name, (int)val, wait); }

	public void putString(String name, String val)               { write(name, val, false); }
	public void putString(String name, String val, boolean wait) { write(name, val, wait); }

	public Monitor monitor(String name)
	{
		CAJChannel chan = lookup(name);
		return new Monitor(chan);
	}

	static final Map<Class<?>, Class<?>> unbox = new HashMap<>();
	static {
		unbox.put(Double.class,  double.class);
		unbox.put(Float.class,   float.class);
		unbox.put(Integer.class, int.class);
		unbox.put(Short.class,   short.class);
		unbox.put(Byte.class,    char.class);
	}

	/* attempt to convert the provided object into an array of
	 * a type which CA can write (primitive integer,
	 * primitive floating point, or String).
	 */
	public static Object coerceWritable(Object val) {
		int count;
		Class<?> klass = val.getClass();

		// ensure val is an array
		if(klass.isArray()) {
			count = Array.getLength(val);
		} else {
			// package scalar in size 1 array
			count = 1;
			Object aval = Array.newInstance(klass, 1);
			Array.set(aval, 0, val);
			val = aval;
			klass = val.getClass();
		}

		Class<?> eklass = klass.getComponentType();

		if(eklass!=String.class && !eklass.isPrimitive()) {
			// unbox array values   eg. [Double] -> [double]

			Class<?> utype = unbox.get(eklass);
			if(utype==null)
				throw new RuntimeException("Unable to coerce "+klass.getName()+" into CA writeable");

			Object aval = Array.newInstance(utype, count);
			for(int i=0; i<count; i++) {
				Array.set(aval, i, Array.get(val, i));
			}
			val = aval;
		}

		return val;
	}
	
	/** Shorthand for write(name, val, false) */
	public void write(String name, Object val)
	{
		write(name, val, false);
	}

	/* map type to DBR code, also constitutes set of acceptable types */
	static final Map<Class<?>, DBRType> puttype = new HashMap<>();
	static {
		puttype.put(double.class, DBRType.DOUBLE);
		puttype.put(float.class, DBRType.FLOAT);
		puttype.put(int.class, DBRType.INT);
		puttype.put(short.class, DBRType.SHORT);
		puttype.put(byte.class, DBRType.BYTE);
		puttype.put(String.class, DBRType.STRING);
	}

	/** Issue a CA Put operation
	 *
	 * Value may be a scalar String or primitive (byte, short, int, float, or long).
	 * Arrays of allows scalars are also accepted.
	 *
	 * @param name PV name
	 * @param val Value to put.  A String, primitive, or array of primitive
	 * @param wait Whether to request, and wait for, completion notification
	 */
	public void write(String name, Object val, boolean wait)
	{
		val = coerceWritable(val);
		int count = Array.getLength(val);

		Class<?> klass = val.getClass();

		Class<?> eklass = klass.getComponentType();
		DBRType dtype = puttype.get(eklass);

		if(dtype==null) {
			throw new RuntimeException("Can't map "+klass.getName()+" to CA compatible type");
		}
		L.info("Put "+name+" as "+dtype.toString());

		putDBR(name, dtype, count, val, wait);
	}

	public PValue readM(String pvname)
	{
		DBR dbr = getDBR(pvname, null, -1);
		return new PValue(this, dbr);
	}

	public Object read(String pvname)
	{
		DBR ret = getDBR(pvname, null, -1);
		if(ret.isSTS()) {
			STS sts = (STS)ret;
			if(sts.getSeverity()==Severity.INVALID_ALARM)
				throw new RuntimeException("INVALID_ALARM active on PV "+pvname);
		}
		return ret.getValue();
	}

	public PValue[] readManyM(String[] names)
	{
		DBR[] dbrs = getDBRs(names, null, -1);
		PValue[] ret = new PValue[dbrs.length];
		for(int i=0; i<dbrs.length; i++) {
			if(dbrs[i]!=null)
				ret[i] = new PValue(this, dbrs[i]);
		}
		return ret;
	}

	// Methods for full cleanup

	/** Close out all channels */
	@Override
	public void close()
	{
		L.info("Closing context");
		if(ctxt!=null) {
			L.fine("Closing CA context");
			Map<String, CAJChannel> cmap;
			synchronized (this) {
				cmap = new HashMap<>(channels); // copy
				channels.clear();
			}
			for(CAJChannel chan : cmap.values()) {
				try {
					chan.destroy();
				}catch(Exception e){
					L.log(Level.SEVERE, "Error to disconnect PV", e);
				}
			}
			ctxt.dispose();
			ctxt=null;
		}
	}

	/** Alias for close() */
	public void destroy()
	{
		close();
	}

	@Override
	protected void finalize()
	{
		L.info("finalize context");
		close();
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
			CAJChannel chan;
			synchronized (this) {
				chan = channels.remove(name);
			}
			if(chan==null) continue;
			try {
				chan.destroy();
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
				lookup(name);
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
	private DBR getDBR(String name, DBRType dtype, int count)
	{
		try {
			CAJChannel ch = lookup(name);
			try(Getter get = new Getter(ch, dtype, count)) {

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
			}
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
	private DBR[] getDBRs(String[] names, DBRType dtype, int count)
	{
		CAJChannel[] chans = new CAJChannel[names.length];
		Getter[] getters = new Getter[names.length];
		DBR[] ret = new DBR[names.length];
		try {
			for(int i=0; i<names.length; i++) {
				chans[i] = lookup(names[i]);
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
		}finally{
			for(Getter get : getters) {
				try {
					get.close();
				} catch (Exception e) {
					L.log(Level.WARNING, "Error cleaning up Getter", e);
				}
			}
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
	private void putDBR(String name, DBRType dtype, int count, Object val, boolean wait)
	{
		try {
			CAJChannel ch = lookup(name);
			try (Putter putter = new Putter(ch, dtype, count, val, wait)) {

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
			}
		}catch(Exception e){
			throw new RuntimeException("Failed to put PV", e);
		}
	}

	// Inner classes for callbacks


	static public final Map<DBRType, DBRType> promotemap = new HashMap<>();
	static {
		promotemap.put(DBRType.STRING, DBRType.TIME_STRING);
		promotemap.put(DBRType.DOUBLE, DBRType.TIME_DOUBLE);
		promotemap.put(DBRType.FLOAT , DBRType.TIME_FLOAT);
		promotemap.put(DBRType.INT   , DBRType.TIME_INT);
		promotemap.put(DBRType.SHORT , DBRType.TIME_SHORT);
		promotemap.put(DBRType.BYTE  , DBRType.TIME_BYTE);
		// maybe handles CLASS_NAME and similar?
		promotemap.put(DBRType.UNKNOWN, DBRType.TIME_STRING);
	}

	// Helper for one-shot get/put operations
	static private abstract class OnConn implements ConnectionListener, AutoCloseable
	{
		protected CAJChannel chan;
		// Have we notified yet?
		public boolean done = false;
		// Have we called onConnect()?
		protected boolean beenconn = false;
		protected boolean listenconn = false;

		public DBRType dtype;
		public int dcount;

		public CAStatus status = CAStatus.IOINPROGESS;
		public DBR data;
		public Exception bad;

		OnConn(CAJChannel ch, DBRType dt, int dc) {
			chan = ch;
			dtype = dt;
			dcount = dc;
		}

		@Override
		public void close() throws Exception {}
		{
			if(listenconn) {
				try {
					chan.removeConnectionListener(this);
				} catch (Exception e) {
					throw new RuntimeException("error removing listener", e);
				}
			}
		}

		protected void doConnect()
		{
			synchronized (chan) {
				try {
					chan.addConnectionListenerAndFireIfConnected(this);
					listenconn = true;
				} catch (Exception e) {
					throw new RuntimeException("error adding listener", e);
				}
			}
		}

		@Override
		public void connectionChanged(ConnectionEvent ev) {
			if(done) return;

			if(ev.isConnected()) {
				if(dtype==null) {
					dtype = chan.getFieldType();
					DBRType pt = promotemap.get(dtype);
					if(pt!=null) dtype = pt;
				}
				if(dcount<0)
					dcount = 0; // CAJ supports dynamic array size
			}

			L.info(String.format("connectionChanged '%s' %sconnected",
					chan.getName(), ev.isConnected()?"":"dis"));

			if(ev.isConnected()) {
				try {
					synchronized (chan) {
						if(!beenconn) {
							beenconn = true;
							onConnect();
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
			} else {
				// oops, became disconnected
				// notify of failure
				synchronized(this) {
					done = true;
					bad = new RuntimeException("Connection lost");
					status = CAStatus.DISCONN;
					notify();
				}
			}
		}

		// Called within synchronized(chan) {}
		protected abstract void onConnect() throws Exception;
	}

	static private class Getter extends OnConn implements GetListener
	{

		public Getter(CAJChannel chan, DBRType t, int c) {
			super(chan, t, c);
			doConnect();
		}

		@Override
		public void onConnect() throws Exception {
			L.info(String.format("get request '%s' for %d", chan.getName(), dcount));
			chan.get(dtype, dcount, this);
			chan.getContext().flushIO();
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

	static private class Putter extends OnConn implements PutListener
	{
		private Object val;
		private boolean wait;

		public Putter(CAJChannel ch, DBRType d, int c, Object v, boolean w)
		{
			super(ch, d, c);
			val = v;
			wait = w;
			doConnect();
		}

		@Override
		public void onConnect() throws Exception {
			int maxcount = chan.getElementCount();
			if(dcount>maxcount) {
				throw new RuntimeException(String.format("Can't put %d elements to %s (max %d)",
						dcount, chan.getName(), maxcount));
			}
			if(wait) {
				chan.put(dtype, dcount, val, this);
			} else {
				chan.put(dtype, dcount, val);
			}
			chan.getContext().flushIO();
			if(!wait) {
				// Done now if not waiting for completion
				// otherwise done in putCompleted()
				synchronized (this) {
					status = CAStatus.NORMAL;
					done = true;
					notify();
				}
			}
		}

		@Override
		public synchronized void putCompleted(PutEvent ev) {
			L.info(String.format("putCompleted '%s'",chan.getName()));
			synchronized (this) {
				status = ev.getStatus();
				done = true;
				notify();
			}
		}
	}
}
