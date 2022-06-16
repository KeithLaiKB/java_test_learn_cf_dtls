package com.learn.californium.client_dtls.v3_2_0.trylearn;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.MatcherMode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.AddressEndpointContext;
import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.config.CertificateAuthenticationMode;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.TimeDefinition;
import org.eclipse.californium.elements.config.Configuration.DefinitionsProvider;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.MdcConnectionListener;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore;
import org.eclipse.californium.scandium.dtls.resumption.AsyncResumptionVerifier;
import org.eclipse.californium.scandium.dtls.x509.AsyncKeyManagerCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyClient {
	
	private DTLSConnector dtlsConnector;
	private static final Logger LOG = LoggerFactory.getLogger(MyClient.class.getName());

	private AtomicInteger clientMessageCounter = new AtomicInteger();

	private static final char[] KEY_STORE_PASSWORD = "CksOneAdmin".toCharArray();
	private static final String KEY_STORE_ALIA ="myclientakeystore";

	private int DEFAULT_PORT 	= 5684;					//5656
	private String addr 		= "127.0.0.1";			//192.168.239.137
	
	public String clientKey_file					="clienta_cert.jks";
	public String clientKey_file_dir				="/mycerts/oneway_jks/mycerts";
	private static String clientKey_file_loc = null;
	
	public String serverCaCrt_file					="s_cacert.crt";
	public String serverCaCrt_file_dir				="/mycerts/oneway_jks/serverca";
	private static String serverCaCrt_file_loc = null;
	
	
	public MyClient() {
	}
	

	public void my_configureToPrepare() {
		String myusr_path = System.getProperty("user.dir");
		serverCaCrt_file_loc 							= 	myusr_path	+ serverCaCrt_file_dir		+"/" + 	serverCaCrt_file;
		
		clientKey_file_loc 							= 	myusr_path	+ clientKey_file_dir		+"/" + 	clientKey_file;
		
		InetSocketAddress bindToAddress = new InetSocketAddress(addr, DEFAULT_PORT);
		////////////////////file->FileInputStream->BufferedInputStream->X509Certificate //////////////////////////////////////
		// ref: https://gist.github.com/erickok/7692592
		FileInputStream fis= null;
		CertificateFactory cf = null;
		Certificate ca=null;
		try {
			cf = CertificateFactory.getInstance("X.509");
			fis = new FileInputStream(serverCaCrt_file_loc);
			InputStream caInput = new BufferedInputStream(fis);
		
		try {
			ca = cf.generateCertificate(caInput);
		} finally {
			caInput.close();
		}
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ref:https://github.com/eclipse/californium/issues/2006
		DtlsConfig.register();
		DefinitionsProvider DEFAULTS = new DefinitionsProvider() {

			@Override
			public void applyDefinitions(Configuration config) {
				config.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, 0);
				config.set(DtlsConfig.DTLS_RECEIVER_THREAD_COUNT, 2);
				config.set(DtlsConfig.DTLS_CONNECTOR_THREAD_COUNT, 2);
			}

		};
		Configuration dtlsConfig = Configuration.createWithFile(Configuration.DEFAULT_FILE, "DTLS example client", DEFAULTS);
		DtlsConnectorConfig.Builder dtlsConfigBuilder = DtlsConnectorConfig.builder(dtlsConfig);

		// security setting 
		dtlsConfigBuilder.set(DtlsConfig.DTLS_ROLE, DtlsRole.CLIENT_ONLY);
		dtlsConfigBuilder.set(DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE, CertificateAuthenticationMode.NONE);
		
		// private key
		PrivateKey prvKey = null;
		try {
			prvKey = SslContextUtil.loadPrivateKey(clientKey_file_loc, KEY_STORE_ALIA, KEY_STORE_PASSWORD, KEY_STORE_PASSWORD);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
		
		// ca certificate
		Certificate[] cas= {ca};
		dtlsConfigBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(prvKey, cas));

		//////////////// add verifier //////////////////
		boolean useTrustAll = false;
		StaticNewAdvancedCertificateVerifier.Builder verifierBuilder= StaticNewAdvancedCertificateVerifier.builder();
		if (useTrustAll==true) {
			verifierBuilder.setTrustAllCertificates();
		} 
		else if (useTrustAll==false) {
			Certificate[] trustedCertificates = cas;
			verifierBuilder.setTrustedCertificates(trustedCertificates);
		}
		// set all raw public keys
		verifierBuilder.setTrustAllRPKs();
		
		NewAdvancedCertificateVerifier verifier = verifierBuilder.build();
		dtlsConfigBuilder.setAdvancedCertificateVerifier(verifier);
		////////////////////////////////////////////////////
		
		// other settings
		dtlsConfigBuilder.setLoggingTag("dtls:" + StringUtil.toString(bindToAddress));
		dtlsConfigBuilder.setConnectionListener(new MdcConnectionListener());

		// dtls connector
		dtlsConnector = new DTLSConnector(dtlsConfigBuilder.build());
		CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
		builder.setConnector(dtlsConnector);
		if (MatcherMode.PRINCIPAL == dtlsConfig.get(CoapConfig.RESPONSE_MATCHING)) {
			builder.setEndpointContextMatcher(new PrincipalEndpointContextMatcher(true));
		}
		builder.setConfiguration(dtlsConfig);

		dtlsConnector.setRawDataReceiver(new MyRawDataChannelImpl(this));
	}

	
	public void setDtlsConnector(DTLSConnector dtlsConnector) {
		this.dtlsConnector = dtlsConnector;
	}
	public DTLSConnector getDtlsConnector() {
		return dtlsConnector;
	}

	public void startConnector() {
		try {
			this.dtlsConnector.start();
		} catch (IOException e) {
			LOG.error("Cannot start connector", e);
		}
	}
	public int stopConnector() {
		if (dtlsConnector.isRunning()) {
			dtlsConnector.destroy();
		}
		return clientMessageCounter.get();
	}
	
	public void startTest(InetSocketAddress peer) {
		RawData data = RawData.outbound(TestMainClient.payload.getBytes(), new AddressEndpointContext(peer), null, false);
		dtlsConnector.send(data);
		System.out.println("client try send message");
	}
	
	public void receive(RawData raw) {
		TestMainClient.messageCounter.countDown();
		//
		long c = TestMainClient.messageCounter.getCount();
		System.out.println("Received my message:"+ new String(raw.getBytes())); 
		if (0 < c) {
			try {
				RawData data = RawData.outbound((TestMainClient.payload + 1 + ".").getBytes(), raw.getEndpointContext(), null, false);
				dtlsConnector.send(data);
			} catch (IllegalStateException e) {
				LOG.debug("send failed after {} messages", (c - 1), e);
			}
		} else {
			dtlsConnector.destroy();
		}

	}
	
}