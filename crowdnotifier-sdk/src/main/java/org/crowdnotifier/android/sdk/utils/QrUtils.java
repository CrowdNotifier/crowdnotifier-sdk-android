package org.crowdnotifier.android.sdk.utils;

import android.util.Base64;

import com.google.protobuf.InvalidProtocolBufferException;

import org.crowdnotifier.android.sdk.model.v2.ProtoV2;
import org.crowdnotifier.android.sdk.model.v3.ProtoV3;
import org.crowdnotifier.android.sdk.model.VenueInfo;

import static android.util.Base64.NO_PADDING;

public class QrUtils {

	private static final String QR_CODE_VERSION_2 = "2";
	private static final String QR_CODE_VERSION_3 = "3";

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
			ProtoV3.QRCodePayload qrCodeEntry = ProtoV3.QRCodePayload.parseFrom(decoded);
			ProtoV3.TraceLocation locationData = qrCodeEntry.getLocationData();
			ProtoV3.CrowdNotifierData crowdNotifierData = qrCodeEntry.getCrowdNotifierData();

			if (System.currentTimeMillis() / 1000 < locationData.getStartTimestamp()) {
				throw new NotYetValidException();
			}
			if (System.currentTimeMillis() / 1000 > locationData.getEndTimestamp()) {
				throw new NotValidAnymoreException();
			}

			CryptoUtils.NoncesAndNotificationKey cryptoData = CryptoUtils.getInstance().getNoncesAndNotificationKey(qrCodeEntry);

			return new VenueInfo(locationData.getDescription(), locationData.getAddress(), cryptoData.notificationKey,
					crowdNotifierData.getPublicKey().toByteArray(), cryptoData.nonce1, cryptoData.nonce2,
					locationData.getStartTimestamp(), locationData.getEndTimestamp(), qrCodeEntry.toByteArray(),
					qrCodeEntry.getCountryData().toByteArray());
		} catch (InvalidProtocolBufferException e) {
			throw new InvalidQRCodeFormatException();
		}
	}

	private static VenueInfo getVenueInfoFromQrCodeV2(String qrCodeString) throws QRException {
		try {
			int decodeFlags = Base64.NO_WRAP | Base64.URL_SAFE | NO_PADDING;
			byte[] decoded = Base64.decode(qrCodeString, decodeFlags);
			ProtoV2.QRCodeEntry qrCodeEntry = ProtoV2.QRCodeEntry.parseFrom(decoded);
			ProtoV2.QRCodeContent qrCode = qrCodeEntry.getData();

			if (System.currentTimeMillis() < qrCode.getValidFrom()) {
				throw new NotYetValidException();
			}
			if (System.currentTimeMillis() > qrCode.getValidTo()) {
				throw new NotValidAnymoreException();
			}

			ProtoV3.NotifyMeLocationData notifyMeLocationData = ProtoV3.NotifyMeLocationData.newBuilder()
					.setRoom(qrCode.getRoom())
					.setVersion(2)
					.setTypeValue(qrCode.getVenueTypeValue())
					.build();

			return new VenueInfo(qrCode.getName(), qrCode.getLocation(), qrCode.getNotificationKey().toByteArray(),
					qrCodeEntry.getMasterPublicKey().toByteArray(), qrCodeEntry.getEntryProof().getNonce1().toByteArray(),
					qrCodeEntry.getEntryProof().getNonce2().toByteArray(), qrCode.getValidFrom(), qrCode.getValidTo(), null,
					notifyMeLocationData.toByteArray());
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
