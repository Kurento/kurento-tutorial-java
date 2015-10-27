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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;

/**
 * User session.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class UserSession {

	private static final Logger log = LoggerFactory
			.getLogger(UserSession.class);

	private final String name;
	private final WebSocketSession session;

	private String sdpOffer;
	private String callingTo;
	private String callingFrom;
	private WebRtcEndpoint webRtcEndpoint;
	private WebRtcEndpoint playingWebRtcEndpoint;
	private final List<IceCandidate> candidateList = new ArrayList<IceCandidate>();

	public UserSession(WebSocketSession session, String name) {
		this.session = session;
		this.name = name;
	}

	public WebSocketSession getSession() {
		return session;
	}

	public String getName() {
		return name;
	}

	public String getSdpOffer() {
		return sdpOffer;
	}

	public void setSdpOffer(String sdpOffer) {
		this.sdpOffer = sdpOffer;
	}

	public String getCallingTo() {
		return callingTo;
	}

	public void setCallingTo(String callingTo) {
		this.callingTo = callingTo;
	}

	public String getCallingFrom() {
		return callingFrom;
	}

	public void setCallingFrom(String callingFrom) {
		this.callingFrom = callingFrom;
	}

	public void sendMessage(JsonObject message) throws IOException {
		log.debug("Sending message from user '{}': {}", name, message);
		session.sendMessage(new TextMessage(message.toString()));
	}

	public String getSessionId() {
		return session.getId();
	}

	public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
		this.webRtcEndpoint = webRtcEndpoint;

		if (this.webRtcEndpoint != null) {
			for (IceCandidate e : candidateList) {
				this.webRtcEndpoint.addIceCandidate(e);
			}
			this.candidateList.clear();
		}
	}

	public void addCandidate(IceCandidate e) {
		if (this.webRtcEndpoint != null) {
			this.webRtcEndpoint.addIceCandidate(e);
		} else {
			candidateList.add(e);
		}

		if (this.playingWebRtcEndpoint != null) {
			this.playingWebRtcEndpoint.addIceCandidate(e);
		}
	}

	public WebRtcEndpoint getPlayingWebRtcEndpoint() {
		return playingWebRtcEndpoint;
	}

	public void setPlayingWebRtcEndpoint(WebRtcEndpoint playingWebRtcEndpoint) {
		this.playingWebRtcEndpoint = playingWebRtcEndpoint;
	}

	public void clear() {
		this.webRtcEndpoint = null;
		this.candidateList.clear();
	}
}
