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
 */
package org.kurento.tutorial.one2onecalladv;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.WebRtcEndpoint;

/**
 * Media Pipeline (connection of Media Elements) for the advanced one to one
 * video communication.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 5.0.0
 */
public class CallMediaPipeline {
	
	private static final SimpleDateFormat df = new SimpleDateFormat(
			"yyyy-MM-dd_HH-mm-ss-S");
	public static final String RECORDING_PATH = "file:///tmp/"
			+ df.format(new Date()) + "-";
	public static final String RECORDING_EXT = ".webm";
	
	private MediaPipeline pipeline;
	private WebRtcEndpoint webRtcCaller;
	private WebRtcEndpoint webRtcCallee;
	private RecorderEndpoint recorderCaller;
	private RecorderEndpoint recorderCallee;
	
	public CallMediaPipeline(KurentoClient kurento, String from, String to) {
		
		// Media pipeline
		pipeline = kurento.createMediaPipeline();
		
		// Media Elements (WebRtcEndpoint, RecorderEndpoint, FaceOverlayFilter)
		webRtcCaller = new WebRtcEndpoint.Builder(pipeline).build();
		webRtcCallee = new WebRtcEndpoint.Builder(pipeline).build();
		
		recorderCaller =
				new RecorderEndpoint.Builder(pipeline, RECORDING_PATH + from
						+ RECORDING_EXT).build();
		recorderCallee =
				new RecorderEndpoint.Builder(pipeline, RECORDING_PATH + to
						+ RECORDING_EXT).build();
		
		String appServerUrl =
				System.getProperty("app.server.url",
						One2OneCallAdvApp.DEFAULT_APP_SERVER_URL);
		FaceOverlayFilter faceOverlayFilterCaller =
				new FaceOverlayFilter.Builder(pipeline).build();
		faceOverlayFilterCaller.setOverlayedImage(appServerUrl
				+ "/img/mario-wings.png", -0.35F, -1.2F, 1.6F, 1.6F);
		
		FaceOverlayFilter faceOverlayFilterCallee =
				new FaceOverlayFilter.Builder(pipeline).build();
		faceOverlayFilterCallee.setOverlayedImage(
				appServerUrl + "/img/Hat.png", -0.2F, -1.35F, 1.5F, 1.5F);
		
		// Connections
		webRtcCaller.connect(faceOverlayFilterCaller);
		faceOverlayFilterCaller.connect(webRtcCallee);
		faceOverlayFilterCaller.connect(recorderCaller);
		
		webRtcCallee.connect(faceOverlayFilterCallee);
		faceOverlayFilterCallee.connect(webRtcCaller);
		faceOverlayFilterCallee.connect(recorderCallee);
	}
	
	public void record() {
		recorderCaller.record();
		recorderCallee.record();
	}
	
	public String generateSdpAnswerForCaller(String sdpOffer) {
		return webRtcCaller.processOffer(sdpOffer);
	}
	
	public String generateSdpAnswerForCallee(String sdpOffer) {
		return webRtcCallee.processOffer(sdpOffer);
	}
	
	public MediaPipeline getPipeline() {
		return pipeline;
	}
}
