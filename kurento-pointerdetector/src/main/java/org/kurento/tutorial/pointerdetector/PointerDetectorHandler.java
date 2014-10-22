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
package org.kurento.tutorial.pointerdetector;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.factory.KurentoClient;
import org.kurento.module.pointerdetector.PointerDetectorFilter;
import org.kurento.module.pointerdetector.PointerDetectorWindowMediaParam;
import org.kurento.module.pointerdetector.WindowInEvent;
import org.kurento.module.pointerdetector.WindowOutEvent;
import org.kurento.module.pointerdetector.WindowParam;
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
 * Pointer Detector handler (application and media logic).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 5.0.0
 */
public class PointerDetectorHandler extends TextWebSocketHandler {

	private final Logger log = LoggerFactory
			.getLogger(PointerDetectorHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	private final ConcurrentHashMap<String, MediaPipeline> pipelines = new ConcurrentHashMap<String, MediaPipeline>();

	@Autowired
	private KurentoClient kurento;

	PointerDetectorFilter pointerDetectorFilter;

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

		case "calibrate":
			calibrate(session, jsonMessage);
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

	private void calibrate(WebSocketSession session, JsonObject jsonMessage) {
		if (pointerDetectorFilter != null) {
			pointerDetectorFilter.trackColorFromCalibrationRegion();
		}
	}

	private void start(final WebSocketSession session, JsonObject jsonMessage) {
		try {
			// Media Logic (Media Pipeline and Elements)
			MediaPipeline pipeline = kurento.createMediaPipeline();
			pipelines.put(session.getId(), pipeline);

			WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
					.build();
			pointerDetectorFilter = new PointerDetectorFilter.Builder(pipeline,
					new WindowParam(5, 5, 30, 30)).build();

			pointerDetectorFilter
					.addWindow(new PointerDetectorWindowMediaParam("window0",
							50, 50, 500, 150));

			pointerDetectorFilter
					.addWindow(new PointerDetectorWindowMediaParam("window1",
							50, 50, 500, 250));

			webRtcEndpoint.connect(pointerDetectorFilter);
			pointerDetectorFilter.connect(webRtcEndpoint);

			pointerDetectorFilter
					.addWindowInListener(new EventListener<WindowInEvent>() {
						@Override
						public void onEvent(WindowInEvent event) {
							JsonObject response = new JsonObject();
							response.addProperty("id", "windowIn");
							response.addProperty("roiId", event.getWindowId());
							try {
								session.sendMessage(new TextMessage(response
										.toString()));
							} catch (Throwable t) {
								sendError(session, t.getMessage());
							}
						}
					});

			pointerDetectorFilter
					.addWindowOutListener(new EventListener<WindowOutEvent>() {

						@Override
						public void onEvent(WindowOutEvent event) {
							JsonObject response = new JsonObject();
							response.addProperty("id", "windowOut");
							response.addProperty("roiId", event.getWindowId());
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
