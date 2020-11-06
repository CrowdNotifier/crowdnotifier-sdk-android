package ch.ubique.n2step.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ch.ubique.n2step.sdk.model.EncryptedVenueVisit;
import ch.ubique.n2step.sdk.model.Exposure;
import ch.ubique.n2step.sdk.model.Payload;
import ch.ubique.n2step.sdk.model.ProblematicEventInfo;
import ch.ubique.n2step.sdk.model.VenueInfo;
import ch.ubique.n2step.sdk.storage.ExposureStorage;
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
		EncryptedVenueVisit encryptedVenueVisit =
				crypto.getEncryptedVenueVisit(arrivalTime, departureTime, notificationKey, venuePublicKey);

		return VenueVisitStorage.getInstance(context).addEntry(encryptedVenueVisit);
	}

	public static List<Exposure> checkForMatches(List<ProblematicEventInfo> problematicEventInfos, Context context) {

		ArrayList<Exposure> newExposures = new ArrayList<>();
		ExposureStorage exposureStorage = ExposureStorage.getInstance(context);

		for (ProblematicEventInfo problematicEventInfo : problematicEventInfos) {

			List<Exposure> matches = CryptoUtils.getInstance().searchAndDecryptMatches(
					problematicEventInfo.getSecretKey(),
					VenueVisitStorage.getInstance(context).getEntries()
			);

			for (Exposure match : matches) {
				if (match.getStartTime() <= problematicEventInfo.getEndTimestamp() &&
						match.getEndTime() >= problematicEventInfo.getStartTimestamp()) {
					Exposure newExposure = new Exposure(match.getId(),
							Math.max(match.getStartTime(), problematicEventInfo.getStartTimestamp()),
							Math.min(match.getEndTime(), problematicEventInfo.getEndTimestamp()));
					boolean added = exposureStorage.addEntry(newExposure);
					if (added) newExposures.add(newExposure);
				}
			}
		}
		return newExposures;
	}

	public static List<Exposure> getExposures(Context context) {
		return ExposureStorage.getInstance(context).getEntries();
	}

	public static void cleanupOldData(Context context, int maxDaysToKeep) {
		VenueVisitStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
		ExposureStorage.getInstance(context).removeEntriesBefore(maxDaysToKeep);
	}

}
