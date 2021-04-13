package org.crowdnotifier.android.sdk.utils;

import android.util.Base64;

import com.google.protobuf.InvalidProtocolBufferException;

import org.crowdnotifier.android.sdk.model.QrV2;
import org.crowdnotifier.android.sdk.model.QrV3;
import org.crowdnotifier.android.sdk.model.VenueInfo;

import static android.util.Base64.NO_PADDING;

public class QrUtils {

	private static final String QR_CODE_VERSION_2 = "2";
	private static final String QR_CODE_VERSION_3 = "3";

	//TODO: Would it maybe make sense to pass a list of Prefixes (for interoperability reasons)
	public static VenueInfo getQrInfo(String qrCodeString, String expectedQrCodePrefix) throws QRException {

		String[] fragmentSplit = qrCodeString.split("#");
		if (fragmentSplit.length != 2) throw new InvalidQRCodeFormatException();
		String[] urlSplits = fragmentSplit[0].split("\\?v=");
		if (urlSplits.length != 2) throw new InvalidQRCodeFormatException();
		String urlPrefix = urlSplits[0];
		String version = urlSplits[1];
		if (!urlPrefix.equals(expectedQrCodePrefix)) throw new InvalidQRCodeFormatException();

		if (QR_CODE_VERSION_2.equals(version)) {
			return getVenueInfoFromQrCodeV2(fragmentSplit[1]);
		} else if (QR_CODE_VERSION_3.equals(version)) {
			return getVenueInfoFromQrCodeV3(fragmentSplit[1]);
		} else {
			throw new InvalidQRCodeVersionException();
		}
	}

	private static VenueInfo getVenueInfoFromQrCodeV3(String qrCodeString) throws QRException {
		try {
			int decodeFlags = Base64.NO_WRAP | Base64.URL_SAFE | NO_PADDING;
			byte[] decoded = Base64.decode(qrCodeString, decodeFlags);
			QrV3.QRCodePayload qrCodeEntry = QrV3.QRCodePayload.parseFrom(decoded);
			QrV3.TraceLocation locationData = qrCodeEntry.getLocationData();
			QrV3.CrowdNotifierData crowdNotifierData = qrCodeEntry.getCrowdNotifierData();

			//TODO: What if this payload is not NotifyMeData?
			QrV3.NotifyMeLocationData notifyMeLocationData = QrV3.NotifyMeLocationData.parseFrom(qrCodeEntry.getCountryData());

			if (System.currentTimeMillis() < locationData.getStartTimestamp()) {
				throw new NotYetValidException();
			}
			if (System.currentTimeMillis() > locationData.getEndTimestamp()) {
				throw new NotValidAnymoreException();
			}

			CryptoUtils.NoncesAndNotificationKey cryptoData = CryptoUtils.getInstance().getNoncesAndNotificationKey(qrCodeEntry);

			return new VenueInfo(locationData.getDescription(), locationData.getAddress(), notifyMeLocationData.getRoom(),
					cryptoData.notificationKey, notifyMeLocationData.getType(), crowdNotifierData.getPublicKey().toByteArray(),
					cryptoData.nonce1, cryptoData.nonce2, locationData.getStartTimestamp(), locationData.getEndTimestamp(),
					qrCodeEntry.toByteArray());
		} catch (InvalidProtocolBufferException e) {
			throw new InvalidQRCodeFormatException();
		}
	}

	private static VenueInfo getVenueInfoFromQrCodeV2(String qrCodeString) throws QRException {
		try {
			int decodeFlags = Base64.NO_WRAP | Base64.URL_SAFE | NO_PADDING;
			byte[] decoded = Base64.decode(qrCodeString, decodeFlags);
			QrV2.QRCodeEntry qrCodeEntry = QrV2.QRCodeEntry.parseFrom(decoded);
			QrV2.QRCodeContent qrCode = qrCodeEntry.getData();

			if (System.currentTimeMillis() < qrCode.getValidFrom()) {
				throw new NotYetValidException();
			}
			if (System.currentTimeMillis() > qrCode.getValidTo()) {
				throw new NotValidAnymoreException();
			}

			return new VenueInfo(qrCode.getName(), qrCode.getLocation(), qrCode.getRoom(),
					qrCode.getNotificationKey().toByteArray(), QrV3.VenueType.forNumber(qrCode.getVenueTypeValue()),
					qrCodeEntry.getMasterPublicKey().toByteArray(), qrCodeEntry.getEntryProof().getNonce1().toByteArray(),
					qrCodeEntry.getEntryProof().getNonce2().toByteArray(), qrCode.getValidFrom(), qrCode.getValidTo(), null);
		} catch (InvalidProtocolBufferException e) {
			throw new InvalidQRCodeFormatException();
		}
	}

	public static class QRException extends Exception { }


	public static class NotYetValidException extends QRException { }


	public static class NotValidAnymoreException extends QRException { }


	public static class InvalidQRCodeFormatException extends QRException { }


	public static class InvalidQRCodeVersionException extends QRException { }

}
