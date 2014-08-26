var webRtcPeer;
var videoInput = document.getElementById('videoInput');
var videoOutput = document.getElementById('videoOutput');

window.onload = function() {
	console = new Console('console', console);
}

function start() {
	showSpinner(videoInput, videoOutput);

	webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
			function(offerSdp, wp) {
				console.log('Invoking SDP offer callback function '
						+ location.host);
				$.ajax({
					url : location.protocol + '/magicmirror',
					type : 'POST',
					dataType : 'text',
					data : offerSdp,
					success : function(data) {
						wp.processSdpAnswer(data);
					},
					error : function(jqXHR, textStatus, error) {
						console.error(error);
					}
				});
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
