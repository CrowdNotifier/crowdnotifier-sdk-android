package org.crowdnotifier.android.sdk.model;

public class ProblematicEventInfo {

	private byte[] identity;
	private byte[] secretKeyForIdentity;
	private long startTimestamp;
	private long endTimestamp;
	byte[] encryptedAssociatedData;
	byte[] cipherTextNonce;

	public ProblematicEventInfo(byte[] identity, byte[] secretKeyForIdentity, long startTimestamp, long endTimestamp,
			byte[] encryptedAssociatedData, byte[] cipherTextNonce) {
		this.identity = identity;
		this.secretKeyForIdentity = secretKeyForIdentity;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.encryptedAssociatedData = encryptedAssociatedData;
		this.cipherTextNonce = cipherTextNonce;
	}

	public byte[] getIdentity() {
		return identity;
	}

	public byte[] getSecretKeyForIdentity() {
		return secretKeyForIdentity;
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public long getEndTimestamp() {
		return endTimestamp;
	}

	public byte[] getEncryptedAssociatedData() {
		return encryptedAssociatedData;
	}

	public byte[] getCipherTextNonce() {
		return cipherTextNonce;
	}

}
