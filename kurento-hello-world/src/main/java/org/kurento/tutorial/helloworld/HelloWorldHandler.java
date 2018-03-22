/*
 * Copyright 2018 Kurento (https://www.kurento.org)
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

package org.kurento.tutorial.helloworld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// Kurento client
import org.kurento.client.BaseRtpEndpoint;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;

// Kurento events
import org.kurento.client.ConnectionStateChangedEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.IceComponentStateChangeEvent;
import org.kurento.client.IceGatheringDoneEvent;
import org.kurento.client.MediaFlowInStateChangeEvent;
import org.kurento.client.MediaFlowOutStateChangeEvent;
import org.kurento.client.MediaStateChangedEvent;
import org.kurento.client.MediaTranscodingStateChangeEvent;
import org.kurento.client.NewCandidatePairSelectedEvent;


/**
 * Kurento Java Tutorial - Handler class.
 */
public class HelloWorldHandler extends TextWebSocketHandler
{
  @Autowired
  private KurentoClient kurento;

  private final Logger log = LoggerFactory.getLogger(HelloWorldHandler.class);
  private final Gson gson = new GsonBuilder().create();
  private final ConcurrentHashMap<String, UserSession> users =
      new ConcurrentHashMap<>();

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message)
      throws Exception
  {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
    String sessionId = session.getId();

    log.debug("[Handler::handleTextMessage] {}, sessionId: {}",
        jsonMessage, sessionId);

    try {
      String messageId = jsonMessage.get("id").getAsString();
      switch (messageId) {
        case "start":
          start(session, jsonMessage);
          break;
        case "stop": {
          stop(session);
          break;
        }
        case "onIceCandidate":
          onRemoteIceCandidate(session, jsonMessage);
          break;
        default:
          sendError(session, "Invalid message, ID: " + messageId);
          break;
      }
    } catch (Throwable ex) {
      log.error("[Handler::handleTextMessage] Exception: {}, sessionId: {}",
          ex, sessionId);
      sendError(session, "Exception: " + ex.getMessage());
    }
  }

  private void addCommonEventListeners(final WebSocketSession session,
      BaseRtpEndpoint ep, final String name)
  {
    // Event: Some error happened
    ep.addErrorListener(new EventListener<ErrorEvent>() {
      @Override
      public void onEvent(ErrorEvent ev) {
        log.error("[{}::{}] source: {}, timestamp: {}, tags: {}, description: {}, errorCode: {}",
            name, ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getDescription(), ev.getErrorCode());
        stop(session);
      }
    });

    // Event: Media is flowing into this sink
    ep.addMediaFlowInStateChangeListener(
        new EventListener<MediaFlowInStateChangeEvent>() {
      @Override
      public void onEvent(MediaFlowInStateChangeEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
            name, ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getState(), ev.getPadName(), ev.getMediaType());
      }
    });

    // Event: Media is flowing out of this source
    ep.addMediaFlowOutStateChangeListener(
        new EventListener<MediaFlowOutStateChangeEvent>() {
      @Override
      public void onEvent(MediaFlowOutStateChangeEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
            name, ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getState(), ev.getPadName(), ev.getMediaType());
      }
    });

    // Event: [TODO write meaning of this event]
    ep.addConnectionStateChangedListener(
        new EventListener<ConnectionStateChangedEvent>() {
      @Override
      public void onEvent(ConnectionStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
            name, ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getOldState(), ev.getNewState());
      }
    });

    // Event: [TODO write meaning of this event]
    ep.addMediaStateChangedListener(
        new EventListener<MediaStateChangedEvent>() {
      @Override
      public void onEvent(MediaStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
            name, ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getOldState(), ev.getNewState());
      }
    });

    // Event: This element will (or will not) perform media transcoding
    ep.addMediaTranscodingStateChangeListener(
        new EventListener<MediaTranscodingStateChangeEvent>() {
      @Override
      public void onEvent(MediaTranscodingStateChangeEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, binName: {}, mediaType: {}",
            name, ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getState(), ev.getBinName(), ev.getMediaType());
      }
    });
  }

  /*
  WebRtcEndpoint configuration.
  Controls the connection between the browser and KMS.
  */
  private void startWebRtcEndpoint(final WebSocketSession session,
      WebRtcEndpoint webRtcEp, String webrtcSdpOffer)
  {
    log.info("[Handler::startWebRtcEndpoint] Configure now");

    addCommonEventListeners(session, webRtcEp, "WebRtcEndpoint");

    // Event: The ICE backend found a local candidate during Trickle ICE
    webRtcEp.addIceCandidateFoundListener(
        new EventListener<IceCandidateFoundEvent>() {
      @Override
      public void onEvent(IceCandidateFoundEvent ev) {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, candidate: {}",
            ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            JsonUtils.toJsonObject(ev.getCandidate()));

        // Update the UI
        {
          JsonObject message = new JsonObject();
          message.addProperty("id", "iceCandidate");
          message.add("candidate", JsonUtils.toJsonObject(ev.getCandidate()));
          sendMessage(session, message.toString());
        }
      }
    });

    // Event: The ICE backend changed state
    webRtcEp.addIceComponentStateChangeListener(
        new EventListener<IceComponentStateChangeEvent>() {
      @Override
      public void onEvent(IceComponentStateChangeEvent ev) {
        log.info("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, streamId: {}, componentId: {}, state: {}",
            ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getStreamId(), ev.getComponentId(), ev.getState());
      }
    });

    // Event: The ICE backend finished gathering ICE candidates
    webRtcEp.addIceGatheringDoneListener(
        new EventListener<IceGatheringDoneEvent>() {
      @Override
      public void onEvent(IceGatheringDoneEvent ev) {
        log.info("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}",
            ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags());
      }
    });

    // Event: The ICE backend selected a new pair of ICE candidates for use
    webRtcEp.addNewCandidatePairSelectedListener(
        new EventListener<NewCandidatePairSelectedEvent>() {
      @Override
      public void onEvent(NewCandidatePairSelectedEvent ev) {
        log.info("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, streamId: {}, local: {}, remote: {}",
            ev.getType(), ev.getSource(), ev.getTimestamp(), ev.getTags(),
            ev.getCandidatePair().getStreamID(),
            ev.getCandidatePair().getLocalCandidate(),
            ev.getCandidatePair().getRemoteCandidate());
      }
    });

    /*
    OPTIONAL: Force usage of an Application-specific STUN server.
    Usually this is configured globally in KMS WebRTC settings file:
    /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini

    But it can also be configured per-application, as shown:

    log.info("[Handler::startWebRtcEndpoint] Using STUN server: 193.147.51.12:3478");
    webRtcEp.setStunServerAddress("193.147.51.12");
    webRtcEp.setStunServerPort(3478);
    */

    // 'webrtcSdpOffer' is the SDP Offer generated by the browser;
    // send the SDP Offer to KMS, and get back its SDP Answer
    String webrtcSdpAnswer = webRtcEp.processOffer(webrtcSdpOffer);

    log.info("[Handler::startWebRtcEndpoint] SDP Offer from browser to KMS:\n{}",
        webrtcSdpOffer);
    log.info("[Handler::startWebRtcEndpoint] SDP Answer from KMS to browser:\n{}",
        webrtcSdpAnswer);

    // Update the UI
    {
      JsonObject message = new JsonObject();
      message.addProperty("id", "startResponse");
      message.addProperty("sdpAnswer", webrtcSdpAnswer);
      sendMessage(session, message.toString());
    }

    webRtcEp.gatherCandidates();
  }

  private void start(final WebSocketSession session, JsonObject jsonMessage)
  {
    // ---- Media pipeline

    log.info("[Handler::start] Create Media Pipeline");

    final UserSession user = new UserSession();
    users.put(session.getId(), user);

    final MediaPipeline pipeline = kurento.createMediaPipeline();
    user.setMediaPipeline(pipeline);

    final WebRtcEndpoint webRtcEp =
        new WebRtcEndpoint.Builder(pipeline).build();
    user.setWebRtcEndpoint(webRtcEp);

    webRtcEp.connect(webRtcEp);


    // ---- Endpoint configuration

    String webrtcSdpOffer = jsonMessage.get("sdpOffer").getAsString();
    startWebRtcEndpoint(session, webRtcEp, webrtcSdpOffer);


    // ---- Debug

    String pipelineDot = pipeline.getGstreamerDot();
    try (PrintWriter out = new PrintWriter("pipeline.dot")) {
      out.println(pipelineDot);
    } catch (IOException ex) {
      log.error("[Handler::start] Exception: {}", ex.getMessage());
    }
  }

  private void stop(final WebSocketSession session)
  {
    // Update the UI
    sendPlayEnd(session);

    // Remove the user session and release all resources
    String sessionId = session.getId();
    UserSession user = users.remove(sessionId);
    if (user != null) {
      MediaPipeline mediaPipeline = user.getMediaPipeline();
      if (mediaPipeline != null) {
        log.info("[Handler::stop] Release the Media Pipeline");
        mediaPipeline.release();
      }
    }
  }

  private void onRemoteIceCandidate(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    String sessionId = session.getId();
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

  public void sendPlayEnd(final WebSocketSession session)
  {
    if (users.containsKey(session.getId())) {
      JsonObject message = new JsonObject();
      message.addProperty("id", "playEnd");
      sendMessage(session, message.toString());
    }
  }

  private void sendError(final WebSocketSession session, String errorMsg)
  {
    if (users.containsKey(session.getId())) {
      JsonObject message = new JsonObject();
      message.addProperty("id", "error");
      message.addProperty("message", errorMsg);
      sendMessage(session, message.toString());
    }
  }

  private synchronized void sendMessage(final WebSocketSession session,
      String message)
  {
    try {
      session.sendMessage(new TextMessage(message));
    } catch (IOException ex) {
      log.error("[Handler::sendMessage] Exception: {}", ex.getMessage());
    }
  }

  @Override
  public void afterConnectionClosed(final WebSocketSession session,
      CloseStatus status) throws Exception
  {
    stop(session);
  }
}
