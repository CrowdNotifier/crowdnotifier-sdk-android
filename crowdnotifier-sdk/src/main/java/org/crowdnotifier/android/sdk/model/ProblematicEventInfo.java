package org.crowdnotifier.android.sdk.model;

public class ProblematicEventInfo {

	private byte[] secretKey;
	private long startTimestamp;
	private long endTimestamp;
	private byte[] encryptedMessage;

	public ProblematicEventInfo(byte[] secretKey, long startTimestamp, long endTimestamp, byte[] encryptedMessage) {
		this.secretKey = secretKey;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.encryptedMessage = encryptedMessage;
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

}
