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
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.factory.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Protocol handler for 1 to N video call communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class CallHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory
			.getLogger(CallHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	private ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<String, UserSession>();

	@Autowired
	private KurentoClient kurento;

	private CallMediaPipeline pipeline;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(),
				JsonObject.class);
		log.debug("Incoming message from session '{}': {}", session.getId(),
				jsonMessage);

		switch (jsonMessage.get("id").getAsString()) {
		case "master":
			master(session, jsonMessage);
			break;
		case "viewer":
			viewer(session, jsonMessage);
			break;
		case "stop":
			stop(session);
			break;
		default:
			break;
		}
	}

	private void master(WebSocketSession session, JsonObject jsonMessage)
			throws IOException {
		if (pipeline == null) {
			UserSession master = new UserSession(session);

			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer")
					.getAsString();
			pipeline = new CallMediaPipeline(kurento, master);
			String sdpAnswer = pipeline.loopback(sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "masterResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);
			master.sendMessage(response);

		} else {
			JsonObject response = new JsonObject();
			response.addProperty("id", "masterResponse");
			response.addProperty("response", "rejected");
			session.sendMessage(new TextMessage(response.toString()));
		}
	}

	private void viewer(WebSocketSession session, JsonObject jsonMessage)
			throws IOException {
		if (pipeline == null) {
			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "rejected");
			session.sendMessage(new TextMessage(response.toString()));
		} else {
			UserSession viewer = new UserSession(session);

			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer")
					.getAsString();
			String sdpAnswer = pipeline.connect(viewer, sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);
			viewer.sendMessage(response);

			viewers.put(session.getId(), viewer);
		}
	}

	private void stop(WebSocketSession session) throws IOException {
		if (pipeline != null) {
			String sessionId = session.getId();
			if (pipeline.getMasterUserSession().getSession().getId()
					.equals(sessionId)) {
				for (UserSession viewer : viewers.values()) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "stopCommunication");
					viewer.sendMessage(response);
				}

				log.info("Releasing media pipeline");
				pipeline.getPipeline().release();
				pipeline = null;
			} else if (viewers.containsKey(sessionId)) {
				viewers.remove(sessionId);
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {
		stop(session);
	}

}
