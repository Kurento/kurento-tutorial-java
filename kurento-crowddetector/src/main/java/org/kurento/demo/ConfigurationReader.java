package org.kurento.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.kurento.commons.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;

public class ConfigurationReader {

	private static final Logger log = LoggerFactory
			.getLogger(ConfigurationReader.class);
	private static final Gson gson = new GsonBuilder().create();

	private JsonObject configFile;

	public ConfigurationReader(String configFileName) throws IOException {

		String configFile = ClassPath.get(configFileName).toString();

		Path configFilePath = Paths.get(configFile);

		if (configFilePath == null) {
			log.debug("File not found {}", configFileName);
		}

		try (JsonReader reader = new JsonReader(Files
				.newBufferedReader(configFilePath, StandardCharsets.UTF_8))) {
			reader.setLenient(true);
			this.configFile = gson.fromJson(reader, JsonObject.class);

		} catch (NoSuchFileException e) {
			log.warn("Configuration file {} not found", configFilePath);
		} catch (IOException e) {
			log.error("Error opening config file {}", configFilePath, e);
		} catch (JsonParseException e) {
			log.error("Error parsing configuration file {}", configFilePath, e);
		}
	}

	public JsonObject getConfig() {
		return this.configFile;
	}
}
