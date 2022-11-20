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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediDataServiceCLI {

	private static Logger LOGGER = LoggerFactory.getLogger(MediDataServiceCLI.class);

	public static void main(String[] args) {

		ClassLoader CLDR = MediDataServiceCLI.class.getClassLoader();
		try {
			PropertyConfigurator.configure(CLDR.getResourceAsStream("resources/log4j.properties"));
		} catch (Exception e) {
		}

		Options options = new Options();
		CommandLineParser cmdParser = new DefaultParser();
		CommandLine cmd = null;

		MediDataService mds = null;

		options.addRequiredOption("config", null, true, "Path to config file");
		options.addOption("sendInvoices", null, false, "Send new invoices to Medidata Box");
		options.addOption("receiveDocuments", null, false, "Receive and save new documents");
		options.addOption("updateTransmissionData", null, false, "Update the Transmissions database");
		options.addOption("updateMessageData", null, false, "Update the Messages database");
		options.addOption("updateParticipants", null, false, "Update the Participants directory");

		try {
			cmd = cmdParser.parse(options, args);
		} catch (ParseException e1) {
			printHelpMessage(options);
			System.exit(1);
		}

		if (cmd.getOptions().length < 2) {
			printHelpMessage(options);
			System.exit(1);
		}

		try {
			mds = new MediDataService(new File(cmd.getOptionValue("config")));
		} catch (Exception e) {
			LOGGER.error("Error initialising Medi Data Service. Check config file or connection status. Exiting");
			LOGGER.debug(MediDataService.getStackTrace(e));
			System.exit(1);
		}

		LOGGER.info("Medidata 2.0 Services for Elexis - starting up");

		for (Option o : cmd.getOptions()) {
			switch (o.getOpt()) {
			case "sendInvoices":
				mds.sendDocuments();
				break;
			case "updateTransmissionData":
				mds.updateTransmissionStatus();
				;
				break;
			case "receiveDocuments":
				mds.receiveDocuments();
				break;
			case "updateMessageData":
				mds.upsertMessages();
				break;
			case "updateParticipants":
				mds.upsertParticipants();
				break;
			}
		}
		LOGGER.info("have a nice day");
	}

	private static void printHelpMessage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		System.out.println("\nMedidata 2.0 Services for Elexis \n");
		formatter.printHelp("java -jar MDService.jar [Options]", options);
		System.out.println(
				"\nUsage examples:\nSending new invoices:\t\t\t\tjava -jar MDService.jar -config ./mdsconfig.conf -sendInvoices");
		System.out.println(
				"Updating transmission and messages database:\tjava -jar MDService.jar -config ./mdsconfig.conf -updateTransmissionData -updateMessageData\n");
	}

}
