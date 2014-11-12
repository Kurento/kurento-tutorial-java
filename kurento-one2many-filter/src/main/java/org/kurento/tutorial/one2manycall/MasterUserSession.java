package org.kurento.tutorial.one2manycall;

import org.kurento.module.markerdetector.ArMarkerdetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

public class MasterUserSession extends UserSession{

	private static final Logger log = LoggerFactory
			.getLogger(MasterUserSession.class);
	
	private ArMarkerdetector arMarkerdetector;
	
	public MasterUserSession(WebSocketSession session) {
		super(session);
	}

	public ArMarkerdetector getArMarkerdetector() {
		return arMarkerdetector;
	}

	public void setArMarkerdetector(ArMarkerdetector arMarkerdetector) {
		this.arMarkerdetector = arMarkerdetector;
	}

}
