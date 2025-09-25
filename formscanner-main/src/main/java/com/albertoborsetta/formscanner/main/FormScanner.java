package com.albertoborsetta.formscanner.main;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.albertoborsetta.formscanner.api.FormTemplate;
import com.albertoborsetta.formscanner.api.exceptions.FormScannerException;
import com.albertoborsetta.formscanner.commons.FormFileUtils;
import com.albertoborsetta.formscanner.commons.FormScannerConstants;
import com.albertoborsetta.formscanner.gui.FormScannerDesktop;
import com.albertoborsetta.formscanner.model.FormScannerModel;

import java.io.UnsupportedEncodingException;

import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import ch.randelshofer.quaqua.QuaquaLookAndFeel;

public class FormScanner {

	private static Logger logger;
	
	/**
	 * Launch the application.
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						FormScannerModel model = new FormScannerModel();
						
						logger = LogManager.getLogger(FormScanner.class.getName());
						
						UIManager.installLookAndFeel("Quaqua", QuaquaLookAndFeel.class.getName());
						
						for (LookAndFeelInfo info : UIManager
								.getInstalledLookAndFeels()) {
							if (model.getLookAndFeel().equals(info.getName())) {
								UIManager.setLookAndFeel(info.getClassName());
								break;
							}
						}
						FormScannerDesktop desktop = new FormScannerDesktop(
								model);
						model.setDesktop(desktop);
						desktop.setIconImage(model.getIcon());
					} catch (UnsupportedEncodingException
							| ClassNotFoundException | InstantiationException
							| IllegalAccessException
							| UnsupportedLookAndFeelException e) {
						logger.debug("Error", e);
					}
				}
			});
		} else {
			configureLogging();
			logger = LogManager.getLogger(FormScanner.class.getName());

			Locale locale = Locale.getDefault();
			FormFileUtils fileUtils = FormFileUtils.getInstance(locale);
			
			File templateFile = new File(args[0]);
			File diagnosticsOutputDir = null;
			if (args.length > 2 && args[2] != null && !args[2].trim().isEmpty()) {
				diagnosticsOutputDir = new File(args[2]);
			}
			FormTemplate template = null;
			DiagnosticsWriter diagnostics = null;
			AnnotatedImageWriter overlayWriter = null;
			try {
				template = new FormTemplate(templateFile);
				diagnostics = new DiagnosticsWriter(template);
				if (diagnosticsOutputDir != null) {
					overlayWriter = new AnnotatedImageWriter(template, diagnosticsOutputDir);
				}
				if (!FormScannerConstants.CURRENT_TEMPLATE_VERSION.equals(template.getVersion())) {
					fileUtils.saveToFile(FilenameUtils.getFullPath(args[0]), template, false);
				}
			} catch (ParserConfigurationException | SAXException | IOException e) {
				logger.debug("Error", e);
				System.exit(-1);
			}
			String[] extensions = ImageIO.getReaderFileSuffixes();
			Iterator<File> fileIterator = FileUtils.iterateFiles(
					new File(args[1]), extensions, false);
			HashMap<String, FormTemplate> filledForms = new HashMap<>();
			BufferedImage image = null;

			while (fileIterator.hasNext()) {
				File imageFile = (File) fileIterator.next();
				fileIterator.remove();
				
				try {
					image = ImageIO.read(imageFile);
				} catch (IOException e) {
					logger.debug("Error", e);
					System.exit(-1);
				}
				FormTemplate filledForm = new FormTemplate(
						imageFile.getName(), template);
				try {
					filledForm.findCorners(
							image, template.getThreshold(),
							template.getDensity(), template.getCornerType(), template.getCrop());
						filledForm.findPoints(
								image, template.getThreshold(),
								template.getDensity(), template.getSize());
							filledForm.findAreas(image);
					} catch (FormScannerException e) {
						logger.debug("Error", e);
						System.exit(-1);
					}
					if (diagnostics != null) {
						diagnostics.record(imageFile, image, filledForm);
					}
					if (overlayWriter != null) {
						overlayWriter.write(imageFile, image, filledForm);
					}
					filledForms
						.put(
								FilenameUtils.getName(imageFile.toString()),
								filledForm);
				}

			Date today = Calendar.getInstance().getTime();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			String timestamp = sdf.format(today);
			String separator = System.getProperty("file.separator");
			File outputFile = new File(
					args[1] + separator + "results_" + timestamp + ".csv");
			fileUtils.saveCsvAs(outputFile, filledForms, false);

			if (diagnostics != null) {
				File diagnosticsFile = new File(
						args[1] + separator + "results_" + timestamp + "_diagnostics.txt");
				try {
					diagnostics.write(diagnosticsFile);
				} catch (IOException e) {
					logger.debug("Error", e);
				}
			}
			System.exit(0);
		}
	}

	private static void configureLogging() {
		if (System.getProperty("log4j.configurationFile") != null) {
			return;
		}
		URL configUrl = FormScanner.class.getClassLoader().getResource("config/log4j.xml");
		if (configUrl != null) {
			System.setProperty("log4j.configurationFile", configUrl.toString());
		}
	}
}
