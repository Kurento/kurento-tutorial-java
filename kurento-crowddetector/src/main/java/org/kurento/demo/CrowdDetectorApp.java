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

import java.io.IOException;

import org.kurento.client.KurentoClient;
import org.kurento.orion.OrionConnector;
import org.kurento.orion.OrionConnectorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
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
@SpringBootApplication
@EnableWebSocket
public class CrowdDetectorApp implements WebSocketConfigurer {

  static final String DEFAULT_CONFIG_FILE_PATH = "/config/configuration.conf.json";

  @Bean
  public ConfigurationReader init() throws IOException {
    return new ConfigurationReader(System.getProperty("app.configFile", DEFAULT_CONFIG_FILE_PATH));
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
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
    registry.addHandler(handler(), "/crowddetector").setAllowedOrigins("*");
  }

  public static void main(String[] args) throws Exception {
    new SpringApplication(CrowdDetectorApp.class).run(args);
  }
}
