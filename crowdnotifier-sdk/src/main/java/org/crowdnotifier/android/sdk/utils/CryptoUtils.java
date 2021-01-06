package org.crowdnotifier.android.sdk.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.Mcl;

import org.crowdnotifier.android.sdk.model.*;

public class CryptoUtils {

	private static final int KEY_BYTES = Box.SECRETKEYBYTES;
	private static final int SEAL_BYTES = Box.SEALBYTES;
	private static final int HASH_BYTES = 32;


	private static CryptoUtils instance;
	private SodiumAndroid sodium;

	private CryptoUtils() {
		sodium = new LazySodiumAndroid(new SodiumAndroid()).getSodium();
		System.loadLibrary("mcljava");
		Mcl.SystemInit(Mcl.BLS12_381);
	}

	public static synchronized CryptoUtils getInstance() {
		if (instance == null) {
			instance = new CryptoUtils();
		}
		return instance;
	}

	public EncryptedVenueVisit getEncryptedVenueVisit(long arrivalTime, long departureTime, VenueInfo venueInfo) {

		byte[] randomValue = new byte[KEY_BYTES];
		sodium.randombytes_buf(randomValue, KEY_BYTES);

		byte[] gR = new byte[KEY_BYTES];
		int result = sodium.crypto_scalarmult_base(gR, randomValue);
		if (result != 0) { throw new RuntimeException("crypto_scalarmult_base returned a value != 0"); }

		byte[] h = new byte[KEY_BYTES];
		result = sodium.crypto_scalarmult(h, randomValue, venueInfo.getPublicKey());
		if (result != 0) { throw new RuntimeException("crypto_scalarmult returned a value != 0"); }

		byte[] infoConcatR1 = concatenate(venueInfoToBytes(venueInfo), venueInfo.getR1());
		byte[] t = new byte[HASH_BYTES];
		result = sodium.crypto_hash_sha256(t, infoConcatR1, infoConcatR1.length);
		if (result != 0) { throw new RuntimeException("crypto_hash_sha256 returned a value != 0"); }

		byte[] aux = (new Gson().toJson(new Payload(arrivalTime, departureTime, venueInfo.getNotificationKey()))).getBytes();
		byte[] tConcatAux = concatenate(t, aux);
		byte[] cipher = new byte[tConcatAux.length + SEAL_BYTES];
		result = sodium.crypto_box_seal(cipher, tConcatAux, tConcatAux.length, venueInfo.getPublicKey());
		if (result != 0) { throw new RuntimeException("crypto_box_seal returned a value != 0"); }

		return new EncryptedVenueVisit(0, new DayDate(departureTime), gR, h, cipher);
	}

	public List<ExposureEvent> searchAndDecryptMatches(ProblematicEventInfo eventInfo, List<EncryptedVenueVisit> venueVisits) {

		List<ExposureEvent> exposureEvents = new ArrayList<>();

		for (EncryptedVenueVisit venueVisit : venueVisits) {

			byte[] computed_h = new byte[KEY_BYTES];
			int result = sodium.crypto_scalarmult(computed_h, eventInfo.getSecretKey(), venueVisit.getEphemeralPublicKey());
			if (result != 0) { throw new RuntimeException("crypto_scalarmult returned a value != 0"); }

			if (!Arrays.equals(computed_h, venueVisit.getTag())) {
				continue;
			}

			byte[] gR = new byte[KEY_BYTES];
			result = sodium.crypto_scalarmult_base(gR, eventInfo.getSecretKey());
			if (result != 0) { throw new RuntimeException("crypto_scalarmult_base returned a value != 0"); }

			byte[] tConcatAux = new byte[venueVisit.getEncryptedPayload().length - SEAL_BYTES];
			result = sodium.crypto_box_seal_open(tConcatAux,venueVisit.getEncryptedPayload(),
					venueVisit.getEncryptedPayload().length,gR,eventInfo.getSecretKey());
			if (result != 0) { throw new RuntimeException("crypto_box_seal_open returned a value != 0"); }

			byte[] t = Arrays.copyOfRange(tConcatAux, 0, 32);
			byte[] aux = Arrays.copyOfRange(tConcatAux, 32, tConcatAux.length);

			byte[] tConcatR2 = concatenate(t, eventInfo.getR2());
			byte[] skP = new byte[KEY_BYTES];
			result = sodium.crypto_hash_sha256(skP, tConcatR2, tConcatR2.length);
			if (result != 0) { throw new RuntimeException("crypto_hash_sha256 returned a value != 0"); }


			byte[] venuePublicKey = new byte[Box.PUBLICKEYBYTES];
			byte[] venuePrivateKey = new byte[Box.SECRETKEYBYTES];
			result = sodium.crypto_box_seed_keypair(venuePublicKey, venuePrivateKey, skP);
			if (result != 0) { throw new RuntimeException("crypto_box_seed_keypair returned a value != 0"); }


			if (!Arrays.equals(venuePrivateKey, eventInfo.getSecretKey())) {
				continue;
			}

			Payload payload = new Gson().fromJson(new String(aux), Payload.class);

			byte[] encryptedMessage = eventInfo.getEncryptedMessage();
			byte[] decryptedMessage = new byte[encryptedMessage.length - Box.MACBYTES];
			result = sodium.crypto_secretbox_open_easy(decryptedMessage, encryptedMessage, encryptedMessage.length,
					eventInfo.getNonce(), payload.getNotificationKey());
			if (result != 0) { throw new RuntimeException("crypto_secretbox_open_easy returned a value != 0"); }

			String decryptedMessageString = new String(decryptedMessage);

			ExposureEvent exposureEvent = new ExposureEvent(venueVisit.getId(), payload.getArrivalTime(),
					payload.getDepartureTime(), decryptedMessageString);

			exposureEvents.add(exposureEvent);
		}
		return exposureEvents;
	}


	private byte[] venueInfoToBytes(VenueInfo venueInfo) {
		Qr.QRCodeContent qrCodeContent = Qr.QRCodeContent.newBuilder()
				.setName(venueInfo.getName())
				.setLocation(venueInfo.getLocation())
				.setRoom(venueInfo.getRoom())
				.setNotificationKey(ByteString.copyFrom(venueInfo.getNotificationKey()))
				.setVenueType(venueInfo.getVenueType())
				.setValidFrom(venueInfo.getValidFrom())
				.setValidTo(venueInfo.getValidTo())
				.build();
		return qrCodeContent.toByteArray();
	}

	private byte[] concatenate(byte[] a, byte[] b) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream(a.length + b.length);
			outputStream.write(a);
			outputStream.write(b);
			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Byte array concatenation failed");
		}
	}

}
