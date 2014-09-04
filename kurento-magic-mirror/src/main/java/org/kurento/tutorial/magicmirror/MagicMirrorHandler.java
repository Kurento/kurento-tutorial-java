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
package org.kurento.tutorial.magicmirror;

import java.io.IOException;

import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.factory.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Magic Mirror handler (application and media logic).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class MagicMirrorHandler extends TextWebSocketHandler {

	private final Logger log = LoggerFactory
			.getLogger(MagicMirrorHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	@Autowired
	private KurentoClient kurento;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(),
				JsonObject.class);

		log.debug("Incoming message: {}", jsonMessage);

		switch (jsonMessage.get("id").getAsString()) {
		case "start":
			start(session, jsonMessage);
			break;
		default:
			break;
		}
	}

	private void start(WebSocketSession session, JsonObject jsonMessage)
			throws IOException {
		// Media Logic (Media Pipeline and Elements)
		MediaPipeline pipeline = kurento.createMediaPipeline();
		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
				.build();
		FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(
				pipeline).build();
		faceOverlayFilter.setOverlayedImage(
				"http://files.kurento.org/imgs/mario-wings.png", -0.35F, -1.2F,
				1.6F, 1.6F);

		webRtcEndpoint.connect(faceOverlayFilter);
		faceOverlayFilter.connect(webRtcEndpoint);

		// SDP negotiation (offer and answer)
		String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
		String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

		// Sending response back to client
		JsonObject response = new JsonObject();
		response.addProperty("id", "startResponse");
		response.addProperty("sdpAnswer", sdpAnswer);
		session.sendMessage(new TextMessage(response.toString()));
	}

}
