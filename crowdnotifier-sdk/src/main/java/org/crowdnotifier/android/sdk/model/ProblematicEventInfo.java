package org.crowdnotifier.android.sdk.model;

public class ProblematicEventInfo {

	private byte[] identity;
	private byte[] secretKeyForIdentity;
	byte[] encryptedAssociatedData;
	byte[] cipherTextNonce;
	private DayDate dayDate;

	public ProblematicEventInfo(byte[] identity, byte[] secretKeyForIdentity, byte[] encryptedAssociatedData,
			byte[] cipherTextNonce, DayDate dayDate) {
		this.identity = identity;
		this.secretKeyForIdentity = secretKeyForIdentity;
		this.encryptedAssociatedData = encryptedAssociatedData;
		this.cipherTextNonce = cipherTextNonce;
		this.dayDate = dayDate;
	}

	public byte[] getIdentity() {
		return identity;
	}

	public byte[] getSecretKeyForIdentity() {
		return secretKeyForIdentity;
	}

	public byte[] getEncryptedAssociatedData() {
		return encryptedAssociatedData;
	}

	public byte[] getCipherTextNonce() {
		return cipherTextNonce;
	}

	public DayDate getDayDate() {
		return dayDate;
	}

}
