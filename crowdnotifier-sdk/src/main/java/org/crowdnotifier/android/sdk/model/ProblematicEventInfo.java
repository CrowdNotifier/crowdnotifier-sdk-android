package org.crowdnotifier.android.sdk.model;

public class ProblematicEventInfo {

	private byte[] secretKey;
	private long startTimestamp;
	private long endTimestamp;

	public ProblematicEventInfo(byte[] secretKey, long startTimestamp, long endTimestamp) {
		this.secretKey = secretKey;
		this.startTimestamp = startTimestamp;
		this.endTimestamp = endTimestamp;
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

}
