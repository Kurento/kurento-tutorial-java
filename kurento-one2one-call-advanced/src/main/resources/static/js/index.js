var ws = new WebSocket('ws://' + location.host + '/call');
var videoInput = document.getElementById('videoInput');
var videoOutput = document.getElementById('videoOutput');
var webRtcPeer;

window.onload = function() {
	console = new Console('console', console);
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
	case 'incommingCall':
		incommingCall(parsedMessage);
		break;
	case 'startCommunication':
		startCommunication(parsedMessage);
		break;
	case 'playResponse':
		playResponse(parsedMessage);
		break;
	case 'playEnd':
		stop();
		break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
}

function resgisterResponse(message) {
	if (message.response != 'accepted') {
		alert('Error registering user. See log for further information.');
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

function playResponse(message) {
	if (message.response != 'accepted') {
		alert('Play request reject.');
	} else {
		webRtcPeer.processSdpAnswer(message.sdpAnswer);
	}
	hideSpinner(videoOutput);
}

function incommingCall(message) {
	if (confirm('User ' + message.from
			+ ' is calling you. Do you accept the call?')) {
		showSpinner(videoInput, videoOutput);
		webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
				function(sdp, wp) {
					var response = {
						id : 'incommingCallResponse',
						from : message.from,
						callResponse : 'accept',
						sdpOffer : sdp
					};
					sendMessage(response);
				});
	} else {
		var response = {
			id : 'incommingCallResponse',
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
}

function call() {
	showSpinner(videoInput, videoOutput);

	kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(
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

function play() {
	showSpinner(videoOutput);

	kwsUtils.WebRtcPeer.startRecvOnly(videoOutput, function(offerSdp, wp) {
		webRtcPeer = wp;
		console.log('Invoking SDP offer callback function');
		var message = {
			id : 'play',
			user : document.getElementById('peer').value,
			sdpOffer : offerSdp
		};
		sendMessage(message);
	});
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
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
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = '';
		arguments[i].style.background = '';
	}
}
