/**
 * 
 */
package net.azib.ipscan.fetchers;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.azib.ipscan.config.ScannerConfig;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.core.values.NotAvailableValue;
import net.azib.ipscan.core.values.NumericListValue;

import org.junit.Before;
import org.junit.Test;

/**
 * PortsFetcherTest
 *
 * @author Anton Keks
 */
public class PortsFetcherTest extends AbstractFetcherTestCase {
	
	private ScannerConfig config;

	@Before
	public void setUp() throws Exception {
		config = createMock(ScannerConfig.class);
		fetcher = new PortsFetcher(config);
	}
	
	@Test
	public void numberOfPortsInFullName() throws Exception {
		config.portString = "";
		assertEquals(fetcher.getName() + " " + NotAvailableValue.INSTANCE, fetcher.getFullName());

		config.portString = "1-3";
		assertEquals(fetcher.getName() + " [3]", fetcher.getFullName());
	}
	
	@Test
	public void scanWithNoResults() throws Exception {
		// this port is unlikely to be open :-)
		config.portString = "65535";
		fetcher.init();
		
		Object value = fetcher.scan(new ScanningSubject(InetAddress.getLocalHost()));
		assertNull(value);

		fetcher.cleanup();
	}
	
	@Test
	public void scanAbortedOnInterrupt() throws Exception {
		// these ports are unlikely to be open :-)
		config.portString = "65530-65535";
		fetcher.init();
		
		Object value = fetcher.scan(new ScanningSubject(InetAddress.getLocalHost()));
		assertNull(value);
		
		// reasonably long timeout (if tests fails, we will have to wait this long...)
		config.portTimeout = 3000;
		// but we don't want to wait :-)
		Thread.currentThread().interrupt();
		long testStartTime = System.currentTimeMillis();
		// this host is unlikely to respond
		value = fetcher.scan(new ScanningSubject(InetAddress.getByName("10.255.255.254")));
		assertNull(value);
		assertTrue("port scanning wasn't interrupted", System.currentTimeMillis() - testStartTime < 3000);
		
		// reset interrupted flag
		assertTrue(Thread.interrupted());
		
		fetcher.cleanup();
	}
	
	@Test
	public void scanWithResults() throws Exception {
		// start local single-accept server
		Thread server = new Thread() {
			public void run() {
				try {
					ServerSocket server = new ServerSocket(65431);
					synchronized (this) {
						this.notify();
					}
					Socket socket = server.accept();
					socket.close();
					server.close();
				}
				catch (IOException e) {
					fail("couldn't test creation of server");
				}
			}
		};
		server.start();
		
		config.portString = "65431";
		fetcher.init();
		
		synchronized (server) {
			server.wait();
		}
		NumericListValue value = (NumericListValue) fetcher.scan(new ScanningSubject(InetAddress.getLocalHost()));
		assertEquals(config.portString, value.toString());
		
		fetcher.cleanup();
	}

}
