/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.tutorial.helloworld;

import java.util.concurrent.CountDownLatch;

import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceGatheringDoneEvent;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello World REST Controller (application logic).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.0.0-SNAPSHOT
 */
@RestController
public class HelloWorldController {

	@Autowired
	private KurentoClient kurento;

	@RequestMapping(value = "/helloworld", method = RequestMethod.POST)
	private String processRequest(@RequestBody String sdpOffer) {

		final CountDownLatch eventReceived = new CountDownLatch(1);
		// Media Logic
		MediaPipeline pipeline = kurento.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
				.build();
		webRtcEndpoint
				.addOnIceGatheringDoneListener(new EventListener<OnIceGatheringDoneEvent>() {
					@Override
					public void onEvent(OnIceGatheringDoneEvent event) {
						eventReceived.countDown();
					}
				});

		webRtcEndpoint.connect(webRtcEndpoint);
		webRtcEndpoint.processOffer(sdpOffer);
		webRtcEndpoint.gatherCandidates();

		try {
			eventReceived.await();
		} catch (InterruptedException e) {

		}
		// SDP negotiation (offer and answer)
		String responseSdp = webRtcEndpoint.getLocalSessionDescriptor();
		return responseSdp;
	}

}
