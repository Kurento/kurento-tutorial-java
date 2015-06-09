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
var from;

var registerName = null;
var registerState = null;
const NOT_REGISTERED = 0;
const REGISTERING = 1;
const REGISTERED = 2;

function setRegisterState(nextState) {
	switch (nextState) {
	case NOT_REGISTERED:
		$('#register').attr('disabled', false);
		setCallState(DISABLED);
		break;
	case REGISTERING:
		$('#register').attr('disabled', true);
		break;
	case REGISTERED:
		$('#register').attr('disabled', true);
		setCallState(NO_CALL);
		break;
	default:
		return;
	}
	registerState = nextState;
}

var callState = null;
const NO_CALL = 0;
const IN_CALL = 1;
const POST_CALL = 2;
const DISABLED = 3;
const IN_PLAY = 4;

function setCallState(nextState) {
	switch (nextState) {
	case NO_CALL:
		$('#call').attr('disabled', false);
		$('#terminate').attr('disabled', true);
		$('#play').attr('disabled', true);
		break;
	case DISABLED:
		$('#call').attr('disabled', true);
		$('#terminate').attr('disabled', true);
		$('#play').attr('disabled', true);
		break;
	case IN_CALL:
		$('#call').attr('disabled', true);
		$('#terminate').attr('disabled', false);
		$('#play').attr('disabled', true);
		break;
	case POST_CALL:
		$('#call').attr('disabled', false);
		$('#terminate').attr('disabled', true);
		$('#play').attr('disabled', false);
		break;
	case IN_PLAY:
		$('#call').attr('disabled', true);
		$('#terminate').attr('disabled', false);
		$('#play').attr('disabled', true);
		break;
	default:
		return;
	}
	callState = nextState;
}

window.onload = function() {
	setRegisterState(NOT_REGISTERED);
	console = new Console('console', console);
	var drag = new Draggabilly(document.getElementById('videoSmall'));
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
	case 'registerResponse':
		registerResponse(parsedMessage);
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
	case 'playResponse':
		playResponse(parsedMessage);
		break;
	case 'playEnd':
		playEnd();
		break;
	case 'iceCandidate':
	    webRtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
        if (!error) return;
	      console.error("Error adding candidate: " + error);
	    });
	    break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
}

function registerResponse(message) {
	if (message.response == 'accepted') {
		setRegisterState(REGISTERED);
		document.getElementById('peer').focus();
	} else {
		setRegisterState(NOT_REGISTERED);
		var errorMessage = message.message ? message.message : 'Unknown reason for register rejection.';
		console.log(errorMessage);
		document.getElementById('name').focus();
		alert('Error registering user. See console for further information.');
	}
}

function callResponse(message) {
	if (message.response != 'accepted') {
		console.info('Call not accepted by peer. Closing call');
		stop();
		setCallState(NO_CALL);
		if (message.message) {
			alert(message.message);
		}
	} else {
		setCallState(IN_CALL);
		webRtcPeer.processAnswer (message.sdpAnswer, function (error) {
			if (error) return console.error (error);
		});
	}
}

function startCommunication(message) {
	setCallState(IN_CALL);
	webRtcPeer.processAnswer (message.sdpAnswer, function (error) {
		if (error) return console.error (error);
	});
}

function playResponse(message) {
	if (message.response != 'accepted') {
		hideSpinner(videoOutput);
		document.getElementById('videoSmall').style.display = 'block';
		alert(message.error);
		document.getElementById('peer').focus();
		setCallState(POST_CALL);
	} else {
		setCallState(IN_PLAY);
		webRtcPeer.processAnswer (message.sdpAnswer, function (error) {
			if (error) return console.error (error);
		});
	}
}

function incomingCall(message) {
	// If bussy just reject without disturbing user
	if (callState != NO_CALL && callState != POST_CALL) {
		var response = {
			id : 'incomingCallResponse',
			from : message.from,
			callResponse : 'reject',
			message : 'bussy'
		};
		return sendMessage(response);
	}

	setCallState(DISABLED);
	if (confirm('User ' + message.from
			+ ' is calling you. Do you accept the call?')) {
		showSpinner(videoInput, videoOutput);

		from = message.from;
		var options = {
			      localVideo: videoInput,
			      remoteVideo: videoOutput,
			      onicecandidate: onIceCandidate
			    }
	    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
	      function (error) {
			  if(error) {
				  return console.error(error);
			  }
			  this.generateOffer (onOfferIncomingCall);
			});
	} else {
		var response = {
			id : 'incomingCallResponse',
			from : message.from,
			callResponse : 'reject',
			message : 'user declined'
		};
		sendMessage(response);
		stop();
	}
}

function onOfferIncomingCall (error, offerSdp) {
	if (error) return console.error ("Error generating the offer");
	var response = {
			id : 'incomingCallResponse',
			from : from,
			callResponse : 'accept',
			sdpOffer : offerSdp
		};
	sendMessage(response);
}

function register() {
	var name = document.getElementById('name').value;
	if (name == '') {
		window.alert("You must insert your user name");
		document.getElementById('name').focus();
		return;
	}
	setRegisterState(REGISTERING);

	var message = {
		id : 'register',
		name : name
	};
	sendMessage(message);
}

function call() {
	if (document.getElementById('peer').value == '') {
		document.getElementById('peer').focus();
		window.alert("You must specify the peer name");
		return;
	}
	setCallState(DISABLED);
	showSpinner(videoInput, videoOutput);

	var options = {
		      localVideo: videoInput,
		      remoteVideo: videoOutput,
		      onicecandidate: onIceCandidate
		    }
	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
		function (error) {
		  if(error) {
			  return console.error(error);
		  }
		  this.generateOffer (onOfferCall);
	});
}

function onOfferCall (error, offerSdp) {
	if (error) return console.error ("Error generating the offer");
	console.log('Invoking SDP offer callback function');
	var message = {
		id : 'call',
		from : document.getElementById('name').value,
		to : document.getElementById('peer').value,
		sdpOffer : offerSdp
	};
	sendMessage(message);
}

function play() {
	var peer = document.getElementById('peer').value;
	if (peer == '') {
		window.alert("You must insert the name of the user recording to be played (field 'Peer')");
		document.getElementById('peer').focus();
		return;
	}

	document.getElementById('videoSmall').style.display = 'none';
	setCallState(DISABLED);
	showSpinner(videoOutput);

	var options = {
		      remoteVideo: videoOutput,
		      onicecandidate: onIceCandidate
		    }
	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
		function (error) {
		  if(error) {
			  return console.error(error);
		  }
		  this.generateOffer (onOfferPlay);
	});
}

function onOfferPlay (error, offerSdp) {
	console.log('Invoking SDP offer callback function');
	var message = {
		id : 'play',
		user : document.getElementById('peer').value,
		sdpOffer : offerSdp
	};
	sendMessage(message);
}

function playEnd() {
	setCallState(POST_CALL);
	hideSpinner(videoInput, videoOutput);
	document.getElementById('videoSmall').style.display = 'block';
}

function stop(message) {
	var stopMessageId = (callState == IN_CALL) ? 'stop' : 'stopPlay';
	setCallState(POST_CALL);
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;

		if (!message) {
			var message = {
				id : stopMessageId
			}
			sendMessage(message);
		}
	}
	hideSpinner(videoInput, videoOutput);
	document.getElementById('videoSmall').style.display = 'block';
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function onIceCandidate(candidate) {
	  console.log("Local candidate" + JSON.stringify(candidate));

	  var message = {
	    id: 'onIceCandidate',
	    candidate: candidate
	  };
	  sendMessage(message);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
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
