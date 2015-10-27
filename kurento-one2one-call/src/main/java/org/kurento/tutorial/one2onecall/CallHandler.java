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
package org.kurento.tutorial.one2onecall;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.jsonrpc.JsonUtils;
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
 * Protocol handler for 1 to 1 video call communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.3.1
 */
public class CallHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory
			.getLogger(CallHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	private final ConcurrentHashMap<String, CallMediaPipeline> pipelines = new ConcurrentHashMap<String, CallMediaPipeline>();

	@Autowired
	private KurentoClient kurento;

	@Autowired
	private UserRegistry registry;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(),
				JsonObject.class);
		UserSession user = registry.getBySession(session);

		if (user != null) {
			log.debug("Incoming message from user '{}': {}", user.getName(),
					jsonMessage);
		} else {
			log.debug("Incoming message from new user: {}", jsonMessage);
		}

		switch (jsonMessage.get("id").getAsString()) {
		case "register":
			try {
				register(session, jsonMessage);
			} catch (Throwable t) {
				handleErrorResponse(t, session, "registerResponse");
			}
			break;
		case "call":
			try {
				call(user, jsonMessage);
			} catch (Throwable t) {
				handleErrorResponse(t, session, "callResponse");
			}
			break;
		case "incomingCallResponse":
			incomingCallResponse(user, jsonMessage);
			break;
		case "onIceCandidate": {
			JsonObject candidate = jsonMessage.get("candidate")
					.getAsJsonObject();
			if (user != null) {
				IceCandidate cand = new IceCandidate(
						candidate.get("candidate").getAsString(),
						candidate.get("sdpMid").getAsString(),
						candidate.get("sdpMLineIndex").getAsInt());
				user.addCandidate(cand);
			}
			break;
		}
		case "stop":
			stop(session);
			break;
		default:
			break;
		}
	}

	private void handleErrorResponse(Throwable t, WebSocketSession session,
			String responseId) throws IOException {
		stop(session);
		log.error(t.getMessage(), t);
		JsonObject response = new JsonObject();
		response.addProperty("id", responseId);
		response.addProperty("response", "rejected");
		response.addProperty("message", t.getMessage());
		session.sendMessage(new TextMessage(response.toString()));
	}

	private void register(WebSocketSession session, JsonObject jsonMessage)
			throws IOException {
		String name = jsonMessage.getAsJsonPrimitive("name").getAsString();

		UserSession caller = new UserSession(session, name);
		String responseMsg = "accepted";
		if (name.isEmpty()) {
			responseMsg = "rejected: empty user name";
		} else if (registry.exists(name)) {
			responseMsg = "rejected: user '" + name + "' already registered";
		} else {
			registry.register(caller);
		}

		JsonObject response = new JsonObject();
		response.addProperty("id", "registerResponse");
		response.addProperty("response", responseMsg);
		caller.sendMessage(response);
	}

	private void call(UserSession caller, JsonObject jsonMessage)
			throws IOException {
		String to = jsonMessage.get("to").getAsString();
		String from = jsonMessage.get("from").getAsString();
		JsonObject response = new JsonObject();

		if (registry.exists(to)) {
			UserSession callee = registry.getByName(to);
			caller.setSdpOffer(
					jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString());
			caller.setCallingTo(to);

			response.addProperty("id", "incomingCall");
			response.addProperty("from", from);

			callee.sendMessage(response);
			callee.setCallingFrom(from);
		} else {
			response.addProperty("id", "callResponse");
			response.addProperty("response",
					"rejected: user '" + to + "' is not registered");

			caller.sendMessage(response);
		}
	}

	private void incomingCallResponse(final UserSession callee,
			JsonObject jsonMessage) throws IOException {
		String callResponse = jsonMessage.get("callResponse").getAsString();
		String from = jsonMessage.get("from").getAsString();
		final UserSession calleer = registry.getByName(from);
		String to = calleer.getCallingTo();

		if ("accept".equals(callResponse)) {
			log.debug("Accepted call from '{}' to '{}'", from, to);

			CallMediaPipeline pipeline = null;
			try {
				pipeline = new CallMediaPipeline(kurento);
				pipelines.put(calleer.getSessionId(), pipeline);
				pipelines.put(callee.getSessionId(), pipeline);

				String calleeSdpOffer = jsonMessage.get("sdpOffer")
						.getAsString();
				callee.setWebRtcEndpoint(pipeline.getCalleeWebRtcEP());
				pipeline.getCalleeWebRtcEP().addOnIceCandidateListener(
						new EventListener<OnIceCandidateEvent>() {
							@Override
							public void onEvent(OnIceCandidateEvent event) {
								JsonObject response = new JsonObject();
								response.addProperty("id", "iceCandidate");
								response.add("candidate", JsonUtils
										.toJsonObject(event.getCandidate()));
								try {
									synchronized (callee.getSession()) {
										callee.getSession()
												.sendMessage(new TextMessage(
														response.toString()));
									}
								} catch (IOException e) {
									log.debug(e.getMessage());
								}
							}
						});

				String calleeSdpAnswer = pipeline
						.generateSdpAnswerForCallee(calleeSdpOffer);
				String callerSdpOffer = registry.getByName(from).getSdpOffer();
				calleer.setWebRtcEndpoint(pipeline.getCallerWebRtcEP());
				pipeline.getCallerWebRtcEP().addOnIceCandidateListener(
						new EventListener<OnIceCandidateEvent>() {

							@Override
							public void onEvent(OnIceCandidateEvent event) {
								JsonObject response = new JsonObject();
								response.addProperty("id", "iceCandidate");
								response.add("candidate", JsonUtils
										.toJsonObject(event.getCandidate()));
								try {
									synchronized (calleer.getSession()) {
										calleer.getSession()
												.sendMessage(new TextMessage(
														response.toString()));
									}
								} catch (IOException e) {
									log.debug(e.getMessage());
								}
							}
						});

				String callerSdpAnswer = pipeline
						.generateSdpAnswerForCaller(callerSdpOffer);

				JsonObject startCommunication = new JsonObject();
				startCommunication.addProperty("id", "startCommunication");
				startCommunication.addProperty("sdpAnswer", calleeSdpAnswer);

				synchronized (callee) {
					callee.sendMessage(startCommunication);
				}

				pipeline.getCalleeWebRtcEP().gatherCandidates();

				JsonObject response = new JsonObject();
				response.addProperty("id", "callResponse");
				response.addProperty("response", "accepted");
				response.addProperty("sdpAnswer", callerSdpAnswer);

				synchronized (calleer) {
					calleer.sendMessage(response);
				}

				pipeline.getCallerWebRtcEP().gatherCandidates();

			} catch (Throwable t) {
				log.error(t.getMessage(), t);

				if (pipeline != null) {
					pipeline.release();
				}

				pipelines.remove(calleer.getSessionId());
				pipelines.remove(callee.getSessionId());

				JsonObject response = new JsonObject();
				response.addProperty("id", "callResponse");
				response.addProperty("response", "rejected");
				calleer.sendMessage(response);

				response = new JsonObject();
				response.addProperty("id", "stopCommunication");
				callee.sendMessage(response);
			}

		} else {
			JsonObject response = new JsonObject();
			response.addProperty("id", "callResponse");
			response.addProperty("response", "rejected");
			calleer.sendMessage(response);
		}
	}

	public void stop(WebSocketSession session) throws IOException {
		String sessionId = session.getId();
		if (pipelines.containsKey(sessionId)) {
			pipelines.get(sessionId).release();
			CallMediaPipeline pipeline = pipelines.remove(sessionId);
			pipeline.release();

			// Both users can stop the communication. A 'stopCommunication'
			// message will be sent to the other peer.
			UserSession stopperUser = registry.getBySession(session);
			UserSession stoppedUser = (stopperUser.getCallingFrom() != null)
					? registry.getByName(stopperUser.getCallingFrom())
					: registry.getByName(stopperUser.getCallingTo());

			JsonObject message = new JsonObject();
			message.addProperty("id", "stopCommunication");
			stoppedUser.sendMessage(message);

			stopperUser.clear();
			stoppedUser.clear();
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {
		registry.removeBySession(session);
	}

}
