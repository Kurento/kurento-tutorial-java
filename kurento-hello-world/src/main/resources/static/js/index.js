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

var ws = new WebSocket('wss://' + location.host + '/helloworld');
var videoInput;
var videoOutput;
var webRtcPeer;
var state = null;

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

window.onload = function()
{
  console = new Console();
  console.log('Page loaded ...');
  videoInput = document.getElementById('videoInput');
  videoOutput = document.getElementById('videoOutput');
  setState(I_CAN_START);
}

window.onbeforeunload = function()
{
  ws.close();
}

ws.onmessage = function(message)
{
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
        if (error)
          return console.error('Error adding candidate: ' + error);
      });
      break;
    default:
      if (state == I_AM_STARTING) {
        setState(I_CAN_START);
      }
      onError('Unrecognized message', parsedMessage);
      break;
  }
}

function start()
{
  console.log('Starting video call ...');

  // Disable start button
  setState(I_AM_STARTING);
  showSpinner(videoInput, videoOutput);

  console.info('[start] Create WebRtcPeer');

  var options = {
    localVideo : videoInput,
    remoteVideo : videoOutput,
    onicecandidate : onIceCandidate
  }
  webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
      function(error) {
        if (error) {
          return console.error(error);
        }

        console.info('[WebRtcPeer] Generate SDP Offer');
        webRtcPeer.generateOffer(onOffer);
      });
}

function onOffer(error, offerSdp)
{
  if (error) {
    return console.error('Error generating the SDP Offer');
  }

  var message = {
    id : 'start',
    sdpOffer : offerSdp
  }

  console.info('[onOffer] Received SDP Offer; send message to Kurento Client at ' + location.host);
  sendMessage(message);
}

function onError(error)
{
  console.error(error);
}

function onIceCandidate(candidate)
{
  console.log('[onIceCandidate] Local candidate: ' + JSON.stringify(candidate));

  var message = {
    id : 'onIceCandidate',
    candidate : candidate
  };
  sendMessage(message);
}

function startResponse(message)
{
  setState(I_CAN_STOP);

  console.info('[startResponse] SDP Answer received from Kurento Client; process in WebRtcPeer');

  webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
    if (error) {
      return console.error(error);
    }
  });
}

function stop()
{
  console.info('[stop] Stop video playback');

  setState(I_CAN_START);
  if (webRtcPeer) {
    webRtcPeer.dispose();
    webRtcPeer = null;

    var message = {
      id : 'stop'
    }
    sendMessage(message);
  }
  hideSpinner(videoInput, videoOutput);
}

function playEnd()
{
  setState(I_CAN_START);
  hideSpinner(videoInput, videoOutput);
}

function setState(nextState)
{
  switch (nextState) {
    case I_CAN_START:
      $('#start').attr('disabled', false);
      $('#start').attr('onclick', 'start()');
      $('#stop').attr('disabled', true);
      $('#stop').removeAttr('onclick');
      break;
    case I_CAN_STOP:
      $('#start').attr('disabled', true);
      $('#stop').attr('disabled', false);
      $('#stop').attr('onclick', 'stop()');
      break;
    case I_AM_STARTING:
      $('#start').attr('disabled', true);
      $('#start').removeAttr('onclick');
      $('#stop').attr('disabled', true);
      $('#stop').removeAttr('onclick');
      break;
    default:
      onError('Unknown state ' + nextState);
      return;
  }
  state = nextState;
}

function sendMessage(message)
{
  console.info('[sendMessage] message: ' + message);

  var jsonMessage = JSON.stringify(message);
  ws.send(jsonMessage);
}

function showSpinner()
{
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].poster = './img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
  }
}

function hideSpinner()
{
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
