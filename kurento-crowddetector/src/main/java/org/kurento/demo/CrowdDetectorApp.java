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

package org.kurento.demo;

import static org.kurento.commons.PropertiesManager.getProperty;

import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.kurento.client.KurentoClient;
import org.kurento.orion.OrionConnector;
import org.kurento.orion.OrionConnectorConfiguration;
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
 * CrowdDetector with RTSP media source (application and media logic).
 *
 * @author David Fernández (d.fernandezlop@gmail.com)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 5.0.4
 */
@Configuration
@EnableWebSocket
@EnableAutoConfiguration
public class CrowdDetectorApp implements WebSocketConfigurer {

  private static final String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";
  static final String DEFAULT_CONFIG_FILE_PATH = "/config/configuration.conf.json";

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
  public ConfigurationReader init() throws IOException {
    return new ConfigurationReader(System.getProperty("app.configFile", DEFAULT_CONFIG_FILE_PATH));
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create(System.getProperty("kms.ws.uri", DEFAULT_KMS_WS_URI));
  }

  @Bean
  public OrionConnector orionConnector() {
    return new OrionConnector();
  }

  @Bean
  public OrionConnectorConfiguration orionConnectorConfiguration() {
    return new OrionConnectorConfiguration();
  }

  @Bean
  public CrowdDetectorOrionPublisher crowdDetectorOrionPublisher() {
    return new CrowdDetectorOrionPublisher();
  }

  @Bean
  public Pipeline pipeline() {
    return new Pipeline();
  }

  @Bean
  public CrowdDetectorHandler handler() {
    return new CrowdDetectorHandler();
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler(), "/crowddetector");
  }

  public static void main(String[] args) throws Exception {
    new SpringApplication(CrowdDetectorApp.class).run(args);
  }
}
