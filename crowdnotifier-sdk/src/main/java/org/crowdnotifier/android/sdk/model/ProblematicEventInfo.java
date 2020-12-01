package org.crowdnotifier.android.sdk.model;

public class ProblematicEventInfo {

	private byte[] secretKey;
	private long startTimestamp;
	private long endTimestamp;
	private byte[] encryptedMessage;
	private byte[] nonce;
	private byte[] r2;

	public ProblematicEventInfo(byte[] secretKey, long startTimestamp, long endTimestamp, byte[] encryptedMessage, byte[] nonce,
			byte[] r2) {
		this.secretKey = secretKey;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.encryptedMessage = encryptedMessage;
		this.nonce = nonce;
		this.r2 = r2;
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

	public byte[] getR2() {
		return r2;
	}

}
