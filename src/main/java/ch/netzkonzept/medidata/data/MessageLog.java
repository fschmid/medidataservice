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

package ch.netzkonzept.medidata.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MessageLog {

	private static Logger LOGGER = LoggerFactory.getLogger(MessageLog.class);

	private Gson gson;
	private ArrayList<MessageLogEntry> msgLog;
	private File msgLogFile;

	public MessageLog(File messageLog) throws FileNotFoundException, UnsupportedEncodingException {
		msgLogFile = messageLog;
		gson = new GsonBuilder().setPrettyPrinting().create();
		if (!msgLogFile.exists()) {
			LOGGER.warn("Local message box not found, creating new local message box");
			PrintWriter writer = new PrintWriter(msgLogFile, "UTF-8");
			writer.println("[]");
			writer.close();
		}
		Reader reader = new FileReader(msgLogFile);
		MessageLogEntry[] messageLogArray = gson.fromJson(reader, MessageLogEntry[].class);
		msgLog = new ArrayList<MessageLogEntry>(Arrays.asList(messageLogArray));
	}

	public void upsertMessage(MessageLogEntry message) throws IOException {
		boolean found = false;
		for (MessageLogEntry le : msgLog) {
			if (le.getId().equals(message.getId())) {
				le = message;
				found = true;
				break;
			}

		}
		if (!found) {
			LOGGER.info("Adding new message to message log with id " + message.getId());
			msgLog.add(message);
		}
		flush();
	}

	private void flush() throws IOException {
		Writer writer = new FileWriter(msgLogFile);
		gson.toJson(msgLog, writer);
		writer.close();
	}

}
