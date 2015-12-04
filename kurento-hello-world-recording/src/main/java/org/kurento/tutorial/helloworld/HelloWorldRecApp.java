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

import static org.kurento.commons.PropertiesManager.getProperty;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.kurento.client.KurentoClient;
import org.kurento.commons.PropertiesManager;
import org.kurento.repository.RepositoryClient;
import org.kurento.repository.RepositoryClientProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@Configuration
@EnableWebSocket
@EnableAutoConfiguration
public class HelloWorldRecApp implements WebSocketConfigurer {

  private static final String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";
  static final String DEFAULT_REPOSITORY_SERVER_URI = "http://localhost:7676";

  static final String REPOSITORY_SERVER_URI =
      System.getProperty("repository.uri", DEFAULT_REPOSITORY_SERVER_URI);
  static final String KMS_WS_URI = System.getProperty("kms.ws.uri", DEFAULT_KMS_WS_URI);

  private static final int SECURE_PORT = getProperty("ws.secureport", 8443);
  private static final int PLAIN_PORT = getProperty("ws.port", 8080);

  @Bean
  public EmbeddedServletContainerFactory servletContainer() {
    TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory() {
      @Override
      protected void postProcessContext(Context context) {
        SecurityConstraint securityConstraint = new SecurityConstraint();
        securityConstraint.setUserConstraint("CONFIDENTIAL");
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        securityConstraint.addCollection(collection);
        context.addConstraint(securityConstraint);
      }
    };

    tomcat.addAdditionalTomcatConnectors(initiateHttpConnector());
    return tomcat;
  }

  private Connector initiateHttpConnector() {
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setScheme("http");
    connector.setPort(PLAIN_PORT);
    connector.setSecure(false);
    connector.setRedirectPort(SECURE_PORT);

    return connector;
  }

  @Bean
  public HelloWorldRecHandler handler() {
    return new HelloWorldRecHandler();
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create(PropertiesManager.getProperty("kms.ws.uri", KMS_WS_URI));
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler(), "/helloworld");
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
