package org.crowdnotifier.android.sdk.model;

public class ExposureEvent {
	private long id;
	private long startTimestamp;
	private long endTimestamp;

	public ExposureEvent(long id, long startTime, long endTime) {
		this.id = id;
		this.startTimestamp = startTime;
		this.endTimestamp = endTime;
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

	public void setId(long id) {
		this.id = id;
	}

}
