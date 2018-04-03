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

const ws = new WebSocket('wss://' + location.host + '/player');
let video;
let webRtcPeer;
let state = null;

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

window.onload = function()
{
  console = new Console();
  console.log('Page loaded');
  video = document.getElementById('video');
  setState(I_CAN_START);
}

window.onbeforeunload = function()
{
  ws.close();
}

ws.onmessage = function(message)
{
  const parsedMessage = JSON.parse(message.data);
  console.log('[onmessage] Received message: ' + message.data);

  switch (parsedMessage.id) {
    case 'startResponse':
      startResponse(parsedMessage);
      break;
    case 'error':
      if (state == I_AM_STARTING) {
        setState(I_CAN_START);
      }
      console.error('[onmessage] Error message from server: ' + parsedMessage.message);
      break;
    case 'playEnd':
      playEnd();
      break;
    case 'iceCandidate':
      webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
        if (error) {
          console.error('[onmessage] Error adding candidate: ' + error);
          return;
        }
      });
      break;
    case 'msgConnInfo':
      document.getElementById("msgConnInfo").value = parsedMessage.text;
      break;
    case 'msgSdpText':
      document.getElementById("msgSdpText").value = parsedMessage.text;
      break;
    default:
      if (state == I_AM_STARTING) {
        setState(I_CAN_START);
      }
      console.error('[onmessage] Unrecognized message: ', parsedMessage);
      break;
  }
}

function explainUserMediaError(error)
{
  const n = error.name;
  if (n == 'NotFoundError' || n == 'DevicesNotFoundError') {
    return "Missing webcam for required tracks";
  }
  else if (n == 'NotReadableError' || n == 'TrackStartError') {
    return "Webcam is already in use";
  }
  else if (n == 'OverconstrainedError' || n == 'ConstraintNotSatisfiedError') {
    return "Webcam doesn't provide required tracks";
  }
  else if (n == 'NotAllowedError' || n == 'PermissionDeniedError') {
    return "Webcam permission has been denied by the user";
  }
  else if (n == 'TypeError') {
    return "No media tracks have been requested";
  }
  else {
    return "Unknown error";
  }
}

function start()
{
  console.log('[start] Update UI');

  // Disable start button
  setState(I_AM_STARTING);
  showSpinner(video);

  console.log('[start] Create WebRtcPeer');

  const options = {
    remoteVideo: video,
    mediaConstraints: { audio: true, video: true },
    onicecandidate: onIceCandidate
  };
  webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
      function(error) {
        if (error) {
          console.error('[WebRtcPeer] Error in constructor: ' + explainUserMediaError(error));
          return;
        }

        console.log('[WebRtcPeer] Generate SDP Offer');
        webRtcPeer.generateOffer(onOffer);
      });
}

function onOffer(error, offerSdp)
{
  if (error) {
    console.error('[onOffer] Error generating SDP Offer: ' + error);
    return;
  }

  console.log('[onOffer] Received SDP Offer; send message to Kurento Client at ' + location.host);

  const message = {
    id: 'start',
    sdpOffer: offerSdp,
    useComedia: document.getElementById('useComedia').checked,
    useSrtp: document.getElementById('useSrtp').checked,
  };

  console.info('[onOffer] COMEDIA checkbox is: ' + message.useComedia);
  console.info('[onOffer] SRTP checkbox is: ' + message.useSrtp);

  sendMessage(message);
}

function onIceCandidate(candidate)
{
  console.log('[onIceCandidate] Local candidate: ' + JSON.stringify(candidate));

  const message = {
    id: 'onIceCandidate',
    candidate: candidate
  };
  sendMessage(message);
}

function startResponse(message)
{
  setState(I_CAN_STOP);

  console.log('[startResponse] SDP Answer received from Kurento Client; process in WebRtcPeer');

  webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
    if (error) {
      console.error('[startResponse] Error processing SDP Answer: ' + error);
      return;
    }
  });
}

function stop()
{
  console.log('[stop] Stop video playback');

  setState(I_CAN_START);
  if (webRtcPeer) {
    webRtcPeer.dispose();
    webRtcPeer = null;

    const message = {
      id: 'stop'
    };
    sendMessage(message);
  }
  hideSpinner(video);
}

function playEnd()
{
  setState(I_CAN_START);
  hideSpinner(video);
}

function setState(nextState)
{
  switch (nextState) {
    case I_CAN_START:
      enableButton('#start', 'start()');
      disableButton('#stop');
      break;
    case I_CAN_STOP:
      disableButton('#start');
      enableButton('#stop', 'stop()');
      break;
    case I_AM_STARTING:
      disableButton('#start');
      disableButton('#stop');
      break;
    default:
      console.error('[setState] Unknown state: ' + nextState);
      return;
  }
  state = nextState;
}

function sendMessage(message)
{
  const jsonMessage = JSON.stringify(message);
  console.log('[sendMessage] message: ' + jsonMessage);
  ws.send(jsonMessage);
}

function disableButton(id)
{
  $(id).attr('disabled', true);
  $(id).removeAttr('onclick');
}

function enableButton(id, functionName)
{
  $(id).attr('disabled', false);
  if (functionName) {
    $(id).attr('onclick', functionName);
  }
}

function showSpinner()
{
  for (let i = 0; i < arguments.length; i++) {
    arguments[i].poster = './img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
  }
}

function hideSpinner()
{
  for (let i = 0; i < arguments.length; i++) {
    arguments[i].src = '';
    arguments[i].poster = './img/webrtc.png';
    arguments[i].style.background = '';
  }
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
  event.preventDefault();
  $(this).ekkoLightbox();
});
