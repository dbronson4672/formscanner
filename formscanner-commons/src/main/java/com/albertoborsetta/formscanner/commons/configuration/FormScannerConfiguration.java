package com.albertoborsetta.formscanner.commons.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FormScannerConfiguration extends Properties {

	private static final String CONFIG_FILE_NAME = "formscanner.properties";
	private static String userConfigFile;

	/**
     *
     */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager
			.getLogger(FormScannerConfiguration.class.getName());
	private static FormScannerConfiguration configurations = null;

	private FormScannerConfiguration() {
		super();
		try {
			if (StringUtils.isNotBlank(userConfigFile) && new File(userConfigFile).isFile()) {
				load(new FileInputStream(userConfigFile));
			} else {
				try (InputStream in = FormScannerConfiguration.class.getClassLoader()
					.getResourceAsStream("config/" + CONFIG_FILE_NAME)) {
					if (in != null) {
						load(in);
					}
				}
			}
		} catch (IOException e) {
			logger.debug("Error", e);
		}
	}

	public static FormScannerConfiguration getConfiguration(String userPath,
			String installPath) {
		if (configurations == null) {
			userConfigFile = userPath + CONFIG_FILE_NAME;

			File userFile = new File(userConfigFile);
			if (!userFile.exists() || userFile.isDirectory()) {
				boolean copied = copyDefaultConfigTo(userFile, installPath);
				if (!copied) {
					System.out.println("Cannot load user configurations... try loading defaults");
					userConfigFile = null;
				}
			}
			if (userFile.exists() && userFile.isFile()) {
				userConfigFile = userFile.getAbsolutePath();
			}

			if (StringUtils.isBlank(userConfigFile) || (!new File(userConfigFile).isFile())) {
				userConfigFile = null;
			}

			configurations = new FormScannerConfiguration();
		}
		return configurations;
	}

	private static boolean copyDefaultConfigTo(File targetFile, String installPath) {
		if (targetFile == null) {
			return createTempConfigFromClasspath();
		}
		if (StringUtils.isNotBlank(installPath)) {
			File defaultFile = new File(installPath + File.separator + "config" + File.separator + CONFIG_FILE_NAME);
			if (defaultFile.isFile()) {
				try {
					ensureParentDirectory(targetFile);
					FileUtils.copyFile(defaultFile, targetFile);
					userConfigFile = targetFile.getAbsolutePath();
					return true;
				} catch (IOException e) {
					logger.debug("Error", e);
				}
			}
		}
		try (InputStream in = FormScannerConfiguration.class.getClassLoader()
				.getResourceAsStream("config/" + CONFIG_FILE_NAME)) {
			if (in != null) {
				ensureParentDirectory(targetFile);
				writeStreamToFile(in, targetFile);
				userConfigFile = targetFile.getAbsolutePath();
				return true;
			}
		} catch (IOException e) {
			logger.debug("Error", e);
		}
		return createTempConfigFromClasspath();
	}

	private static boolean createTempConfigFromClasspath() {
		try (InputStream in = FormScannerConfiguration.class.getClassLoader()
				.getResourceAsStream("config/" + CONFIG_FILE_NAME)) {
			if (in != null) {
				File tempFile = File.createTempFile("formscanner-default-", ".properties");
				writeStreamToFile(in, tempFile);
				tempFile.deleteOnExit();
				userConfigFile = tempFile.getAbsolutePath();
				return true;
			}
		} catch (IOException e) {
			logger.debug("Error", e);
		}
		return false;
	}

	private static void ensureParentDirectory(File targetFile) {
		File parent = targetFile.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
	}

	private static void writeStreamToFile(InputStream in, File target) throws IOException {
		try (FileOutputStream out = new FileOutputStream(target)) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
	}

	public void store() {
		try {
			store(new FileOutputStream(userConfigFile), null);
		} catch (IOException e) {
			logger.debug("Error", e);
		}
	}

	public <T> Object getProperty(String key, T defaultValue) {
		String val = getProperty(key);

		if (StringUtils.isEmpty(val))
			return defaultValue;
		
		if (defaultValue instanceof Integer)
			return Integer.valueOf(val);
		
		if (defaultValue instanceof Boolean)
			return Boolean.valueOf(val);
		
		if (defaultValue instanceof String)
			return val;
		
		return val;
	}

//	public Integer getProperty(String key, Integer defaultValue) {
//		String val = getProperty(key);
//		return (StringUtils.isEmpty(val)) ? defaultValue : Integer.valueOf(val);
//	}
//	
//	public boolean getProperty(String key, boolean defaultValue) {
//		String val = getProperty(key);
//		return (StringUtils.isEmpty(val)) ? defaultValue : Boolean.parseBoolean(val);
//	}

	public <T> void setProperty(String key, T value) {
		setProperty(key, String.valueOf(value));
	}
}
