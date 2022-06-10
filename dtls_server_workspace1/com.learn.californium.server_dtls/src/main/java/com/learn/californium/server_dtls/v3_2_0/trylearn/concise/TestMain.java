package com.learn.californium.server_dtls.v3_2_0.trylearn.concise;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMain {

	private static final Logger LOG = LoggerFactory
			.getLogger(TestMain.class.getName());

	public TestMain() {
		
	}



	public static void main(String[] args) {
		MyServer server = new MyServer();
		server.my_configureToPrepare();		// settings before start
		server.start();						// start
		//
		try {
			for (;;) {
				Thread.sleep(5000);
			}
		} catch (InterruptedException e) {
		}
	}

}