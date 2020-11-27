package org.crowdnotifier.android.sdk.model;

public class ProblematicEventInfo {

	private byte[] secretKey;
	private long startTimestamp;
	private long endTimestamp;
	private byte[] encryptedMessage;
	private byte[] nonce;

	public ProblematicEventInfo(byte[] secretKey, long startTimestamp, long endTimestamp, byte[] encryptedMessage, byte[] nonce) {
		this.secretKey = secretKey;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.encryptedMessage = encryptedMessage;
		this.nonce = nonce;
	}

	public byte[] getSecretKey() {
		return secretKey;
	}

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public long getEndTimestamp() {
		return endTimestamp;
	}

	public byte[] getEncryptedMessage() {
		return encryptedMessage;
	}

	public byte[] getNonce() {
		return nonce;
	}

}
