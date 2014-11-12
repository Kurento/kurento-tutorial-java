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

import org.kurento.module.markerdetector.ArMarkerdetector;
import org.springframework.web.socket.WebSocketSession;

/**
 * Sub-class of user session to store marker detector filter.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @since 5.0.4
 */
public class MasterUserSession extends UserSession {

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
