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

var ws = new WebSocket('ws://' + location.host + '/call');
var videoInput;
var videoOutput;
var webRtcPeer;

window.onload = function() {
	console = new Console('console', console);
	dragDrop.initElement('videoSmall');
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
	document.getElementById('name').focus();
}

window.onbeforeunload = function() {
	ws.close();
}

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'resgisterResponse':
		resgisterResponse(parsedMessage);
		break;
	case 'callResponse':
		callResponse(parsedMessage);
		break;
	case 'incomingCall':
		incomingCall(parsedMessage);
		break;
	case 'startCommunication':
		startCommunication(parsedMessage);
		break;
	case 'stopCommunication':
		console.info("Communication ended by remote peer");
		stop(true);
		break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
}

function resgisterResponse(message) {
	if (message.response != 'accepted') {
		alert('Error registering user. See console for further information.');
	}
}

function callResponse(message) {
	if (message.response != 'accepted') {
		console.info('Call not accepted by peer. Closing call');
		stop();
	} else {
		webRtcPeer.processSdpAnswer(message.sdpAnswer);
	}
}

function startCommunication(message) {
	webRtcPeer.processSdpAnswer(message.sdpAnswer);
}

function incomingCall(message) {
	if (confirm('User ' + message.from
			+ ' is calling you. Do you accept the call?')) {
		showSpinner(videoInput, videoOutput);
		webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput,
				videoOutput, function(sdp, wp) {
					var response = {
						id : 'incomingCallResponse',
						from : message.from,
						callResponse : 'accept',
						sdpOffer : sdp
					};
					sendMessage(response);
				});
	} else {
		var response = {
			id : 'incomingCallResponse',
			from : message.from,
			callResponse : 'reject'
		};
		sendMessage(response);
		stop();
	}
}

function register() {
	var message = {
		id : 'register',
		name : document.getElementById('name').value
	};
	sendMessage(message);
	document.getElementById('peer').focus();
}

function call() {
	showSpinner(videoInput, videoOutput);

	kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(
			offerSdp, wp) {
		webRtcPeer = wp;
		console.log('Invoking SDP offer callback function');
		var message = {
			id : 'call',
			from : document.getElementById('name').value,
			to : document.getElementById('peer').value,
			sdpOffer : offerSdp
		};
		sendMessage(message);
	});
}

function stop(message) {
	if (webRtcPeer) {
		webRtcPeer.dispose();

		if (!message) {
			var message = {
				id : 'stop'
			}
			sendMessage(message);
		}
	}
	videoInput.src = '';
	videoOutput.src = '';
	hideSpinner(videoInput, videoOutput);
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
