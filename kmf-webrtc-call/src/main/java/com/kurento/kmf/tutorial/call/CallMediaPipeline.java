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
package com.kurento.kmf.tutorial.call;

import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

/**
 * Media Pipeline (WebRTC endpoints, i.e. Kurento Media Elements) and
 * connections for the 1 to 1 video communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.3.1
 */
public class CallMediaPipeline {

	private MediaPipeline mp;
	private WebRtcEndpoint callerWebRtcEP;
	private WebRtcEndpoint calleeWebRtcEP;

	public CallMediaPipeline(MediaPipelineFactory mpf) {
		this.mp = mpf.create();
		this.callerWebRtcEP = mp.newWebRtcEndpoint().build();
		this.calleeWebRtcEP = mp.newWebRtcEndpoint().build();

		this.callerWebRtcEP.connect(this.calleeWebRtcEP);
		this.calleeWebRtcEP.connect(this.callerWebRtcEP);
	}

	public String generateSdpAnswerForCaller(String sdpOffer) {
		return callerWebRtcEP.processOffer(sdpOffer);
	}

	public String generateSdpAnswerForCallee(String sdpOffer) {
		return calleeWebRtcEP.processOffer(sdpOffer);
	}

}
