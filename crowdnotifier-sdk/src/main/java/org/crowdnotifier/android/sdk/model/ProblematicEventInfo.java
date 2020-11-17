package org.crowdnotifier.android.sdk.model;

public class ProblematicEventInfo {

	private byte[] secretKey;
	private long startTimestamp;
	private long endTimestamp;
	private String encryptedMessage;

	public ProblematicEventInfo(byte[] secretKey, long startTimestamp, long endTimestamp, String encryptedMessage) {
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

	public String getEncryptedMessage() {
		return encryptedMessage;
	}

}
