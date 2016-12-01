package yascaif.demo;


import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;


/** MATLAB event handler demo
 *
 * @note This code needs to be in the *static* class path in order for the
 * Listener code generation tricks to work.
 *
 * cf.
 *  http://undocumentedmatlab.com/blog/matlab-callbacks-for-java-events
 *  http://undocumentedmatlab.com/blog/matlab-callbacks-for-java-events-in-r2014a
 *
 *  evt = yascaif.demo.EventTest();
 *  evt = handle(evt, 'CallbackProperties'); % needed for >=2014a
 *  evt.get  % should list EventNameCallback 
 *  set(evt, 'EventNameCallback', @(h,e) disp(e.value));
 *  evt.notify(4.2);
 */
public class EventTest implements AutoCloseable {
	private static Logger log = Logger.getLogger(EventTest.class.getCanonicalName());
	
	public class MyEvt extends EventObject
	{
		private static final long serialVersionUID = 1L;

		public double value;

		public MyEvt(Object obj, double val) {
			super(obj);
			value = val;
			log.info("MyEvt ctor");
		}
		
		double getValue() {
			return value;
		}
	}
	
	// The class name must end in 'Listener' for matlab to find it.
	public interface TheListener extends EventListener
	{
		void EventName(MyEvt evt);
	}

	private final List<TheListener> listeners = new LinkedList<>();

	// add/remove suffix must match *Listener name
	public synchronized void addTheListener(TheListener l)
	{
		log.info("addTheListener");
		listeners.add(l);
	}

	public synchronized void removeTheListener(TheListener l)
	{
		log.info("removeTheListener");
		listeners.remove(l);
	}
	
	public void callme(double val)
	{
		log.info("start notify");
		
		MyEvt evt = new MyEvt(this, val);

		for (TheListener l : listeners) {
			l.EventName(evt);
		}
		
		log.info("end notify");
	}

	// matlab doesn't know about AutoClosable
	@Override
	public void close() {
		log.info(EventTest.class.getName()+" close");
	}

	// GC finalize still gets called eventually after the matlab workspace is cleared
	@Override
	protected void finalize() {
		log.info(EventTest.class.getName()+" finalize");
	}
}
