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
package org.kurento.tutorial.player;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.EventListener;
import org.kurento.client.FaceOverlayFilter;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.KurentoClient;

/**
 * HTTP Player with Kurento; the media pipeline is composed by a PlayerEndpoint
 * connected to a filter (FaceOverlay) and an HttpGetEnpoint; the default
 * desktop web browser is launched to play the video.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class KurentoHttpPlayer {

	public static void main(String[] args) throws IOException,
			URISyntaxException, InterruptedException {
		// Connecting to Kurento Server
		KurentoClient kurento = KurentoClient
				.create("ws://localhost:8888/kurento");

		// Creating media pipeline
		MediaPipeline pipeline = kurento.createMediaPipeline();

		// Creating media elements
		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/fiwarecut.mp4").build();
		FaceOverlayFilter filter = new FaceOverlayFilter.Builder(pipeline)
				.build();
		filter.setOverlayedImage(
				"http://files.kurento.org/imgs/mario-wings.png", -0.2F, -1.1F,
				1.6F, 1.6F);
		HttpGetEndpoint http = new HttpGetEndpoint.Builder(pipeline).build();

		// Connecting media elements
		player.connect(filter);
		filter.connect(http);

		// Reacting to events
		player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
			@Override
			public void onEvent(EndOfStreamEvent event) {
				System.out.println("The playing has finished");
				System.exit(0);
			}
		});

		// Playing media and opening the default desktop browser
		player.play();
		String videoUrl = http.getUrl();
		Desktop.getDesktop().browse(new URI(videoUrl));

		// Setting a delay to wait the EndOfStream event, previously subscribed
		Thread.sleep(60000);
	}
}
