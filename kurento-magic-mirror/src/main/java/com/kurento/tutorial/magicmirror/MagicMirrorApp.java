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
package com.kurento.tutorial.magicmirror;

import org.kurento.client.factory.KurentoClient;
import org.kurento.client.factory.KurentoClientFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Magic Mirror App (main).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 4.3.1
 */
@ComponentScan
@EnableAutoConfiguration
public class MagicMirrorApp {

	@Bean
	public KurentoClient kurentoClient() {
		return KurentoClientFactory.createKurentoClient();
	}

	public static void main(String[] args) throws Exception {
		new SpringApplication(MagicMirrorApp.class).run(args);
	}
}
