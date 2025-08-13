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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import ch.netzkonzept.medidata.data.DocumentListEntry;
import ch.netzkonzept.medidata.data.MessageLogEntry;
import ch.netzkonzept.medidata.data.Participant;
import ch.netzkonzept.medidata.data.TransmissionLogEntry;

public class MediDataBoxFacade {

	private String host;
	private String token;
	private String clientID;
	private CloseableHttpClient client;
	private Gson gson;

	private static Logger LOGGER = LoggerFactory.getLogger(MediDataService.class);

	public MediDataBoxFacade(String host, String token, String clientID, String certFile, String certKey)
			throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, Exception, FileNotFoundException, IOException {

		this.host = host;
		this.token = token;
		this.clientID = clientID;

		gson = new GsonBuilder().setPrettyPrinting().create();

		/*final SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustAllStrategy()).build();
		final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
				.setSslContext(sslcontext).setHostnameVerifier(new NoopHostnameVerifier()).build();
		final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
				.setSSLSocketFactory(sslSocketFactory).setMaxConnTotal(400).setMaxConnPerRoute(200)
				.setConnectionTimeToLive(TimeValue.ofSeconds(3)).build();*/
		
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(new FileInputStream(certFile), certKey.toCharArray());
		SSLContext sslcontext = SSLContexts.custom()
		        .loadKeyMaterial(keyStore, certKey.toCharArray())
		        .build();
		final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
				.setSslContext(sslcontext).setHostnameVerifier(new NoopHostnameVerifier()).build();
		final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
				.setSSLSocketFactory(sslSocketFactory).setMaxConnTotal(400).setMaxConnPerRoute(200)
				.setConnectionTimeToLive(TimeValue.ofSeconds(3)).build();		

		client = HttpClients.custom().setConnectionManager(cm).evictExpiredConnections().build();
	}

	public TransmissionLogEntry postUploads(File dataFile) throws IOException {
		return postUploads(dataFile, null);
	}

	public TransmissionLogEntry postUploads(File dataFile, File controlFile) throws IOException {
		LOGGER.info("Uploading invoice " + dataFile.getName());
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addBinaryBody("elauploadstream", dataFile, ContentType.APPLICATION_OCTET_STREAM, dataFile.getName());
		if (controlFile != null) {
			LOGGER.info("Uploading controlfile " + controlFile.getName());
			builder.addBinaryBody("elauploadinfo", controlFile, ContentType.APPLICATION_JSON, controlFile.getName());
		}

		String result = Request.post("https://" + host + "/ela/uploads")
				.addHeader("X-CLIENT-ID", clientID)
				.addHeader("Authorization", "Basic " + token)
				.body(builder.build())
				.execute(client)
				.returnContent()
				.asString();

		TransmissionLogEntry transLog = gson.fromJson(result, TransmissionLogEntry.class);

		return (transLog);
	}

	public TransmissionLogEntry getStatus(String transmissionReference) throws IOException {
		LOGGER.info("Get transmission status for reference " + transmissionReference);
		String result = Request.get("https://" + host + "/ela/uploads/" + transmissionReference + "/status")
				.addHeader("X-CLIENT-ID", clientID).addHeader("Authorization", "Basic " + token).execute(client)
				.returnContent().asString(StandardCharsets.UTF_8);

		TransmissionLogEntry transLog = gson.fromJson(result, TransmissionLogEntry.class);

		return (transLog);
	}

	public DocumentListEntry[] getDocumentsList() throws IOException {
		LOGGER.info("Get documents list");
		String result = Request.get("https://" + host + "/ela/downloads?limit=1000")
				.addHeader("X-CLIENT-ID", clientID).addHeader("Authorization", "Basic " + token).execute(client)
				.returnContent().asString(StandardCharsets.UTF_8);

		DocumentListEntry[] docList = gson.fromJson(result, DocumentListEntry[].class);
		return docList;
	}

	public DocumentListEntry getDocument(DocumentListEntry doc) throws IOException {
		LOGGER.info("Get document " + doc.getTransmissionReference());
		String result = Request.get("https://" + host + "/ela/downloads/" + doc.getTransmissionReference())
				.addHeader("X-CLIENT-ID", clientID).addHeader("Authorization", "Basic " + token)
				.addHeader("Accept", "application/octet-stream").execute(client).returnContent()
				.asString(StandardCharsets.UTF_8);
		doc.setDocument(result);
		return doc;
	}

	public void confirmDocument(DocumentListEntry doc) throws IOException {
		LOGGER.info("Confirming document " + doc.getTransmissionReference());
		Request.put("https://" + host + "/ela/downloads/" + doc.getTransmissionReference() + "/status")
				.addHeader("X-CLIENT-ID", clientID).addHeader("Authorization", "Basic " + token)
				.bodyString("{\"status\": \"CONFIRMED\"}", ContentType.APPLICATION_JSON).execute(client);
	}

	public MessageLogEntry[] getMessages() throws IOException {
		LOGGER.info("Get messages");
		String result = Request.get("https://" + host + "/ela/notifications?limit=1000")
				.addHeader("X-CLIENT-ID", clientID).addHeader("Authorization", "Basic " + token).execute(client)
				.returnContent().asString(StandardCharsets.UTF_8);

		MessageLogEntry[] msgLog = gson.fromJson(result, MessageLogEntry[].class);

		for (MessageLogEntry msg : msgLog) {
			if (!msg.isRead()) {
				LOGGER.info("Confirming new unread message " + msg.getId());
				result = Request.put("https://" + host + "/ela/notifications/" + msg.getId() + "/status")
						.addHeader("X-CLIENT-ID", clientID).addHeader("Authorization", "Basic " + token)
						.bodyString("{\"notificationFetched\": true}", ContentType.APPLICATION_JSON).execute(client)
						.returnContent().asString();
			}

		}

		return msgLog;
	}

	public Participant[] getParticipants() throws IOException {

		String result = Request.get("https://" + host + "/ela/participants?limit=1000")
				.addHeader("X-CLIENT-ID", clientID)
				.addHeader("Authorization", "Basic " + token)
				.execute(client)
				.returnContent().asString(StandardCharsets.UTF_8);

		Participant[] participantDir = gson.fromJson(result, Participant[].class);

		return participantDir;
	}

}
