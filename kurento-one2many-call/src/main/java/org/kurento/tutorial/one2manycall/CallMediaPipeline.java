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
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class CallMediaPipeline {

	private MediaPipeline pipeline;
	private UserSession firstUserSession;

	public CallMediaPipeline(KurentoClient kurento, UserSession userSession) {
		pipeline = kurento.createMediaPipeline();
		firstUserSession = userSession;
		firstUserSession.setWebRtcEndpoint(new WebRtcEndpoint.Builder(pipeline)
				.build());
	}

	public String connect(UserSession nextUserSession) throws IOException {
		String sdpAnswer = null;
		if (!nextUserSession.equals(firstUserSession)) {
			nextUserSession.setWebRtcEndpoint(new WebRtcEndpoint.Builder(
					pipeline).build());
			updateConnection(nextUserSession);
			sdpAnswer = nextUserSession.getWebRtcEndpoint().processOffer(
					nextUserSession.getSdpOffer());
		} else {
			// Loopback
			firstUserSession.getWebRtcEndpoint().connect(
					firstUserSession.getWebRtcEndpoint());
			sdpAnswer = firstUserSession.getWebRtcEndpoint().processOffer(
					firstUserSession.getSdpOffer());
		}

		return sdpAnswer;
	}

	public void setFirstUserSession(UserSession firstUserSession) {
		this.firstUserSession = firstUserSession;
	}

	public void updateConnection(UserSession userSession) {
		firstUserSession.getWebRtcEndpoint().connect(
				userSession.getWebRtcEndpoint());
		userSession.getWebRtcEndpoint().connect(
				firstUserSession.getWebRtcEndpoint());
	}

	public MediaPipeline getPipeline() {
		return pipeline;
	}

	public UserSession getFirstUserSession() {
		return firstUserSession;
	}
}
