package org.crowdnotifier.android.sdk.model;

public class ExposureEvent {
	private long id;
	private long startTimestamp;
	private long endTimestamp;
	private String message;
	private byte[] countryData;

	public ExposureEvent(long id, long startTime, long endTime, String message, byte[] countryData) {
		this.id = id;
		this.startTimestamp = startTime;
		this.endTimestamp = endTime;
		this.message = message;
		this.countryData = countryData;
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

	public byte[] getCountryData() {
		return countryData;
	}

	public void setId(long id) {
		this.id = id;
	}

}
