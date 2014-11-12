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
package org.kurento.tutorial.platedetector;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.KurentoClient;
import org.kurento.module.platedetector.PlateDetectedEvent;
import org.kurento.module.platedetector.PlateDetectorFilter;
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
 * Plate Detector handler (application and media logic).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.0.0
 */
public class PlateDetectorHandler extends TextWebSocketHandler {

	private final Logger log = LoggerFactory
			.getLogger(PlateDetectorHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	private final ConcurrentHashMap<String, MediaPipeline> pipelines = new ConcurrentHashMap<String, MediaPipeline>();

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

		case "stop":
			String sessionId = session.getId();
			if (pipelines.containsKey(sessionId)) {
				pipelines.get(sessionId).release();
				pipelines.remove(sessionId);
			}
			break;

		default:
			sendError(session,
					"Invalid message with id "
							+ jsonMessage.get("id").getAsString());
			break;
		}
	}

	private void start(final WebSocketSession session, JsonObject jsonMessage) {
		try {
			// Media Logic (Media Pipeline and Elements)
			MediaPipeline pipeline = kurento.createMediaPipeline();
			pipelines.put(session.getId(), pipeline);

			WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
					.build();
			PlateDetectorFilter plateDetectorFilter = new PlateDetectorFilter.Builder(
					pipeline).build();

			webRtcEndpoint.connect(plateDetectorFilter);
			plateDetectorFilter.connect(webRtcEndpoint);

			plateDetectorFilter
					.addPlateDetectedListener(new EventListener<PlateDetectedEvent>() {
						@Override
						public void onEvent(PlateDetectedEvent event) {
							JsonObject response = new JsonObject();
							response.addProperty("id", "plateDetected");
							response.addProperty("plate", event.getPlate());
							try {
								session.sendMessage(new TextMessage(response
										.toString()));
							} catch (Throwable t) {
								sendError(session, t.getMessage());
							}
						}
					});

			// SDP negotiation (offer and answer)
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

			// Sending response back to client
			JsonObject response = new JsonObject();
			response.addProperty("id", "startResponse");
			response.addProperty("sdpAnswer", sdpAnswer);
			session.sendMessage(new TextMessage(response.toString()));
		} catch (Throwable t) {
			sendError(session, t.getMessage());
		}
	}

	private void sendError(WebSocketSession session, String message) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "error");
			response.addProperty("message", message);
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Exception sending message", e);
		}
	}
}
