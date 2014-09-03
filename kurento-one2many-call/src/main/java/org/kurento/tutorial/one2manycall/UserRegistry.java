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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.web.socket.WebSocketSession;

/**
 * Stack of users registered in the system.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class UserRegistry {

	LinkedBlockingDeque<UserSession> userStack = new LinkedBlockingDeque<UserSession>();
	private ConcurrentHashMap<String, UserSession> usersBySessionId = new ConcurrentHashMap<String, UserSession>();

	public void add(UserSession userSession) {
		userStack.add(userSession);
		usersBySessionId.put(userSession.getSession().getId(), userSession);
	}

	public void remove(WebSocketSession session) {
		String sessionId = session.getId();
		UserSession userSession = usersBySessionId.get(sessionId);
		userStack.remove(userSession);
		usersBySessionId.remove(sessionId);
	}

	public UserSession getByUserSession(WebSocketSession session) {
		return usersBySessionId.get(session.getId());
	}

	public UserSession getFirst() {
		UserSession first = null;
		if (!userStack.isEmpty()) {
			first = userStack.getFirst();
		}
		return first;
	}

	public UserSession getLast() {
		UserSession last = null;
		if (!userStack.isEmpty()) {
			last = userStack.getLast();
		}
		return last;
	}

	public boolean isEmpty() {
		return userStack.isEmpty();
	}

}
