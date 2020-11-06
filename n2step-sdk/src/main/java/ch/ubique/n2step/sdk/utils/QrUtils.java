package ch.ubique.n2step.sdk.utils;

import android.util.Base64;

import com.google.protobuf.InvalidProtocolBufferException;

import ch.ubique.n2step.sdk.model.Qr;
import ch.ubique.n2step.sdk.model.VenueInfo;

import static android.util.Base64.NO_PADDING;

public class QrUtils {

	public static VenueInfo getQrInfo(String qrCodeString, String expectedQrCodePrefix) {

		String[] splits = qrCodeString.split("#");
		if (splits.length != 2) return null;
		if (!splits[0].equals(expectedQrCodePrefix)) return null;

		try {
			int decodeFlags = Base64.NO_WRAP | Base64.URL_SAFE | NO_PADDING;
			byte[] decoded = Base64.decode(splits[1], decodeFlags);
			Qr.QRCodeWrapper qrCodeWrapper = Qr.QRCodeWrapper.parseFrom(decoded);
			Qr.QRCodeContent qrCode = qrCodeWrapper.getContent();

			boolean validSignature =
					CryptoUtils.getInstance().isSignatureValid(qrCodeWrapper.getSignature().toByteArray(), qrCode.toByteArray(),
							qrCode.getPublicKey().toByteArray());
			if (!validSignature) return null;

			return new VenueInfo(qrCode.getName(), qrCode.getLocation(), qrCode.getRoom(), qrCode.getVenueType(),
					qrCode.getPublicKey().toByteArray(), qrCode.getNotificationKey().toByteArray());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
			return null;
		}
	}

}
