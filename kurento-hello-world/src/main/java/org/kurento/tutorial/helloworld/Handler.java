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
public class Handler extends TextWebSocketHandler
{
  private final Logger log = LoggerFactory.getLogger(Handler.class);
  private final Gson gson = new GsonBuilder().create();

  private final ConcurrentHashMap<String, UserSession> users =
      new ConcurrentHashMap<>();

  @Autowired
  private KurentoClient kurento;

  @Override
  public void afterConnectionClosed(final WebSocketSession session,
      CloseStatus status) throws Exception
  {
    log.debug("[Handler::afterConnectionClosed] status: {}, sessionId: {}",
        status, session.getId());

    stop(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session,
      TextMessage message) throws Exception
  {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(),
        JsonObject.class);
    String sessionId = session.getId();

    log.debug("[Handler::handleTextMessage] {}, sessionId: {}",
        jsonMessage, sessionId);

    try {
      String messageId = jsonMessage.get("id").getAsString();
      switch (messageId) {
        case "PROCESS_SDP_OFFER":
          handleProcessSdpOffer(session, jsonMessage);
          break;
        case "ADD_ICE_CANDIDATE":
          handleAddIceCandidate(session, jsonMessage);
          break;
        case "STOP":
          handleStop(session, jsonMessage);
          break;
        default:
          sendError(session, "Invalid message, id: " + messageId);
          break;
      }
    } catch (Throwable ex) {
      log.error("[Handler::handleTextMessage] Exception: {}, sessionId: {}",
          ex, sessionId);
      sendError(session, "Exception: " + ex.getMessage());
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable ex)
      throws Exception
  {
    log.error("[Handler::handleTransportError] Exception: {}, sessionId: {}",
        ex, session.getId());
  }

  // START ---------------------------------------------------------------------

  private void addBaseEventListeners(final WebSocketSession session,
      BaseRtpEndpoint baseRtpEp, final String className)
  {
    log.info("[Handler::addBaseEventListeners] name: {}, class: {}, sessionId: {}",
        baseRtpEp.getName(), className, session.getId());

    // Event: Some error happened
    baseRtpEp.addErrorListener(new EventListener<ErrorEvent>() {
      @Override
      public void onEvent(ErrorEvent ev) {
        log.error("[{}::{}] source: {}, timestamp: {}, tags: {}, description: {}, errorCode: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getDescription(), ev.getErrorCode());
        stop(session);
      }
    });

    // Event: Media is flowing into this sink
    baseRtpEp.addMediaFlowInStateChangeListener(
        new EventListener<MediaFlowInStateChangeEvent>() {
      @Override
      public void onEvent(MediaFlowInStateChangeEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
      }
    });

    // Event: Media is flowing out of this source
    baseRtpEp.addMediaFlowOutStateChangeListener(
        new EventListener<MediaFlowOutStateChangeEvent>() {
      @Override
      public void onEvent(MediaFlowOutStateChangeEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
      }
    });

    // Event: [TODO write meaning of this event]
    baseRtpEp.addConnectionStateChangedListener(
        new EventListener<ConnectionStateChangedEvent>() {
      @Override
      public void onEvent(ConnectionStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getOldState(), ev.getNewState());
      }
    });

    // Event: [TODO write meaning of this event]
    baseRtpEp.addMediaStateChangedListener(
        new EventListener<MediaStateChangedEvent>() {
      @Override
      public void onEvent(MediaStateChangedEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getOldState(), ev.getNewState());
      }
    });

    // Event: This element will (or will not) perform media transcoding
    baseRtpEp.addMediaTranscodingStateChangeListener(
        new EventListener<MediaTranscodingStateChangeEvent>() {
      @Override
      public void onEvent(MediaTranscodingStateChangeEvent ev) {
        log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, binName: {}, mediaType: {}",
            className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getState(), ev.getBinName(), ev.getMediaType());
      }
    });
  }

  private void addWebRtcEventListeners(final WebSocketSession session,
      final WebRtcEndpoint webRtcEp)
  {
    log.info("[Handler::addWebRtcEventListeners] name: {}, sessionId: {}",
        webRtcEp.getName(), session.getId());

    // Event: The ICE backend found a local candidate during Trickle ICE
    webRtcEp.addIceCandidateFoundListener(
        new EventListener<IceCandidateFoundEvent>() {
      @Override
      public void onEvent(IceCandidateFoundEvent ev) {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, candidate: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), JsonUtils.toJsonObject(ev.getCandidate()));

        JsonObject message = new JsonObject();
        message.addProperty("id", "ADD_ICE_CANDIDATE");
        message.addProperty("webRtcEpId", webRtcEp.getId());
        message.add("candidate", JsonUtils.toJsonObject(ev.getCandidate()));
        sendMessage(session, message.toString());
      }
    });

    // Event: The ICE backend changed state
    webRtcEp.addIceComponentStateChangeListener(
        new EventListener<IceComponentStateChangeEvent>() {
      @Override
      public void onEvent(IceComponentStateChangeEvent ev) {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, streamId: {}, componentId: {}, state: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getStreamId(), ev.getComponentId(), ev.getState());
      }
    });

    // Event: The ICE backend finished gathering ICE candidates
    webRtcEp.addIceGatheringDoneListener(
        new EventListener<IceGatheringDoneEvent>() {
      @Override
      public void onEvent(IceGatheringDoneEvent ev) {
        log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags());
      }
    });

    // Event: The ICE backend selected a new pair of ICE candidates for use
    webRtcEp.addNewCandidatePairSelectedListener(
        new EventListener<NewCandidatePairSelectedEvent>() {
      @Override
      public void onEvent(NewCandidatePairSelectedEvent ev) {
        log.info("[WebRtcEndpoint::{}] name: {}, timestamp: {}, tags: {}, streamId: {}, local: {}, remote: {}",
            ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
            ev.getTags(), ev.getCandidatePair().getStreamID(),
            ev.getCandidatePair().getLocalCandidate(),
            ev.getCandidatePair().getRemoteCandidate());
      }
    });
  }

  private void initWebRtcEndpoint(final WebSocketSession session,
      final WebRtcEndpoint webRtcEp, String sdpOffer)
  {
    addBaseEventListeners(session, webRtcEp, "WebRtcEndpoint");
    addWebRtcEventListeners(session, webRtcEp);

    /*
    OPTIONAL: Force usage of an Application-specific STUN server.
    Usually this is configured globally in KMS WebRTC settings file:
    /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini

    But it can also be configured per-application, as shown:

    log.info("[Handler::initWebRtcEndpoint] Using STUN server: 193.147.51.12:3478");
    webRtcEp.setStunServerAddress("193.147.51.12");
    webRtcEp.setStunServerPort(3478);
    */

    // Process the SDP Offer to generate an SDP Answer
    String sdpAnswer = webRtcEp.processOffer(sdpOffer);

    log.info("[Handler::initWebRtcEndpoint] name: {}, SDP Offer from browser to KMS:\n{}",
        webRtcEp.getName(), sdpOffer);
    log.info("[Handler::initWebRtcEndpoint] name: {}, SDP Answer from KMS to browser:\n{}",
        webRtcEp.getName(), sdpAnswer);

    JsonObject message = new JsonObject();
    message.addProperty("id", "PROCESS_SDP_ANSWER");
    message.addProperty("sdpAnswer", sdpAnswer);
    sendMessage(session, message.toString());
  }

  private void startWebRtcEndpoint(WebRtcEndpoint webRtcEp)
  {
    // Calling gatherCandidates() is when the Endpoint actually starts working.
    // In this tutorial, this is emphasized for demonstration purposes by
    // leaving the ICE candidate gathering in its own method.
    webRtcEp.gatherCandidates();
  }

  private void handleProcessSdpOffer(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    // ---- Session handling

    String sessionId = session.getId();

    log.info("[Handler::handleStart] User count: {}", users.size());
    log.info("[Handler::handleStart] New user: {}", sessionId);

    final UserSession user = new UserSession();
    users.put(session.getId(), user);


    // ---- Media pipeline

    log.info("[Handler::handleStart] Create Media Pipeline");
    final MediaPipeline pipeline = kurento.createMediaPipeline();
    user.setMediaPipeline(pipeline);

    final WebRtcEndpoint webRtcEp =
        new WebRtcEndpoint.Builder(pipeline).build();
    user.setWebRtcEndpoint(webRtcEp);


    // ---- Endpoint configuration

    webRtcEp.connect(webRtcEp);

    String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
    initWebRtcEndpoint(session, webRtcEp, sdpOffer);
    startWebRtcEndpoint(webRtcEp);


    // ---- Debug
    // String pipelineDot = pipeline.getGstreamerDot();
    // try (PrintWriter out = new PrintWriter("pipeline.dot")) {
    //   out.println(pipelineDot);
    // } catch (IOException ex) {
    //   log.error("[Handler::start] Exception: {}", ex.getMessage());
    // }
  }

  // ADD_ICE_CANDIDATE ---------------------------------------------------------

  private void handleAddIceCandidate(final WebSocketSession session,
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

      WebRtcEndpoint webRtcEp = user.getWebRtcEndpoint();
      webRtcEp.addIceCandidate(candidate);
    }
  }

  // STOP ----------------------------------------------------------------------

  public void sendPlayEnd(final WebSocketSession session)
  {
    if (users.containsKey(session.getId())) {
      JsonObject message = new JsonObject();
      message.addProperty("id", "END_PLAYBACK");
      sendMessage(session, message.toString());
    }
  }

  private void stop(final WebSocketSession session)
  {
    log.info("[Handler::stop]");

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

  private void handleStop(final WebSocketSession session,
      JsonObject jsonMessage)
  {
    stop(session);
  }

  // ---------------------------------------------------------------------------

  private void sendError(final WebSocketSession session, String errMsg)
  {
    if (users.containsKey(session.getId())) {
      JsonObject message = new JsonObject();
      message.addProperty("id", "ERROR");
      message.addProperty("message", errMsg);
      sendMessage(session, message.toString());
    }
  }

  private synchronized void sendMessage(final WebSocketSession session,
      String message)
  {
    if (!session.isOpen()) {
      log.error("[Handler::sendMessage] WebSocket session is closed");
      return;
    }

    try {
      session.sendMessage(new TextMessage(message));
    } catch (IOException ex) {
      log.error("[Handler::sendMessage] Exception: {}", ex.getMessage());
    }
  }
}
