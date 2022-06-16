package com.learn.californium.client_dtls.v3_2_0.trylearn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.util.DaemonThreadFactory;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMainClient {

	private final int DEFAULT_PORT = 5684;		//5656
	private final long DEFAULT_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(10000);
	private final Logger LOG = LoggerFactory.getLogger(TestMainClient.class);
	private final String addr = "127.0.0.1";		//192.168.239.137

	
	public static CountDownLatch messageCounter;
	public static String payload = "HELLO WORLD_";
	
	
	
	public TestMainClient() {
	}

	public static void main(String[] args) throws InterruptedException {
		new TestMainClient().myrun();
	}
	
	private void myrun() {
		try {
			int clients = 1;
			int messages = 10;
			int maxMessages = (messages * clients);
			CountDownLatch num_clients_tmp = new CountDownLatch(clients);

			messageCounter = new CountDownLatch(maxMessages);
			//===========================create client connector===========================
			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),new DaemonThreadFactory("Aux#"));
	
			// Create & start clients
			MyClient client = new MyClient();
			
			// to settings before start
			client.my_configureToPrepare();

			// start client
			executor.execute(new Runnable() {
	
				@Override
				public void run() {
					client.startConnector();
					num_clients_tmp.countDown();
				}
			});
			num_clients_tmp.await();
	
			System.out.println(clients + " DTLS example clients started.");
			//===========================create socket address===========================
			// Get peer address
			InetSocketAddress peer = new InetSocketAddress(addr, DEFAULT_PORT);
			// send 1st message to server
			long nanos = System.nanoTime();
			long lastMessageCountDown = messageCounter.getCount();
			client.startTest(peer);
	
			System.out.println("original start nano is "+nanos);
			//
			// Wait with timeout or all messages send.
			while (messageCounter.await(DEFAULT_TIMEOUT_NANOS, TimeUnit.NANOSECONDS)==false) {
				long current = messageCounter.getCount();
				if (lastMessageCountDown == current && current < maxMessages) {
					nanos += DEFAULT_TIMEOUT_NANOS; 
					break;
				}
				lastMessageCountDown = current;
			}
			System.out.println("original start nano changed to "+nanos);
			
			//=========================== stop client ===========================
			client.stopConnector();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}