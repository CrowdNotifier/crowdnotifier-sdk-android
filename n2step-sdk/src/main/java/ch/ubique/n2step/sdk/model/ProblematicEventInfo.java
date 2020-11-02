package ch.ubique.n2step.sdk.model;

public class ProblematicEventInfo {
	private String secretKey;
	private long startTimestamp;
	private long endTimestamp;
	private String encryptedMessage;

	public ProblematicEventInfo(String secretKey, long startTimestamp, long endTimestamp, String encryptedMessage) {
		this.secretKey = secretKey;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
		this.encryptedMessage = encryptedMessage;
	}

	public String getSecretKey() {
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
