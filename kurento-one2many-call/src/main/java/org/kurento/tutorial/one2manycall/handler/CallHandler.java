
package org.kurento.tutorial.one2manycall.handler;

import java.io.IOException;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.tutorial.one2manycall.model.Room;
import org.kurento.tutorial.one2manycall.model.UserSession;
import org.kurento.tutorial.one2manycall.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class CallHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(CallHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
		log.debug("Incoming message from session '{}': {}", session.getId(), jsonMessage);

		String id = jsonMessage.get("id").getAsString();

		if (id.equals("presenter")) {
			String roomName = jsonMessage.get("roomName").getAsString();
			try {
				presenter(session, roomName, jsonMessage);
			} catch (Exception e) {
				handleErrorResponse(e, session, "presenterResponse");
			}
			return;
		}
		if (id.equals("viewer")) {
			String roomName = jsonMessage.get("roomName").getAsString();
			try {
				viewer(session, roomName, jsonMessage);
			} catch (Exception e) {
				handleErrorResponse(e, session, "viewerResponse");
			}
			return;
		}
		if (id.equals("stop")) {
			try {
				stop(session);
			} catch (Exception e) {
				handleErrorResponse(e, session, "stopResponse");
			}
			return;
		}
		return;
	}

	private void handleErrorResponse(Throwable throwable, WebSocketSession session, String responseId)
			throws IOException {
		stop(session);
		log.error(throwable.getMessage(), throwable);
		JsonObject response = new JsonObject();
		response.addProperty("id", responseId);
		response.addProperty("response", "rejected");
		response.addProperty("message", throwable.getMessage());
		session.sendMessage(new TextMessage(response.toString()));
	}

	private synchronized void presenter(final WebSocketSession session, String roomName, JsonObject jsonMessage)
			throws IOException {
		Room room = RoomRepository.rooms().get(roomName);
		UserSession presenter = new UserSession(session);
		if (room == null) {
			// 방 만들고
			RoomRepository.addRoom(new Room(roomName, KurentoClient.create()));
			// 방 들어가고
			RoomRepository.joinRoomByPresenter(presenter, roomName);
			// 방 가져와서
			room = RoomRepository.rooms().get(roomName);
			WebRtcEndpoint presenterRtcEndpoint = room.getPresenter().getWebRtcEndpoint();

			presenterRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

				@Override
				public void onEvent(IceCandidateFoundEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "iceCandidate");
					response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
				}
			});

			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
			String sdpAnswer = presenterRtcEndpoint.processOffer(sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "presenterResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);

			synchronized (session) {
				presenter.sendMessage(response);
			}
			presenterRtcEndpoint.gatherCandidates();
		} else {
			JsonObject response = new JsonObject();
			response.addProperty("id", "presenterResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message", "Another user is currently acting as sender. Try again later ...");
			session.sendMessage(new TextMessage(response.toString()));
		}
	}

	private synchronized void viewer(final WebSocketSession session, String roomName, JsonObject jsonMessage)
			throws IOException {
		Room room = RoomRepository.rooms().get(roomName);
		if (room == null) {
			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message", "No active sender now. Become sender or . Try again later ...");
			session.sendMessage(new TextMessage(response.toString()));
		} else {
			UserSession viewer = new UserSession(session);
			WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(room.getPipeline()).build();

			nextWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

				@Override
				public void onEvent(IceCandidateFoundEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "iceCandidate");
					response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
				}
			});
			viewer.setWebRtcEndpoint(nextWebRtc);
			RoomRepository.joinRoomByViewer(viewer, roomName);
			room.getPresenter().getWebRtcEndpoint().connect(nextWebRtc);

			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
			String sdpAnswer = nextWebRtc.processOffer(sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);

			synchronized (session) {
				viewer.sendMessage(response);
			}
			nextWebRtc.gatherCandidates();
		}
	}

	private synchronized void stop(WebSocketSession session) throws IOException {
		String sessionId = session.getId();
		Room room = RoomRepository.findByPresenterId(sessionId);
		if (room != null) {
			room.close();
			return;
		}
		for(Room viewerRoom : RoomRepository.rooms().values()) {
			for(UserSession user : viewerRoom.getParticipants().values()) {
				if(user.getSession().getId().equals(session.getId())) {
					user.getWebRtcEndpoint().release();
					viewerRoom.removeUser(user);
				}
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		stop(session);
	}
}
