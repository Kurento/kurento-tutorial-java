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
package org.kurento.tutorial.one2onecalladv;

import static org.kurento.tutorial.one2onecalladv.CallMediaPipeline.RECORDING_EXT;
import static org.kurento.tutorial.one2onecalladv.CallMediaPipeline.RECORDING_PATH;

import java.io.IOException;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;

/**
 * Media Pipeline (connection of Media Elements) for playing the recorded one to
 * one video communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class PlayMediaPipeline {

	private static final Logger log = LoggerFactory
			.getLogger(PlayMediaPipeline.class);

	private MediaPipeline pipeline;
	private WebRtcEndpoint webRtc;
	private PlayerEndpoint player;

	public PlayMediaPipeline(KurentoClient kurento, String user,
			final WebSocketSession session) {
		// Media pipeline
		pipeline = kurento.createMediaPipeline();

		// Media Elements (WebRtcEndpoint, PlayerEndpoint)
		webRtc = new WebRtcEndpoint.Builder(pipeline).build();
		player = new PlayerEndpoint.Builder(pipeline, RECORDING_PATH + user
				+ RECORDING_EXT).build();

		// Connection
		player.connect(webRtc);

		// Player listeners
		player.addErrorListener(new EventListener<ErrorEvent>() {
			@Override
			public void onEvent(ErrorEvent event) {
				log.info("ErrorEvent: {}", event.getDescription());
				sendPlayEnd(session);
			}
		});
		player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				sendPlayEnd(session);
			}
		});
	}

	public void sendPlayEnd(WebSocketSession session) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "playEnd");
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Error sending playEndOfStream message", e);
		}

		// Release pipeline
		pipeline.release();
	}

	public void play() {
		player.play();
	}

	public String generateSdpAnswer(String sdpOffer) {
		return webRtc.processOffer(sdpOffer);
	}

	public MediaPipeline getPipeline() {
		return pipeline;
	}

}
