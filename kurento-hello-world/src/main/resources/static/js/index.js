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

var webRtcPeer;
var videoInput;
var videoOutput;

window.onload = function() {
	console = new Console('console', console);
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
	console.log("Loading complete ...");
}

function start() {
	console.log("Starting video call ...");
	showSpinner(videoInput, videoOutput);

	var options = {
      localVideo: videoInput,
      remoteVideo: videoOutput,
      oncandidategatheringdone: onCandidateGatheringDone
    }

	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
		function (error) {
		  if(error) {
			  return console.error(error);
		  }
		  webRtcPeer.generateOffer (onOffer);
	});
}

function stop() {
	if (webRtcPeer) {
		console.log("Stopping video call ...");
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	hideSpinner(videoInput, videoOutput);
}

function onCandidateGatheringDone () {
	$.ajax({
		url : location.protocol + '/helloworld',
		type : 'POST',
		dataType : 'text',
		contentType : 'application/sdp',
		data : webRtcPeer.getLocalSessionDescriptor().sdp,
		success : function(sdpAnswer) {
			console.log("Received sdpAnswer from server. Processing ...");
			webRtcPeer.processAnswer (sdpAnswer, function (error) {
				if (error) return console.error (error);
			});
		},
		error : function(jqXHR, textStatus, error) {
			onError(error);
		}
	});

}

function onOffer(sdpOffer) {
	console.info('Invoking SDP offer callback function ' + location.host);
}

function onError(error) {
	console.error(error);
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
