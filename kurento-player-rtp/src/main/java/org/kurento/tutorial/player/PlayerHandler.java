/*
 * Copyright 2017 Kurento (https://www.kurento.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kurento.tutorial.player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

// Events
import org.kurento.client.ErrorEvent;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.IceComponentStateChangeEvent;
import org.kurento.client.IceGatheringDoneEvent;
import org.kurento.client.NewCandidatePairSelectedEvent;

import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Protocol handler for video player through RTP.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (dfernandezlop@gmail.com)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.1.1
 */
public class PlayerHandler extends TextWebSocketHandler {

  @Autowired
  private KurentoClient kurento;

  private final Logger log = LoggerFactory.getLogger(PlayerHandler.class);
  private final Gson gson = new GsonBuilder().create();
  private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
    String sessionId = session.getId();
    log.debug("[PlayerHandler::handleTextMessage] {}, sessionId: {}", jsonMessage, sessionId);

    try {
      switch (jsonMessage.get("id").getAsString()) {
        case "start":
          String browserSdpOffer = jsonMessage.get("sdpOffer").getAsString();
          start(session, browserSdpOffer);
          break;
        case "stop":
          stop(sessionId);
          break;
        case "onIceCandidate":
          onRemoteIceCandidate(sessionId, jsonMessage);
          break;
        default:
          sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
          break;
      }
    } catch (Throwable t) {
      log.error("[PlayerHandler::handleTextMessage] Exception handling {}, sessionId: {}", jsonMessage, sessionId, t);
      sendError(session, t.getMessage());
    }
  }

  private void start(final WebSocketSession session, String browserSdpOffer)
  {
    /*
    1. Media pipeline
    Create an object which controls the KMS pipeline through RPC.
    */

    UserSession user = new UserSession();
    MediaPipeline pipeline = kurento.createMediaPipeline();
    user.setMediaPipeline(pipeline);

    WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
    user.setWebRtcEndpoint(webRtcEndpoint);

    RtpEndpoint rtpEndpoint = new RtpEndpoint.Builder(pipeline).build();
    user.setRtpEndpoint(rtpEndpoint);

    users.put(session.getId(), user);
    rtpEndpoint.connect(webRtcEndpoint);


    /*
    2. WebRtcEndpoint
    Controls the connection between the browser and KMS.
    */

    // Process each candidate generated during Trickle ICE
    webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
      @Override
      public void onEvent(IceCandidateFoundEvent event) {
        log.debug("[WebRtcEndpoint::IceCandidateFound] {}", JsonUtils.toJsonObject(event.getCandidate()));
        JsonObject response = new JsonObject();
        response.addProperty("id", "iceCandidate");
        response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
        try {
          synchronized (session) {
            session.sendMessage(new TextMessage(response.toString()));
          }
        } catch (Exception e) {
          log.error("[PlayerHandler::onIceCandidateFoundEvent] Exception: {}", e.getMessage());
        }
      }
    });

    webRtcEndpoint.addIceComponentStateChangeListener(new EventListener<IceComponentStateChangeEvent>() {
      @Override
      public void onEvent(IceComponentStateChangeEvent event) {
        log.debug(
            "[WebRtcEndpoint::IceComponentStateChange] Source: {}, Type: {}, StreamId: {}, Component: {}, State: {}",
            event.getSource(), event.getType(), event.getStreamId(), event.getComponentId(), event.getState());
      }
    });

    webRtcEndpoint.addIceGatheringDoneListener(new EventListener<IceGatheringDoneEvent>() {
      @Override
      public void onEvent(IceGatheringDoneEvent event) {
        log.debug("[WebRtcEndpoint::IceGatheringDone]");
      }
    });

    webRtcEndpoint.addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {
      @Override
      public void onEvent(NewCandidatePairSelectedEvent event) {
        log.debug("[WebRtcEndpoint::NewCandidatePairSelected] Local: {}, Remote: {}, StreamId: {}",
            event.getCandidatePair().getLocalCandidate(), event.getCandidatePair().getRemoteCandidate(),
            event.getCandidatePair().getStreamID());
      }
    });

    /*
    Optional: Force usage of an Application-specific STUN server.
    Usually this is configured globally in KMS WebRTC settings file:
    /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
    */
    // log.debug("[PlayerHandler::start] Using STUN server: 193.147.51.12:3478");
    // webRtcEndpoint.setStunServerAddress("193.147.51.12");
    // webRtcEndpoint.setStunServerPort(3478);

    String browserSdpAnswer = webRtcEndpoint.processOffer(browserSdpOffer);

    log.info("[PlayerHandler::start]\nSDP Offer from browser to KMS:\n{}", browserSdpOffer);
    log.info("[PlayerHandler::start]\nSDP Answer from KMS to browser:\n{}", browserSdpAnswer);

    {
      JsonObject response = new JsonObject();
      response.addProperty("id", "startResponse");
      response.addProperty("sdpAnswer", browserSdpAnswer);
      sendMessage(session, response.toString());
    }

    webRtcEndpoint.gatherCandidates();


    /*
    3. RtpEndpoint
    Controls the SDP Offer/Answer negotiation between a 3rd-party RTP sender
    and KMS. A fake SDP Offer simulates the features that our RTP sender
    will have; then, the SDP Answer is parsed to extract the RTP and RTCP ports
    where KMS will listen.
    */

    // >>>> CONFIGURATION
    // Set the appropriate values for your setup
    boolean useComedia = true;
    String senderIp = "127.0.0.1";
    int senderRtpPortA = 5006;
    int senderRtpPortV = 5004;
    int senderSsrcA = 445566;
    int senderSsrcV = 112233;
    String senderCname = "user@example.com";
    String senderCodecV = "H264";
    // String senderCodecV = "VP8";
    // <<<< CONFIGURATION

    if (useComedia) {
      // Use Discard port (9)
      senderRtpPortA = 9;
      senderRtpPortV = 9;
    }

    rtpEndpoint.addErrorListener(new EventListener<ErrorEvent>() {
      @Override
      public void onEvent(ErrorEvent event) {
        log.info("[RtpEndpoint] ErrorEvent: {}", event.getDescription());
        sendPlayEnd(session);
      }
    });

    // Set maximum bandwidth on reception, for Skylight tests
    rtpEndpoint.setMaxVideoRecvBandwidth(1049); // In kbps = 1000 bps

    String sdpComediaStr = "";
    if (useComedia) {
      sdpComediaStr = "a=direction:active\r\n"; // Inspired by RFC 4145 Draft 05 ("COMEDIA")
    }

/*
# SDP quick reference
SDP structure is composed of levels: session > media > source.
Each level can contain one or more of the next ones.
Typically, one session contains several medias, and each media contains one source.

---- Session-level information ----
v=
o=
s=
c=
t=
---- Media-level attributes ----
m=
a=
---- Source-level attributes ----
a=ssrc

Some default values are defined by different RFCs:
- RFC 3264 defines recommended values for "s=", "t=", "a=sendonly".
- RFC 5576 defines source-level attribute "a=ssrc".
*/

    String fakeSdpOffer =
      "v=0\r\n"
      + "o=- 0 0 IN IP4 " + senderIp + "\r\n"
      + "s=-\r\n"
      + "c=IN IP4 " + senderIp + "\r\n"
      + "t=0 0\r\n"
      + "m=audio " + senderRtpPortA + " RTP/AVPF 96\r\n"
      + "a=rtpmap:96 opus/48000/2\r\n"
      + "a=sendonly\r\n"
      + sdpComediaStr
      + "a=ssrc:" + senderSsrcA + " cname:" + senderCname + "\r\n"
      + "m=video " + senderRtpPortV + " RTP/AVPF 103\r\n"
      + "a=rtpmap:103 " + senderCodecV + "/90000\r\n"
      // No REMB support in the gst-launch command
      //+ "a=rtcp-fb:103 goog-remb\r\n"
      + "a=sendonly\r\n"
      + sdpComediaStr
      + "a=ssrc:" + senderSsrcV + " cname:" + senderCname + "\r\n"
      + "";
    log.info("[PlayerHandler::start]\nFake SDP Offer from App to KMS:\n{}", fakeSdpOffer);

    // Get SDP Answer from KMS and mangle it with custom values for the camera
    String kmsSdpAnswer = rtpEndpoint.processOffer(fakeSdpOffer);
    kmsSdpAnswer += ""
      + "a=x-skl-ssrca:" + senderSsrcA + "\r\n"
      + "a=x-skl-ssrcv:" + senderSsrcV + "\r\n"
      + "a=x-skl-cname:" + senderCname + "\r\n";

    log.info("[PlayerHandler::start]\nSDP Answer from KMS to App:\n{}", kmsSdpAnswer);

    // Parse SDP Answer
    // No error checking: assume that the SDP Answer from KMS is well formed...
    Pattern p; Matcher m;

    p = Pattern.compile("m=audio (\\d+) RTP");
    m = p.matcher(kmsSdpAnswer);
    m.find();
    int kmsRtpPortA = Integer.parseInt(m.group(1));
    int senderRtcpPortA = senderRtpPortA + 1;

    p = Pattern.compile("m=video (\\d+) RTP");
    m = p.matcher(kmsSdpAnswer);
    m.find();
    int kmsRtpPortV = Integer.parseInt(m.group(1));
    int senderRtcpPortV = senderRtpPortV + 1;

    p = Pattern.compile("c=IN IP4 (([0-9]{1,3}\\.){3}[0-9]{1,3})");
    m = p.matcher(kmsSdpAnswer);
    m.find();
    String kmsIp = m.group(1);

    // Check if KMS accepted the use of "direction" attribute
    useComedia = kmsSdpAnswer.contains("a=direction:passive");

    String msgRtcp;
    if (useComedia) {
      msgRtcp = "* KMS will discover remote IP and port to send RTCP";
    } else {
      msgRtcp = String.format(
        "* KMS sends Audio RTCP to: %s:%d\n"
        + "* KMS sends Video RTCP to: %s:%d\n",
        senderIp, senderRtcpPortA,
        senderIp, senderRtcpPortV);
    }

    String msgPortInfo = String.format(
      "SDP negotiation finished\n"
      + "* KMS local IP address: %s\n"
      + "* KMS listens for Audio RTP at port: %d\n"
      + "* KMS listens for Video RTP at port: %d\n"
      + "* KMS expects Audio SSRC: %d\n"
      + "* KMS expects Video SSRC: %d\n"
      + msgRtcp,
      kmsIp, kmsRtpPortA, kmsRtpPortV,
      senderSsrcA, senderSsrcV);

    log.info("\n [PlayerHandler::start] " + msgPortInfo);

    // Send info to web UI
    {
      JsonObject response = new JsonObject();
      response.addProperty("id", "msgPortInfo");
      response.addProperty("text", msgPortInfo);
      try {
        synchronized (session) {
          session.sendMessage(new TextMessage(response.toString()));
        }
      } catch (Exception e) {
        log.error("[PlayerHandler::start] sendMessage exception: {}", e.getMessage());
      }

      response = new JsonObject();
      response.addProperty("id", "msgSdpText");
      response.addProperty("text", kmsSdpAnswer);
      try {
        synchronized (session) {
          session.sendMessage(new TextMessage(response.toString()));
        }
      } catch (Exception e) {
        log.error("[PlayerHandler::start] Exception: {}", e.getMessage());
      }
    }
  }

  private void stop(String sessionId) {
    UserSession user = users.remove(sessionId);

    if (user != null) {
      user.release();
    }
  }

  private void onRemoteIceCandidate(String sessionId, JsonObject jsonMessage) {
    UserSession user = users.get(sessionId);

    if (user != null) {
      JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();
      IceCandidate candidate =
        new IceCandidate(jsonCandidate.get("candidate").getAsString(),
        jsonCandidate.get("sdpMid").getAsString(),
        jsonCandidate.get("sdpMLineIndex").getAsInt());
      user.getWebRtcEndpoint().addIceCandidate(candidate);
    }
  }

  public void sendPlayEnd(WebSocketSession session) {
    if (users.containsKey(session.getId())) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "playEnd");
      sendMessage(session, response.toString());
    }
  }

  private void sendError(WebSocketSession session, String message) {
    if (users.containsKey(session.getId())) {
      JsonObject response = new JsonObject();
      response.addProperty("id", "error");
      response.addProperty("message", message);
      sendMessage(session, response.toString());
    }
  }

  private synchronized void sendMessage(WebSocketSession session, String message) {
    try {
      session.sendMessage(new TextMessage(message));
    } catch (IOException e) {
      log.error("[PlayerHandler::sendMessage] Exception: {}", e.getMessage());
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    stop(session.getId());
  }
}
