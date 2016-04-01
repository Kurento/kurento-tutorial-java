/*
 * (C) Copyright 2015-2016 Kurento (http://kurento.org/)
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

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.StoppedEvent;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

/**
 * User session.
 *
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.1.1
 */
public class UserSession {

  private final Logger log = LoggerFactory.getLogger(UserSession.class);

  private String id;
  private WebRtcEndpoint webRtcEndpoint;
  private RecorderEndpoint recorderEndpoint;
  private MediaPipeline mediaPipeline;
  private Date stopTimestamp;

  public UserSession(WebSocketSession session) {
    this.id = session.getId();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public WebRtcEndpoint getWebRtcEndpoint() {
    return webRtcEndpoint;
  }

  public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
    this.webRtcEndpoint = webRtcEndpoint;
  }

  public void setRecorderEndpoint(RecorderEndpoint recorderEndpoint) {
    this.recorderEndpoint = recorderEndpoint;
  }

  public MediaPipeline getMediaPipeline() {
    return mediaPipeline;
  }

  public void setMediaPipeline(MediaPipeline mediaPipeline) {
    this.mediaPipeline = mediaPipeline;
  }

  public void addCandidate(IceCandidate candidate) {
    webRtcEndpoint.addIceCandidate(candidate);
  }

  public Date getStopTimestamp() {
    return stopTimestamp;
  }

  public void stop() {
    if (recorderEndpoint != null) {
      final CountDownLatch stoppedCountDown = new CountDownLatch(1);
      ListenerSubscription subscriptionId = recorderEndpoint
          .addStoppedListener(new EventListener<StoppedEvent>() {

            @Override
            public void onEvent(StoppedEvent event) {
              stoppedCountDown.countDown();
            }
          });
      recorderEndpoint.stop();
      try {
        if (!stoppedCountDown.await(5, TimeUnit.SECONDS)) {
          log.error("Error waiting for recorder to stop");
        }
      } catch (InterruptedException e) {
        log.error("Exception while waiting for state change", e);
      }
      recorderEndpoint.removeStoppedListener(subscriptionId);
    }
  }

  public void release() {
    this.mediaPipeline.release();
    this.webRtcEndpoint = null;
    this.mediaPipeline = null;
    if (this.stopTimestamp == null) {
      this.stopTimestamp = new Date();
    }
  }
}
