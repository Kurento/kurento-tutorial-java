/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package org.kurento.tutorial.helloworld;

import org.kurento.client.KurentoClient;
import org.kurento.repository.RepositoryClient;
import org.kurento.repository.RepositoryClientProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Hello World (WebRTC in loopback with recording) main class.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @since 6.1.1
 */
@SpringBootApplication
@EnableWebSocket
public class HelloWorldRecApp implements WebSocketConfigurer {

  static final String DEFAULT_REPOSITORY_SERVER_URI = "http://localhost:7676";

  static final String REPOSITORY_SERVER_URI = System.getProperty("repository.uri",
      DEFAULT_REPOSITORY_SERVER_URI);

  @Bean
  public HelloWorldRecHandler handler() {
    return new HelloWorldRecHandler();
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler(), "/helloworld").setAllowedOrigins("*");
  }

  @Bean
  public RepositoryClient repositoryServiceProvider() {
    if (REPOSITORY_SERVER_URI.startsWith("file://")) {
      return null;
    }
    return RepositoryClientProvider.create(REPOSITORY_SERVER_URI);
  }

  @Bean
  public UserRegistry registry() {
    return new UserRegistry();
  }

  public static void main(String[] args) throws Exception {
    new SpringApplication(HelloWorldRecApp.class).run(args);
  }
}
