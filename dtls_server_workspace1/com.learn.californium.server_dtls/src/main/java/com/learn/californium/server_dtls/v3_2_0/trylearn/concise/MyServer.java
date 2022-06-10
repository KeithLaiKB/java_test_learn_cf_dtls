package com.learn.californium.server_dtls.v3_2_0.trylearn.concise;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.config.CoapConfig.MatcherMode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.PrincipalEndpointContextMatcher;
import org.eclipse.californium.elements.config.CertificateAuthenticationMode;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.DefinitionsProvider;
import org.eclipse.californium.elements.config.SystemConfig;
import org.eclipse.californium.elements.config.TimeDefinition;
import org.eclipse.californium.elements.config.UdpConfig;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.DtlsHealthLogger;
import org.eclipse.californium.scandium.MdcConnectionListener;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedMultiPskStore;
import org.eclipse.californium.scandium.dtls.resumption.AsyncResumptionVerifier;
import org.eclipse.californium.scandium.dtls.x509.AsyncKeyManagerCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.AsyncNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyServer {
	
	private int DEFAULT_PORT 	= 5684;					//5656
	private String addr 		= "127.0.0.1";			//192.168.239.137
	private final Logger LOG = LoggerFactory.getLogger(MyServer.class.getName());
	private final char[] KEY_STORE_PASSWORD = "myKeystoreAdministrator".toCharArray();
	


	private DTLSConnector dtlsConnector;
	
	public MyServer() {
		
	}
	
	public String serverCaCrt_file					="s_cacert.crt";
	public String serverCaCrt_file_dir				="/mycerts/oneway_jks/myca";
	private static String serverCaCrt_file_loc = null;
	
	public String serverKey_file					="server_cert.jks";
	public String serverKey_file_dir				="/mycerts/oneway_jks/mycerts";
	private static String serverKey_file_loc = null;
	
	public String serverCrt_file					="server_cert.crt";
	public String serverCrt_file_dir				="/mycerts/oneway_jks/mycerts";
	private static String serverCrt_file_loc = null;
	
	public void my_configureToPrepare() {
		String myusr_path = System.getProperty("user.dir");
		serverCaCrt_file_loc 							= 	myusr_path	+ serverCaCrt_file_dir		+"/" + 	serverCaCrt_file;
		serverKey_file_loc								= 	myusr_path	+ serverKey_file_dir		+"/" + 	serverKey_file;
		serverCrt_file_loc								= 	myusr_path	+ serverCrt_file_dir		+"/" + 	serverCrt_file;
		
		InetSocketAddress bindToAddress = new InetSocketAddress(addr, DEFAULT_PORT);
        //////////////////// file->FileInputStream->BufferedInputStream->X509Certificate //////////////////////////////////////
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
				config.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, 6);
				config.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, false);
			}

		};
		Configuration dtlsConfig = Configuration.createWithFile(Configuration.DEFAULT_FILE, "DTLS example server", DEFAULTS);
		DtlsConnectorConfig.Builder dtlsConfigBuilder = DtlsConnectorConfig.builder(dtlsConfig);

		// address
		dtlsConfigBuilder.setAddress(bindToAddress);	
		
		// security setting 
		dtlsConfigBuilder.set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY);
		dtlsConfigBuilder.set(DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE, CertificateAuthenticationMode.NONE);
		
		// private key
		PrivateKey prvKey = null;
		try {
			prvKey = SslContextUtil.loadPrivateKey(serverKey_file_loc, "mykeystoreAlias", KEY_STORE_PASSWORD, KEY_STORE_PASSWORD);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
		// ca certificate
		Certificate[] cas= {ca};
		dtlsConfigBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(prvKey, cas));

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

		// setting receiver
		dtlsConnector.setRawDataReceiver(new MyRawDataChannelImpl(dtlsConnector));
		

	}
	
	public void start() {
		try {
			dtlsConnector.start();
			System.out.println("DTLS example server started");
		} catch (IOException e) {
			throw new IllegalStateException(
					"Unexpected error starting the DTLS UDP server", e);
		}
	}
	
	
}