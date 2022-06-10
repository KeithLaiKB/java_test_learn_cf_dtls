package com.learn.californium.client_dtls.v3_2_0.trylearn;


import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyRawDataChannelImpl implements RawDataChannel {

	private static final Logger LOG = LoggerFactory.getLogger(MyRawDataChannelImpl.class.getName());
	private MyClient myClient;
	
	public MyRawDataChannelImpl(MyClient myClient) {
		this.myClient = myClient;
	}

	@Override
	public void receiveData(RawData raw) {
		if (this.myClient.getDtlsConnector().isRunning()) {
			this.myClient.receive(raw);
		}
	}
	
	
	
	

}