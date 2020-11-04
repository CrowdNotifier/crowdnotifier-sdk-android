package ch.ubique.n2step.sdk.utils;

import android.util.Base64;

import com.google.protobuf.InvalidProtocolBufferException;

import ch.ubique.n2step.sdk.model.VenueInfo;
import ch.ubique.n2step.sdk.model.Qr;

public class QrUtils {

	private static final String QR_CODE_PREFIX = "https://qr-dev.n2s.ch";

	public static VenueInfo getQrInfo(String qrCodeString) {

		String[] splits = qrCodeString.split("#");
		if (splits.length != 2) return null;
		if (!splits[0].equals(QR_CODE_PREFIX)) return null;

		try {
			int decodeFlags = Base64.NO_WRAP | Base64.URL_SAFE;
			byte[] decoded = Base64.decode(splits[1], decodeFlags);
			Qr.QRCode qrCode = Qr.QRCode.parseFrom(decoded);
			//TODO: Check signature of QR Code
			return new VenueInfo(qrCode.getName(), qrCode.getLocation(), qrCode.getRoom(), qrCode.getVenueType(),
					qrCode.getPublicKey().toByteArray(), qrCode.getNotificationKey().toByteArray());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return null;
		}
	}

}
