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
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class CallHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory
			.getLogger(CallHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	@Autowired
	private KurentoClient kurento;

	@Autowired
	private UserRegistry registry;

	private CallMediaPipeline pipeline;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(),
				JsonObject.class);
		log.debug("Incoming message from session '{}': {}", session.getId(),
				jsonMessage);

		switch (jsonMessage.get("id").getAsString()) {
		case "call":
			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer")
					.getAsString();
			UserSession userSession = new UserSession(session, sdpOffer);
			call(userSession);
			break;
		case "stop":
			stop(session);
			break;
		default:
			break;
		}
	}

	private void call(UserSession userSession) throws IOException {
		registry.add(userSession);
		if (pipeline == null) {
			pipeline = new CallMediaPipeline(kurento, userSession);
		}
		String sdpAnswer = pipeline.connect(userSession);

		JsonObject response = new JsonObject();
		response.addProperty("id", "callResponse");
		response.addProperty("response", "accepted");
		response.addProperty("sdpAnswer", sdpAnswer);
		userSession.sendMessage(response);
	}

	private void stop(WebSocketSession session) throws IOException {
		registry.remove(session);

		if (pipeline != null) {
			UserSession first = registry.getFirst();
			if (first == null) {
				// Nobody registered
				pipeline.getPipeline().release();
				pipeline = null;
			} else {
				pipeline.setFirstUserSession(first);
				pipeline.updateConnection(registry.getLast());
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {
		stop(session);
	}

}
