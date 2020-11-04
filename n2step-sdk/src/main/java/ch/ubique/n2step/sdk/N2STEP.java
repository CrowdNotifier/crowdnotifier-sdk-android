package ch.ubique.n2step.sdk;

import android.content.Context;

import java.util.List;

import com.google.gson.Gson;

import ch.ubique.n2step.sdk.model.Exposure;
import ch.ubique.n2step.sdk.model.Payload;
import ch.ubique.n2step.sdk.model.ProblematicEventInfo;
import ch.ubique.n2step.sdk.model.VenueInfo;
import ch.ubique.n2step.sdk.storage.VenueVisitStorage;
import ch.ubique.n2step.sdk.utils.CryptoUtils;
import ch.ubique.n2step.sdk.utils.QrUtils;

public class N2STEP {

	/**
	 * @param qrCodeData
	 * @return A {@link VenueInfo} if the input qrCodeData was a valid qrCode, null otherwise.
	 */
	public static VenueInfo getInfo(String qrCodeData) {
		return QrUtils.getQrInfo(qrCodeData);
	}


	public static long addVenueVisit(long arrivalTime, long departureTime, byte[] notificationKey, byte[] venuePublicKey,
			Context context) {

		CryptoUtils crypto = CryptoUtils.getInstance();

		byte[] ephemeralSecretKey = crypto.getRandomEphemeralSecretKey();
		byte[] ephemeralPublicKey = crypto.computeEphemeralPublicKey(ephemeralSecretKey);
		byte[] sharedKey = crypto.computeSharedKey(venuePublicKey, ephemeralSecretKey);

		String payload = new Gson().toJson(new Payload(arrivalTime, departureTime, notificationKey));
		byte[] encryptedPayload = crypto.encryptMessage(payload, venuePublicKey);

		return VenueVisitStorage.getInstance(context).addEntry(arrivalTime, ephemeralPublicKey, sharedKey, encryptedPayload);
	}

	public static List<Exposure> checkForMatches(List<ProblematicEventInfo> problematicEventInfos) {
		//TODO implement
		/*
		1. Go through all check ins overlapping with the problematicEvent duration and check if our public key to the power of
		the published secret key is equals the shared key (pk^skV == sharedKey)
		2. If so, decrypt the check-in and check-out DB entry, use the notification_key to decrypt the message and create an
		Exposure
		object and add it to the return list.
		 */
		return null;
	}

	public static void cleanupOldData(int maxDaysToKeep) {
		//TODO implement
	}

}
