package org.crowdnotifier.android.sdk.model;

public class UserUploadInfo {
	private byte[] preId;
	private byte[] timeKey;
	private byte[] notificationKey;
	private long intervalStartMs;
	private long intervalEndMs;

	public UserUploadInfo(byte[] preId, byte[] timeKey, byte[] notificationKey, long intervalStartMs, long intervalEndMs) {
		this.preId = preId;
		this.timeKey = timeKey;
		this.notificationKey = notificationKey;
		this.intervalStartMs = intervalStartMs;
		this.intervalEndMs = intervalEndMs;
	}

	public byte[] getPreId() {
		return preId;
	}

	public byte[] getTimeKey() {
		return timeKey;
	}

	public byte[] getNotificationKey() {
		return notificationKey;
	}

	public long getIntervalStartMs() {
		return intervalStartMs;
	}

	public long getIntervalEndMs() {
		return intervalEndMs;
	}

}
