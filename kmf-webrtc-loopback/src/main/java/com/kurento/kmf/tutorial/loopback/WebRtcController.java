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
package com.kurento.kmf.tutorial.loopback;

import java.io.IOException;
import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.WebRtcEndpoint;
import com.kurento.kmf.media.factory.MediaPipelineFactory;

/**
 * WebRTC handler (application logic).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.3.1
 */
@RestController
public class WebRtcController {

	private final Logger log = LoggerFactory.getLogger(WebRtcController.class);

	@Autowired
	private MediaPipelineFactory mpf;

	@RequestMapping(value = "/webrtc", method = RequestMethod.POST)
	private String webrtc(@RequestBody String sdpOffer) throws IOException {

		// SDP Offer
		sdpOffer = URLDecoder.decode(sdpOffer, "UTF-8");
		log.debug("Received SDP offer: {}", sdpOffer);

		// Media Logic
		MediaPipeline mp = mpf.create();
		WebRtcEndpoint webRtcEndpoint = mp.newWebRtcEndpoint().build();
		FaceOverlayFilter faceOverlayFilter = mp.newFaceOverlayFilter().build();
		faceOverlayFilter.setOverlayedImage(
				"http://files.kurento.org/imgs/mario-wings.png", -0.35F, -1.2F,
				1.6F, 1.6F);
		webRtcEndpoint.connect(faceOverlayFilter);
		faceOverlayFilter.connect(webRtcEndpoint);

		// SDP Answer
		String responseSdp = webRtcEndpoint.processOffer(sdpOffer);
		log.debug("Sent SDP response: {}", responseSdp);

		return responseSdp;
	}

}
