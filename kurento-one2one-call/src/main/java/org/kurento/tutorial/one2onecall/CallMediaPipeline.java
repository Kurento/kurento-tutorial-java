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

package org.kurento.tutorial.one2onecall;

import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;

/**
 * Media Pipeline (WebRTC endpoints, i.e. Kurento Media Elements) and connections for the 1 to 1
 * video communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 4.3.1
 */
public class CallMediaPipeline {

  private MediaPipeline pipeline;
  private WebRtcEndpoint callerWebRtcEp;
  private WebRtcEndpoint calleeWebRtcEp;

  public CallMediaPipeline(KurentoClient kurento) {
    try {
      this.pipeline = kurento.createMediaPipeline();
      this.callerWebRtcEp = new WebRtcEndpoint.Builder(pipeline).build();
      this.calleeWebRtcEp = new WebRtcEndpoint.Builder(pipeline).build();

      this.callerWebRtcEp.connect(this.calleeWebRtcEp);
      this.calleeWebRtcEp.connect(this.callerWebRtcEp);
    } catch (Throwable t) {
      if (this.pipeline != null) {
        pipeline.release();
      }
    }
  }

  public String generateSdpAnswerForCaller(String sdpOffer) {
    return callerWebRtcEp.processOffer(sdpOffer);
  }

  public String generateSdpAnswerForCallee(String sdpOffer) {
    return calleeWebRtcEp.processOffer(sdpOffer);
  }

  public void release() {
    if (pipeline != null) {
      pipeline.release();
    }
  }

  public WebRtcEndpoint getCallerWebRtcEp() {
    return callerWebRtcEp;
  }

  public WebRtcEndpoint getCalleeWebRtcEp() {
    return calleeWebRtcEp;
  }

}
