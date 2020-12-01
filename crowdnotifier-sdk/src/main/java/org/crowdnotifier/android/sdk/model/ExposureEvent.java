package org.crowdnotifier.android.sdk.model;

public class ExposureEvent {
	private long id;
	private long startTimestamp;
	private long endTimestamp;
	private String message;

	public ExposureEvent(long id, long startTime, long endTime, String message) {
		this.id = id;
		this.startTimestamp = startTime;
		this.endTimestamp = endTime;
		this.message = message;
	}

	public long getId() {
		return id;
	}

	public long getStartTime() {
		return startTimestamp;
	}

	public long getEndTime() {
		return endTimestamp;
	}

	public String getMessage() {
		return message;
	}

	public void setId(long id) {
		this.id = id;
	}

}
