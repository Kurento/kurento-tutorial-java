package org.kurento.tutorial.one2manycall.model;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.client.Continuation;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class Room {

	private final static Logger log = LoggerFactory.getLogger(Room.class);

	private final String name;
	private final ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<String, UserSession>();

	private MediaPipeline pipeline;
	private KurentoClient kurentoClient;
	private UserSession presenter;

	public Room(String name, KurentoClient kurentoClient) {
		super();
		this.name = name;
		this.kurentoClient = kurentoClient;
	}

	public MediaPipeline getPipeline() {
		return pipeline;
	}

	public KurentoClient getKurentoClient() {
		return kurentoClient;
	}

	public String getName() {
		return name;
	}

	public Map<String, UserSession> getParticipants() {
		return participants;
	}
	
	public UserSession getPresenter() {
		return presenter;
	}

	public void createPipeline() {
		if (pipeline != null) {
			return;
		}
		try {
			kurentoClient.createMediaPipeline(new Continuation<MediaPipeline>() {

				@Override
				public void onSuccess(MediaPipeline result) throws Exception {
					pipeline = result;
					System.out.println("success : " + (pipeline == null));
					log.debug("ROOM {}: Created MediaPipeline", name);
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					log.error("ROOM {}: Failed to create MediaPipeline", name, cause);
				}
			});
		} catch (Exception e) {
			log.error("Unable to create media pipeline for room '{}'", name, e);
		}

//		pipeline.addErrorListener(new EventListener<ErrorEvent>() {
//
//			@Override
//			public void onEvent(ErrorEvent event) {
//				System.out.println("event");
//				log.warn("ROOM {}: Pipeline error encountered", name);
//			}
//		});
	}

	public void closePipeline() {
		if (pipeline == null) {
			return;
		}
		pipeline.release(new Continuation<Void>() {

			@Override
			public void onSuccess(Void result) throws Exception {
				log.debug("ROOM {}: Released Pipeline", Room.this.name);
			}

			@Override
			public void onError(Throwable cause) throws Exception {
				log.warn("ROOM {}: Could not successfully release Pipeline", Room.this.name, cause);
			}
		});
	}

	public void close() throws IOException {
		for(UserSession user : participants.values()) {
			JsonObject response = new JsonObject();
			response.addProperty("id", "stopCommunication");
			user.sendMessage(response);
		}
		if(pipeline != null) {
			pipeline.release();
			pipeline = null;
		}
	}
	
	public void joinByViewer(UserSession user) {
		if(user == null) {
			return;
		}
		participants.put(user.getSession().getId(), user);
	}
	
	public void joinByPresenter(UserSession user) {
		if(presenter != null) {
			return;
		}
		createPipeline();
		System.out.println("pipeline : " + (pipeline == null));
		System.out.println("user : " + (user == null));
		presenter = user;
		presenter.setWebRtcEndpoint(new WebRtcEndpoint.Builder(pipeline).build());
	}
	
	public void removeUser(UserSession user) {
		participants.remove(user.getSession().getId());
	}
}
