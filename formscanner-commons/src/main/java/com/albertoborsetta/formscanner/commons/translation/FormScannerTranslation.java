package com.albertoborsetta.formscanner.commons.translation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.StringUtils;

public class FormScannerTranslation extends Properties {

	/**
     *
     */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(FormScannerTranslation.class.getName());
	protected static FormScannerTranslation translations = null;

	private FormScannerTranslation(String path, String language) {
		super();
		String resourcePath = "language/formscanner-" + language + ".lang";
		try (InputStream translationStream = openTranslationStream(path, resourcePath)) {
			if (translationStream != null) {
				load(translationStream);
			} else {
				logger.debug("Missing translation resource: {}", resourcePath);
			}
		} catch (IOException e) {
			logger.debug("Error", e);
		}
	}

	private InputStream openTranslationStream(String path, String resourcePath) throws IOException {
		if (StringUtils.isNotBlank(path)) {
			File translationFile = new File(path, resourcePath);
			if (translationFile.isFile()) {
				return new FileInputStream(translationFile);
			}
		}
		return FormScannerTranslation.class.getClassLoader().getResourceAsStream(resourcePath);
	}

	public static void setTranslation(String path, String language) {
		translations = new FormScannerTranslation(path, language);
	}

	public static String getTranslationFor(String key) {
		String value = translations.getProperty(key, key);
		try {
			value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.debug("Error", e);
		}
		return value;
	}

	public static char getMnemonicFor(String key) {
		String value = translations.getProperty(key, key);
		try {
			value = new String(value.getBytes("ISO-8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.debug("Error", e);
		}
		return value.charAt(0);
	}
}
