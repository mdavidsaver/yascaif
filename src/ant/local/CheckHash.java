package ant.local;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Hash the contents of a file and compare against an expected value.
 * 
 * Allowed values of algo="..." are those excepted by
 * java.security.MessageDigest.  Which always includes
 * "MD5", "SHA-1", and "SHA-256" as of Java 1.7
 * 
 * <target ...> 
 *   <taskdef name="check-hash" classname="ant.CheckHash" classpath="out"/>
 *   <check-hash algo="SHA-256" file="some.file"
 *          hash="208430ab09408..."/>
 *   ...
 */
public class CheckHash extends Task {

	File tohash;
	BigInteger thehash;
	String algo = "SHA-256";

	public void setFile(File f)
	{
		tohash = f;
	}

	public void setHash(String s)
	{
		thehash = new BigInteger(s, 16);
	}

	public void setAlgo(String s)
	{
		algo = s;
	}
	
	@Override
	public void execute()
	{
		try {
			MessageDigest mac = MessageDigest.getInstance(algo);

			try(InputStream fs = new FileInputStream(tohash))
			{

				byte[] buf = new byte[2048];

				while(true) {
					int N = fs.read(buf);
					if(N<=0)
						break;

					mac.update(buf, 0, N);
				}
			}

			byte[] buf = mac.digest();
			// prefix w/ a zero byte to ensure BigInteger
			// picks up a positive number
			byte[] buf2 = new byte[buf.length+1];
			System.arraycopy(buf, 0, buf2, 1, buf.length);
			BigInteger actual = new BigInteger(buf2);

			if(!actual.equals(thehash)) {
				throw new BuildException(tohash.toString()+" Hash "+
			actual.toString(16)+" is not expected "+thehash.toString(16));
			} else {
				System.out.println("Match "+tohash.toString());
			}
		} catch (BuildException e) {
			throw e;
		} catch (Exception e) {
			throw new BuildException("Failure", e);
		}
	}
}
