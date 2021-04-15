package org.crowdnotifier.android.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.crowdnotifier.android.sdk.model.EncryptedVenueVisit;
import org.crowdnotifier.android.sdk.model.ExposureEvent;
import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.crowdnotifier.android.sdk.utils.QrUtils;

public class CrowdNotifier {

	/**
	 * Given an qrCode String, this function tries to extract a VenueInfo object from it. The qrCode String should be of the
	 * following format: <prefix>?v=<qr-code-version>#<base64-encoded-protobuf>
	 * e.g. https://qr.notify-me.ch?v=3#<base64-encoded-protobuf>
	 *
	 * Set the expectedQrCodePrefix parameter to the <prefix> part of the qrCode String.
	 *
	 * @return A {@link VenueInfo} if the input qrCode was a valid qrCode, null otherwise.
	 */
	public static VenueInfo getVenueInfo(String qrCode, String expectedQrCodePrefix) throws QrUtils.QRException {
		return QrUtils.getQrInfo(qrCode, expectedQrCodePrefix);
	}


	/**
	 * Encrypts and stores a VenueVisit to EncryptedSharedPreferences.
	 *
	 * @return An id, which identifies the stored encrypted VenueVisit.
	 */
	public static long addCheckIn(long arrivalTime, long departureTime, VenueInfo venueInfo, Context context) {

		CryptoUtils crypto = CryptoUtils.getInstance();
		EncryptedVenueVisit encryptedVenueVisit = crypto.getEncryptedVenueVisit(arrivalTime, departureTime, venueInfo);

		return VenueVisitStorage.getInstance(context).addEntry(encryptedVenueVisit);
	}

	/**
	 * Updates a previously stored VenueVisit in EncryptedSharedPreferences.
	 *
	 * @return true if the update was successful (i.e. an encrypted Venue Visit with the provided id was found and updated), false
	 * otherwise.
	 */
	public static boolean updateCheckIn(long id, long arrivalTime, long departureTime, VenueInfo venueInfo, Context context) {

		CryptoUtils crypto = CryptoUtils.getInstance();
		EncryptedVenueVisit encryptedVenueVisit = crypto.getEncryptedVenueVisit(arrivalTime, departureTime, venueInfo);
		encryptedVenueVisit.setId(id);
		return VenueVisitStorage.getInstance(context).updateEntry(encryptedVenueVisit);
	}

	/**
	 * Checks whether any stored encrypted VenueVisit matches with a ProblematicEventInfo. A match means, that the the stored
	 * encrypted VenueVisit could be decrypted using the provided secretKeyForIdentity and identity inside the
	 * {@link ProblematicEventInfo}, and additionally the time intervals of the VenueVisit and the ProblematicEventInfo overlap.
	 *
	 * @return A list containing all {@link ExposureEvent} objects that matched with at least one of the provided
	 * ProblematicEventInfo's
	 */
	public static List<ExposureEvent> checkForMatches(List<ProblematicEventInfo> publishedSKs, Context context) {

		ArrayList<ExposureEvent> newExposureEvents = new ArrayList<>();
		ExposureStorage exposureStorage = ExposureStorage.getInstance(context);

		for (ProblematicEventInfo problematicEventInfo : publishedSKs) {

			List<ExposureEvent> matches = CryptoUtils.getInstance()
					.searchAndDecryptMatches(problematicEventInfo, VenueVisitStorage.getInstance(context).getEntries());

			for (ExposureEvent match : matches) {
				if (match.getStartTime() <= problematicEventInfo.getEndTimestamp() &&
						match.getEndTime() >= problematicEventInfo.getStartTimestamp()) {
					boolean added = exposureStorage.addEntry(match);
					if (added) newExposureEvents.add(match);
				}
			}
		}
		return newExposureEvents;
	}

	/**
	 * @return a list of all {@link ExposureEvent} objects that have previously matched with a provided ProblematicEventInfo in the
	 * checkForMatches function and have not been removed yet.
	 */
	public static List<ExposureEvent> getExposureEvents(Context context) {
		return ExposureStorage.getInstance(context).getEntries();
	}

	/**
	 * Deletes all ExposureEvents that are older than maxDaysToKeep days.
	 */
	public static void cleanUpOldData(Context context, int maxDaysToKeep) {
		VenueVisitStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
		ExposureStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
	}

	/**
	 * Removes an ExposureEvent with the given exposureId.
	 */
	public static void removeExposure(Context context, long exposureId) {
		ExposureStorage.getInstance(context).removeExposure(exposureId);
	}

}
