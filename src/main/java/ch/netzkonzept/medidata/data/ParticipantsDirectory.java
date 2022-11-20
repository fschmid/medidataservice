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

public class ParticipantsDirectory {

	private static Logger LOGGER = LoggerFactory.getLogger(ParticipantsDirectory.class);

	private Gson gson;
	private ArrayList<Participant> participantsDir;
	private File participantsDirFile;

	public ParticipantsDirectory(File participantsDirectoryFile)
			throws FileNotFoundException, UnsupportedEncodingException {
		participantsDirFile = participantsDirectoryFile;
		gson = new GsonBuilder().setPrettyPrinting().create();
		if (!participantsDirFile.exists()) {
			LOGGER.warn("Local participants directory not found, creating new local directory");
			PrintWriter writer = new PrintWriter(participantsDirFile, "UTF-8");
			writer.println("[]");
			writer.close();
		}
		Reader reader = new FileReader(participantsDirFile);
		Participant[] participantsArray = gson.fromJson(reader, Participant[].class);
		participantsDir = new ArrayList<Participant>(Arrays.asList(participantsArray));
	}

	public void upsertParticipant(Participant participant) throws IOException {
		boolean found = false;
		for (Participant p : participantsDir) {
			if (p.getGlnParticipant().equals(participant.getGlnParticipant())) {
				p = participant;
				found = true;
				break;
			}

		}
		if (!found) {
			participantsDir.add(participant);
		}
		flush();
	}

	public boolean checkGLN(String GLN) {
		boolean ok = false;
		for (Participant p : participantsDir) {
			if (p.getGlnParticipant().equals(GLN)) {
				ok = true;
				break;
			}

		}
		return ok;
	}

	private void flush() throws IOException {
		Writer writer = new FileWriter(participantsDirFile);
		gson.toJson(participantsDir, writer);
		writer.close();
	}

}
