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
package org.kurento.demo;

import java.io.IOException;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * CrowdDetector with RTSP media source (application and media logic).
 * 
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @since 5.0.4
 */
public class CrowdDetectorHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory
			.getLogger(CrowdDetectorHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	@Autowired
	private KurentoClient kurento;

	@Autowired
	private ConfigurationReader config;

	@Autowired
	private Pipeline pipeline;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(),
				JsonObject.class);

		log.debug("Incoming message: {}", jsonMessage);

		switch (jsonMessage.get("id").getAsString()) {
		case "start":
			try {
				start(session, jsonMessage);
			} catch (Throwable t) {
				sendError(session, t.getMessage());
			}
			break;

		case "stop":
			this.pipeline.removeWebRtcEndpoint(session.getId());
			break;

		case "updateFeed":
			updateFeed(jsonMessage);
			break;

		case "changeProcessingWidth":
			changeProcessingWidth(jsonMessage.get("width").getAsInt());
			break;

		case "onIceCandidate": {
			JsonObject candidate = jsonMessage.get("candidate")
					.getAsJsonObject();

			IceCandidate cand = new IceCandidate(candidate.get("candidate")
					.getAsString(), candidate.get("sdpMid").getAsString(),
					candidate.get("sdpMLineIndex").getAsInt());
			this.pipeline.addCandidate(cand, session.getId());
			break;
		}

		default:
			sendError(session,
					"Invalid message with id "
							+ jsonMessage.get("id").getAsString());
			break;
		}
	}

	private void start(final WebSocketSession session, JsonObject jsonMessage)
			throws IOException {

		updateFeed(jsonMessage);

		if (this.pipeline.getPlayerEndpoint() == null) {
			try {
				JsonObject response = new JsonObject();
				response.addProperty("id", "noPlayer");
				session.sendMessage(new TextMessage(response.toString()));
			} catch (IOException a) {
				log.error("Exception sending message", a);
			}
			return;
		}

		if (!this.pipeline.isPlaying()) {
			try {
				JsonObject response = new JsonObject();
				response.addProperty("id", "noPlaying");
				session.sendMessage(new TextMessage(response.toString()));
			} catch (IOException a) {
				log.error("Exception sending message", a);
			}
			return;
		}

		MediaPipeline mediaPipeline = this.pipeline.getPipeline();

		WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(
				mediaPipeline).build();

		webRtcEndpoint
				.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {

					@Override
					public void onEvent(OnIceCandidateEvent event) {
						JsonObject response = new JsonObject();
						response.addProperty("id", "iceCandidate");
						response.add("candidate",
								JsonUtils.toJsonObject(event.getCandidate()));
						try {
							synchronized (session) {
								session.sendMessage(new TextMessage(response
										.toString()));
							}
						} catch (IOException e) {
							log.debug(e.getMessage());
						}
					}
				});

		this.pipeline.setWebRtcEndpoint(session.getId(), webRtcEndpoint);

		this.pipeline.getCrowdDetectorFilter().connect(webRtcEndpoint);

		// SDP negotiation (offer and answer)
		String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
		String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

		// Sending response back to client
		JsonObject response = new JsonObject();
		response.addProperty("id", "startResponse");
		response.addProperty("sdpAnswer", sdpAnswer);
		response.addProperty("feedUrl", this.pipeline.getFeedUrl());
		response.addProperty("rois", gson.toJson(this.pipeline.getRois()));
		synchronized (session) {
			session.sendMessage(new TextMessage(response.toString()));
		}
		webRtcEndpoint.gatherCandidates();
	}

	private static void sendError(WebSocketSession session, String message) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "error");
			response.addProperty("message", message);
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Exception sending message", e);
		}
	}

	private void updateFeed(JsonObject jsonMessage) {

		JsonElement feedUrlJson = jsonMessage.get("feedUrl");
		if (feedUrlJson == null) {
			log.warn("No feed url defined");
		} else {
			String feedUrl = feedUrlJson.getAsString();
			log.debug("Updating video feed");
			this.pipeline.setFeedUrl(feedUrl);
		}
	}

	private void changeProcessingWidth(int width) {
		this.pipeline.getCrowdDetectorFilter().setProcessingWidth(width);
	}
}
