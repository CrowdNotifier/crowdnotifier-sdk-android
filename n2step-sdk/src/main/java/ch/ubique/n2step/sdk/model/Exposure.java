package ch.ubique.n2step.sdk.model;

public class Exposure {
	private long id;
	private long startTimestamp;
	private long endTimestamp;

	public Exposure(long id, long startTime, long endTime) {
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
