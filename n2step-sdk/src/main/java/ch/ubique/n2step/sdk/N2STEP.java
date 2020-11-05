package ch.ubique.n2step.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ch.ubique.n2step.sdk.model.EncryptedVenueVisit;
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
		EncryptedVenueVisit encryptedVenueVisit =
				crypto.getEncryptedVenueVisit(arrivalTime, departureTime, notificationKey, venuePublicKey);

		return VenueVisitStorage.getInstance(context).addEntry(encryptedVenueVisit);
	}

	public static List<Exposure> checkForMatches(List<ProblematicEventInfo> problematicEventInfos, Context context) {

		List<Exposure> result = new ArrayList<>();

		for (ProblematicEventInfo problematicEventInfo : problematicEventInfos) {

			List<Payload> matches = CryptoUtils.getInstance().searchAndDecryptMatches(
					problematicEventInfo.getSecretKey(),
					VenueVisitStorage.getInstance(context).getEntries()
			);

			for (Payload match : matches) {
				if ((match.getArrivalTime() <= problematicEventInfo.getEndTimestamp() &&
						match.getDepartureTime() >= problematicEventInfo.getStartTimestamp())
						|| (match.getArrivalTime() <= problematicEventInfo.getEndTimestamp() &&
						match.getDepartureTime() >= problematicEventInfo.getStartTimestamp())) {

					result.add(new Exposure(0, Math.max(match.getArrivalTime(), problematicEventInfo.getStartTimestamp()),
							Math.min(match.getDepartureTime(), problematicEventInfo.getEndTimestamp())));
				}
			}
		}

		return result;
	}

	public static void cleanupOldData(int maxDaysToKeep) {
		//TODO implement
	}

}
