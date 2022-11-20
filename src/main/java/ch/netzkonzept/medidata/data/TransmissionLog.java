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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TransmissionLog {

	private static Logger LOGGER = LoggerFactory.getLogger(TransmissionLog.class);

	private Gson gson;
	private ArrayList<TransmissionLogEntry> transLog;
	private File transLogFile;

	public TransmissionLog(File transmissionLog) throws FileNotFoundException, UnsupportedEncodingException {
		transLogFile = transmissionLog;
		gson = new GsonBuilder().setPrettyPrinting().create();
		if (!transLogFile.exists()) {
			LOGGER.warn("Local transmission log not found, creating new local transmission log");
			PrintWriter writer = new PrintWriter(transLogFile, "UTF-8");
			writer.println("[]");
			writer.close();
		}
		Reader reader = new FileReader(transmissionLog);
		TransmissionLogEntry[] transLogArray = gson.fromJson(reader, TransmissionLogEntry[].class);
		transLog = new ArrayList<TransmissionLogEntry>(Arrays.asList(transLogArray));
	}

	public void updateTransmission(TransmissionLogEntry transmission) throws IOException {
		for (TransmissionLogEntry le : transLog) {
			if (le.getTransmissionReference().equals(transmission.getTransmissionReference())) {
				le.setStatus(transmission.getStatus());
			}
		}
		flush();
	}

	public List<TransmissionLogEntry> getTransmissionsToUpdate() {
		List<TransmissionLogEntry> result = new ArrayList<TransmissionLogEntry>();
		for (TransmissionLogEntry le : transLog) {
			if (le.getStatus() == null || le.getStatus().isEmpty()
					|| le.getStatus().equals(TransmissionLogEntry.PROCESSING)) {
				result.add(le);
			}
		}
		return result;
	}

	public void insertTransmission(TransmissionLogEntry transmission) throws IOException {
		transLog.add(transmission);
		flush();
	}

	private void flush() throws IOException {
		Writer writer = new FileWriter(transLogFile);
		gson.toJson(transLog, writer);
		writer.close();
	}

}
