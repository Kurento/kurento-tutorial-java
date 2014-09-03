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

	private WebSocketSession session;
	private String sdpOffer;
	private WebRtcEndpoint webRtcEndpoint;

	public UserSession(WebSocketSession session, String sdpOffer) {
		this.session = session;
		this.sdpOffer = sdpOffer;
	}

	public WebSocketSession getSession() {
		return session;
	}

	public String getSdpOffer() {
		return sdpOffer;
	}

	public void setSdpOffer(String sdpOffer) {
		this.sdpOffer = sdpOffer;
	}

	public void sendMessage(JsonObject message) throws IOException {
		log.debug("Sending message from user with session Id '{}': {}",
				session.getId(), message);
		session.sendMessage(new TextMessage(message.toString()));
	}

	@Override
	public boolean equals(Object obj) {
		return session.getId().equals(((UserSession) obj).getSession().getId());
	}

	public WebRtcEndpoint getWebRtcEndpoint() {
		return webRtcEndpoint;
	}

	public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
		this.webRtcEndpoint = webRtcEndpoint;
	}
}
