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
package org.kurento.tutorial.one2manycall;

import java.io.IOException;

import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.factory.KurentoClient;

/**
 * Media Pipeline (WebRTC endpoints, i.e. Kurento Media Elements) and
 * connections for the 1 to N video communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class CallMediaPipeline {

	private MediaPipeline pipeline;
	private UserSession masterUserSession;

	public CallMediaPipeline(KurentoClient kurento, UserSession userSession) {
		pipeline = kurento.createMediaPipeline();
		masterUserSession = userSession;
		masterUserSession
				.setWebRtcEndpoint(new WebRtcEndpoint.Builder(pipeline).build());
	}

	public String loopback(String sdpOffer) throws IOException {
		WebRtcEndpoint masterWebRtc = masterUserSession.getWebRtcEndpoint();
		masterWebRtc.connect(masterWebRtc);

		String sdpAnswer = masterWebRtc.processOffer(sdpOffer);
		return sdpAnswer;
	}

	public String connect(UserSession nextUserSession, String sdpOffer)
			throws IOException {
		WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(pipeline)
				.build();
		masterUserSession.getWebRtcEndpoint().connect(nextWebRtc);

		String sdpAnswer = nextWebRtc.processOffer(sdpOffer);
		return sdpAnswer;
	}

	public UserSession getMasterUserSession() {
		return masterUserSession;
	}

	public MediaPipeline getPipeline() {
		return pipeline;
	}

}
