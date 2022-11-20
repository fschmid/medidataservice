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

public class DocumentListEntry {

	private String transmissionReference;
	private String documentReference;
	private String correlationReference;
	private String senderGln;
	private String docType;
	private Long fileSize;
	private String modus;
	private String status;
	private String created;
	private String document;

	public String getDocument() {
		return document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getTransmissionReference() {
		return transmissionReference;
	}

	public void setTransmissionReference(String transmissionReference) {
		this.transmissionReference = transmissionReference;
	}

	public String getDocumentReference() {
		return documentReference;
	}

	public void setDocumentReference(String documentReference) {
		this.documentReference = documentReference;
	}

	public String getCorrelationReference() {
		return correlationReference;
	}

	public void setCorrelationReference(String correlationReference) {
		this.correlationReference = correlationReference;
	}

	public String getSenderGln() {
		return senderGln;
	}

	public void setSenderGln(String senderGln) {
		this.senderGln = senderGln;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getModus() {
		return modus;
	}

	public void setModus(String modus) {
		this.modus = modus;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

}
