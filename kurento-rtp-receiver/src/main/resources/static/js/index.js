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

var ws = new WebSocket('wss://' + location.host + '/player');
var video;
var webRtcPeer;
var state = null;

var I_CAN_START = 0;
var I_CAN_STOP = 1;
var I_AM_STARTING = 2;

window.onload = function() {
  console = new Console();
  video = document.getElementById('video');
  setState(I_CAN_START);
}

window.onbeforeunload = function() {
  ws.close();
}

ws.onmessage = function(message) {
  var parsedMessage = JSON.parse(message.data);
  console.info('Received message: ' + message.data);

  switch (parsedMessage.id) {
    case 'startResponse':
      startResponse(parsedMessage);
      break;
    case 'error':
      if (state == I_AM_STARTING) {
        setState(I_CAN_START);
      }
      onError('Error message from server: ' + parsedMessage.message);
      break;
    case 'playEnd':
      playEnd();
      break;
    case 'iceCandidate':
      webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
        if (error) {
          return console.error('Error adding candidate: ' + error);
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
      onError('Unrecognized message', parsedMessage);
      break;
  }
}

function start() {
  // Disable start button
  setState(I_AM_STARTING);
  showSpinner(video);

  var options = {
    remoteVideo: video,
    mediaConstraints: {
      audio: true,
      video: true
    },
    onicecandidate: onIceCandidate
  }

  console.info('[start] Create WebRtcPeer');

  webRtcPeer =
    new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function(error) {
      if (error) {
        return console.error(error);
      }

      console.info('[WebRtcPeer] Generate SDP Offer');
      webRtcPeer.generateOffer(onOffer);
    });
}

function onOffer(error, offer) {
  if (error) {
    return console.error('Error generating the SDP Offer');
  }

  var message = {
    id: 'start',
    sdpOffer: offer,
    useComedia: document.getElementById('useComedia').checked,
    useSrtp: document.getElementById('useSrtp').checked,
  }

  console.info('[onOffer] Received SDP Offer; send message to Kurento Client at ' + location.host);
  console.info('[onOffer] COMEDIA checkbox is: ' + message.useComedia);
  console.info('[onOffer] SRTP checkbox is: ' + message.useSrtp);

  sendMessage(message);
}

function onError(error) {
  console.error(error);
}

function onIceCandidate(candidate) {
  console.log('[onIceCandidate] Local candidate: ' + JSON.stringify(candidate));

  var message = {
    id: 'onIceCandidate',
    candidate: candidate
  }
  sendMessage(message);
}

function startResponse(message) {
  setState(I_CAN_STOP);

  console.info('[startResponse] SDP Answer received from Kurento Client; process in WebRtcPeer');

  webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
    if (error) {
      return console.error(error);
    }
  });
}

function stop() {
  console.log('Stop video ...');
  setState(I_CAN_START);
  if (webRtcPeer) {
    webRtcPeer.dispose();
    webRtcPeer = null;

    var message = {
      id: 'stop'
    }
    sendMessage(message);
  }
  hideSpinner(video);
}

function playEnd() {
  setState(I_CAN_START);
  hideSpinner(video);
}

function setState(nextState) {
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
    onError('Unknown state ' + nextState);
    return;
  }
  state = nextState;
}

function sendMessage(message) {
  var jsonMessage = JSON.stringify(message);
  console.log('Send message: ' + jsonMessage);
  ws.send(jsonMessage);
}

function disableButton(id) {
  $(id).attr('disabled', true);
  $(id).removeAttr('onclick');
}

function enableButton(id, functionName) {
  $(id).attr('disabled', false);
  if (functionName) {
    $(id).attr('onclick', functionName);
  }
}

function showSpinner() {
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].poster = './img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
  }
}

function hideSpinner() {
  for (var i = 0; i < arguments.length; i++) {
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
