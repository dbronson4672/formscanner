package com.albertoborsetta.formscanner.commons.resources;

import java.awt.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class FormScannerResources {

	private static final String PNG = ".png";
	private static File iconsDirectory;
	private static File licenseDirectory;
	private static File extractedLicense;
	private static String template;
	private static final Logger logger = LogManager
			.getLogger(FormScannerResources.class.getName());

	public static void setResources(String path) {
		iconsDirectory = null;
		licenseDirectory = null;
		if (StringUtils.isBlank(path)) {
			return;
		}
		File base = new File(path);
		if (base.isFile()) {
			base = base.getParentFile();
		}
		if (base != null && base.isDirectory()) {
			File potentialIcons = new File(base, "icons");
			if (potentialIcons.isDirectory()) {
				iconsDirectory = potentialIcons;
			}
			File potentialLicense = new File(base, "license");
			if (potentialLicense.isDirectory()) {
				licenseDirectory = potentialLicense;
			}
		}
	}

	public static ImageIcon getIconFor(String key) {
		File iconFile = resolveIconFile(key);
		if (iconFile != null && iconFile.isFile()) {
			return new ImageIcon(iconFile.getAbsolutePath());
		}
		URL resource = FormScannerResources.class.getResource("/icons/" + key + PNG);
		return resource != null ? new ImageIcon(resource) : new ImageIcon();
	}

	public static void setTemplate(String tpl) {
		template = tpl;
	}

	public static File getTemplate() {
		return new File(template);
	}

	public static File getLicense() {
		if (licenseDirectory != null) {
			File file = new File(licenseDirectory, "license.txt");
			if (file.isFile()) {
				return file;
			}
		}
		if (extractedLicense == null) {
				try (InputStream in = FormScannerResources.class.getResourceAsStream("/license/license.txt")) {
					if (in != null) {
						extractedLicense = File.createTempFile("formscanner-license-", ".txt");
						extractedLicense.deleteOnExit();
						writeStreamToFile(in, extractedLicense);
					}
				} catch (IOException e) {
					logger.debug("Error", e);
				}
			}
			return extractedLicense;
		}

	public static Image getFormScannerIcon() {
		File iconFile = resolveIconFile(FormScannerResourcesKeys.FORMSCANNER_ICON);
		if (iconFile != null && iconFile.isFile()) {
			try {
				return ImageIO.read(iconFile);
			} catch (IOException e) {
				logger.debug("Unable to read icon from filesystem, falling back to classpath", e);
			}
		}
		try (InputStream stream = FormScannerResources.class
				.getResourceAsStream("/icons/" + FormScannerResourcesKeys.FORMSCANNER_ICON + PNG)) {
			return stream != null ? ImageIO.read(stream) : null;
		} catch (IOException e) {
			logger.catching(e);
			return null;
		}
	}

	private static File resolveIconFile(String key) {
		if (iconsDirectory != null && iconsDirectory.isDirectory()) {
			File iconFile = new File(iconsDirectory, key + PNG);
			if (iconFile.isFile()) {
				return iconFile;
			}
			}
			return null;
	}

	private static void writeStreamToFile(InputStream in, File target) throws IOException {
		ensureParentDirectory(target);
		try (FileOutputStream out = new FileOutputStream(target)) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
	}

	private static void ensureParentDirectory(File target) {
		File parent = target.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
	}
}
