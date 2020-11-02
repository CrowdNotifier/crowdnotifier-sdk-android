package ch.ubique.n2step.sdk.model;

public class Exposure {
	private long id;
	private long startTimestamp;
	private long endTimestamp;
	private String message;

	public Exposure(long id, long startTime, long endTime, String message) {
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

}
