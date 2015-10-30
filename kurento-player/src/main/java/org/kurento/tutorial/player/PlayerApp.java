/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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
package org.kurento.tutorial.player;

import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Play of a video through WebRTC (main).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
@EnableWebSocket
@SpringBootApplication
public class PlayerApp implements WebSocketConfigurer {

	final static String KMS_WS_URI_PROP = "kms.ws.uri";
	final static String KMS_WS_URI_DEFAULT = "ws://localhost:8888/kurento";

	@Bean
	public PlayerHandler handler() {
		return new PlayerHandler();
	}

	@Bean
	public KurentoClient kurentoClient() {
		return KurentoClient.create(
				System.getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT));
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(handler(), "/player");
	}

	public static void main(String[] args) throws Exception {
		new SpringApplication(PlayerApp.class).run(args);
	}
}
