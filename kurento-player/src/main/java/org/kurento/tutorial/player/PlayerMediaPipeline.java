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

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.OnIceCandidateEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;

/**
 * Media pipeline per user.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 6.1.1
 */
public class PlayerMediaPipeline {

  private static final String PLAY_URL_PROP = "demo.play.url";

  private WebRtcEndpoint webRtcEndpoint;
  private MediaPipeline mediaPipeline;
  private PlayerEndpoint playerEndpoint;

  public void initMediaPipeline(KurentoClient kurento, String videourl) {
    mediaPipeline = kurento.createMediaPipeline();
    webRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
    playerEndpoint =
        new PlayerEndpoint.Builder(mediaPipeline, System.getProperty(PLAY_URL_PROP, videourl))
            .build();
    playerEndpoint.connect(webRtcEndpoint);
  }

  public void gatherCandidates(EventListener<OnIceCandidateEvent> eventListener) {
    webRtcEndpoint.addOnIceCandidateListener(eventListener);
    webRtcEndpoint.gatherCandidates();
  }

  public String processOffer(String sdpOffer) {
    return webRtcEndpoint.processOffer(sdpOffer);
  }

  public void play(EventListener<ErrorEvent> error, EventListener<EndOfStreamEvent> eos) {
    playerEndpoint.addErrorListener(error);
    playerEndpoint.addEndOfStreamListener(eos);
    playerEndpoint.play();

  }

  public void play() {
    playerEndpoint.play();
  }

  public void pause() {
    playerEndpoint.pause();
  }

  public void release() {
    playerEndpoint.release();
  }

  public void addIceCandidate(IceCandidate candidate) {
    webRtcEndpoint.addIceCandidate(candidate);
  }

}
