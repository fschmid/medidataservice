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

public class Participant {

	private String glnParticipant;
	private String glnReceiver;
	private String name;
	private String street;
	private String zipCode;
	private String town;
	private Integer[] lawTypes;
	private Integer bagNumber;
	private boolean tgTpChange;
	private boolean additionalCosts;
	private Integer maxReceive;
	private String[] notSupported;
	private boolean tgAllowed;

	public String getGlnParticipant() {
		return glnParticipant;
	}

	public void setGlnParticipant(String glnParticipant) {
		this.glnParticipant = glnParticipant;
	}

	public String getGlnReceiver() {
		return glnReceiver;
	}

	public void setGlnReceiver(String glnReceiver) {
		this.glnReceiver = glnReceiver;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getTown() {
		return town;
	}

	public void setTown(String town) {
		this.town = town;
	}

	public Integer[] getLawTypes() {
		return lawTypes;
	}

	public void setLawTypes(Integer[] lawTypes) {
		this.lawTypes = lawTypes;
	}

	public Integer getBagNumber() {
		return bagNumber;
	}

	public void setBagNumber(Integer bagNumber) {
		this.bagNumber = bagNumber;
	}

	public boolean isTgTpChange() {
		return tgTpChange;
	}

	public void setTgTpChange(boolean tgTpChange) {
		this.tgTpChange = tgTpChange;
	}

	public boolean isAdditionalCosts() {
		return additionalCosts;
	}

	public void setAdditionalCosts(boolean additionalCosts) {
		this.additionalCosts = additionalCosts;
	}

	public Integer getMaxReceive() {
		return maxReceive;
	}

	public void setMaxReceive(Integer maxReceive) {
		this.maxReceive = maxReceive;
	}

	public String[] getNotSupported() {
		return notSupported;
	}

	public void setNotSupported(String[] notSupported) {
		this.notSupported = notSupported;
	}

	public boolean isTgAllowed() {
		return tgAllowed;
	}

	public void setTgAllowed(boolean tgAllowed) {
		this.tgAllowed = tgAllowed;
	}

}