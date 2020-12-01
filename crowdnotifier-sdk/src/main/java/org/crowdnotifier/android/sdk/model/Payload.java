package org.crowdnotifier.android.sdk.model;

public class Payload {
	long arrivalTime;
	long departureTime;
	private byte[] notificationKey;

	public Payload(long arrivalTime, long departureTime, byte[] notificationKey) {
		this.arrivalTime = arrivalTime;
		this.departureTime = departureTime;
		this.notificationKey = notificationKey;
	}

	public long getArrivalTime() {
		return arrivalTime;
	}

	public long getDepartureTime() {
		return departureTime;
	}


	public byte[] getNotificationKey() {
		return notificationKey;
	}

}
