package com.learn.californium.server_dtls.v3_2_0.trylearn.concise;

import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyRawDataChannelImpl implements RawDataChannel {

	private Connector connector;
	private static final Logger LOG = LoggerFactory.getLogger(MyRawDataChannelImpl.class.getName());
	public MyRawDataChannelImpl(Connector con) {
		this.connector = con;
	}

	@Override
	public void receiveData(final RawData raw) {
		if (LOG.isInfoEnabled()) {
			LOG.info("Received request: {}", new String(raw.getBytes()));
		}
		//RawData response = RawData.outbound("ACK".getBytes(),raw.getEndpointContext(), null, false);
		RawData response = RawData.outbound("hellllo".getBytes(),raw.getEndpointContext(), null, false);
		connector.send(response);
	}
}