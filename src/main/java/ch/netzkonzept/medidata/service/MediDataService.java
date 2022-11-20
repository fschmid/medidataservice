/*******************************************************************************
 * Copyright (c) Netzkonzept Gmbh <info@netzkonzept.ch>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Netzkonzept Gmbh <info@netzkonzept.ch> - initial implementation
 ******************************************************************************/

package ch.netzkonzept.medidata.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ch.netzkonzept.medidata.data.DocumentListEntry;
import ch.netzkonzept.medidata.data.MessageLog;
import ch.netzkonzept.medidata.data.MessageLogEntry;
import ch.netzkonzept.medidata.data.Participant;
import ch.netzkonzept.medidata.data.ParticipantsDirectory;
import ch.netzkonzept.medidata.data.TransmissionLog;
import ch.netzkonzept.medidata.data.TransmissionLogEntry;

public class MediDataService {

	private Configuration config;
	private MediDataBoxFacade box;
	private TransmissionLog transLog;
	private MessageLog msgLog;
	private ParticipantsDirectory partDir;
	private Path baseDir;
	private Path receiveDir;
	private Path sendDir;
	private Path errorDir;
	private Path processingDir;
	private Path doneDir;
	private boolean checkGLN;

	private static Logger LOGGER = LoggerFactory.getLogger(MediDataService.class);

	public MediDataService(File configFile) throws ConfigurationException, KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException, FileNotFoundException, UnsupportedEncodingException {

		config = new Configurations().properties(configFile);

		box = new MediDataBoxFacade(config.getString("MedidataHost"), config.getString("AuthToken"),
				config.getString("XClientID"));

		checkGLN = config.getBoolean("CheckGLN");

		baseDir = Paths.get(config.getString("BaseDir"));
		receiveDir = baseDir.resolve("receive");
		sendDir = baseDir.resolve("send");
		errorDir = sendDir.resolve("error");
		processingDir = sendDir.resolve("processing");
		doneDir = sendDir.resolve("done");

		transLog = new TransmissionLog(new File(baseDir + File.separator + "transmissions.json"));
		msgLog = new MessageLog(new File(baseDir + File.separator + "messages.json"));
		partDir = new ParticipantsDirectory(new File(baseDir + File.separator + "participants.json"));

	}

	public void sendDocuments() {

		File filesList[] = new File(sendDir.toString()).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xml");
			}
		});

		LOGGER.info("Sending new invoices, found " + filesList.length + " invoices to process.");

		for (File dataFile : filesList) {
			TransmissionLogEntry transRef;
			LOGGER.info("Processing invoice " + dataFile.getName());
			try {
				LOGGER.info("Checking toGLN in participants directory");
				boolean glnOK = partDir.checkGLN(getToGLNFromInvoiceFile(dataFile));
				File controlFile = new File(dataFile.getAbsolutePath().replaceFirst("[.][^.]+$", "") + ".json");
				if (glnOK || !checkGLN) {
					if (checkGLN) {
						LOGGER.info("GLN check passed");
					} else {
						LOGGER.info("GLN check skipped");
					}
					if (controlFile.exists()) {
						LOGGER.info("Sending Data File " + dataFile.getName() + " and control File "
								+ controlFile.getName());
						transRef = box.postUploads(dataFile, controlFile);
						Files.move(controlFile.toPath(),
								processingDir.resolve(FilenameUtils.getName(controlFile.getAbsolutePath())));
						transRef.setControlFile(FilenameUtils.getName(controlFile.getAbsolutePath()));
					} else {
						LOGGER.info("Sending Data File " + dataFile.getName());
						transRef = box.postUploads(dataFile);
					}
					Files.move(dataFile.toPath(),
							processingDir.resolve(FilenameUtils.getName(dataFile.getAbsolutePath())));
					transRef.setInvoiceReference(FilenameUtils.getName(dataFile.getAbsolutePath()));
					transLog.insertTransmission(transRef);
				} else {
					LOGGER.error("GLN invalid, moving invoice to ERROR Directory");
					if (controlFile.exists()) {
						Files.move(controlFile.toPath(),
								errorDir.resolve(FilenameUtils.getName(controlFile.getAbsolutePath())));
					}
					Files.move(dataFile.toPath(), errorDir.resolve(FilenameUtils.getName(dataFile.getAbsolutePath())));
				}
			} catch (Exception e) {
				LOGGER.error("Error sending " + dataFile.getAbsolutePath() + " resuming next");
				LOGGER.debug(getStackTrace(e));
			}
		}
	}

	public void updateTransmissionStatus() {
		List<TransmissionLogEntry> toUpdate = transLog.getTransmissionsToUpdate();
		LOGGER.info("Updating transmission status of pending transmissions, found " + toUpdate.size()
				+ " transmission log entries to process.");
		for (TransmissionLogEntry le : toUpdate) {
			LOGGER.info("Updating transmission " + le.getTransmissionReference());
			try {
				TransmissionLogEntry result = box.getStatus(le.getTransmissionReference());
				le.setStatus(result.getStatus());
				le.setModified(result.getModified());
				le.setCreated(result.getCreated());
				transLog.updateTransmission(le);
				switch (le.getStatus()) {
				case TransmissionLogEntry.ERROR:
					LOGGER.error("Got ERROR status on" + le.getTransmissionReference()
							+ " moving invoice to ERROR directory");
					Files.move(processingDir.resolve(le.getInvoiceReference()),
							errorDir.resolve(le.getInvoiceReference()));
					if (!(le.getControlFile() == null)) {
						Files.move(processingDir.resolve(le.getControlFile()), errorDir.resolve(le.getControlFile()));
					}
					break;
				case TransmissionLogEntry.DONE:
					LOGGER.info(
							"Got DONE status on" + le.getTransmissionReference() + " moving invoice to DONE directory");
					Files.move(processingDir.resolve(le.getInvoiceReference()),
							doneDir.resolve(le.getInvoiceReference()));
					if (!(le.getControlFile() == null)) {
						Files.move(processingDir.resolve(le.getControlFile()), doneDir.resolve(le.getControlFile()));
					}
					break;
				}
			} catch (Exception e) {
				LOGGER.error("Error updating transmission " + le.getTransmissionReference() + " resuming next");
				LOGGER.debug(getStackTrace(e));
			}
		}
	}

	public void receiveDocuments() {
		LOGGER.info("Checking for new documents to download");
		try {
			DocumentListEntry[] docs = box.getDocumentsList();
			for (DocumentListEntry doc : docs) {
				if (doc.getStatus().equals("PENDING")) {
					try {
						LOGGER.info("Download new pending document " + doc.getDocumentReference() + "-"
								+ doc.getTransmissionReference());
						doc = box.getDocument(doc);
						LOGGER.info("Saving document " + doc.getDocumentReference() + "-"
								+ doc.getTransmissionReference() + ".xml");
						BufferedWriter writer = Files.newBufferedWriter(
								receiveDir.resolve(doc.getTransmissionReference() + ".xml"), StandardCharsets.UTF_8);
						writer.write(doc.getDocument());
						writer.close();
						LOGGER.info("Confirming document " + doc.getDocumentReference() + "-"
								+ doc.getTransmissionReference());
						box.confirmDocument(doc);
					} catch (Exception e) {
						LOGGER.error("Error processing document " + doc.getDocumentReference() + "-"
								+ doc.getTransmissionReference() + " resuming next.");
						LOGGER.debug(getStackTrace(e));
					}
				} else {
					LOGGER.debug("Skipping already downloaded document " + doc.getDocumentReference() + "-"
							+ doc.getTransmissionReference());
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error receive documents");
			LOGGER.debug(getStackTrace(e));
		}

	}

	public void upsertMessages() {
		LOGGER.info("Updating existing messages and downloading new messages");
		try {
			MessageLogEntry[] messages = box.getMessages();
			for (MessageLogEntry msg : messages) {
				msgLog.upsertMessage(msg);
			}

		} catch (IOException e) {
			LOGGER.error("Error upsert messages");
			LOGGER.debug(getStackTrace(e));
		}

	}

	public void upsertParticipants() {
		LOGGER.info("Updating existing participants and downloading new participants");
		try {
			Participant[] participants = box.getParticipants();
			for (Participant p : participants) {
				partDir.upsertParticipant(p);
			}

		} catch (IOException e) {
			LOGGER.error("Error upsert participants");
			LOGGER.debug(getStackTrace(e));
		}

	}

	private String getToGLNFromInvoiceFile(File dataFile)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder builder = docFactory.newDocumentBuilder();
		Document doc = builder.parse(dataFile);
		XPathExpression expr = XPathFactory.newInstance().newXPath().compile("//*[\"transport\"=local-name()]/@to");
		return (String) expr.evaluate(doc, XPathConstants.STRING);
	}

	public static String getStackTrace(final Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

}
