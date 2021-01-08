package org.crowdnotifier.android.sdk.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import com.herumi.mcl.GT;
import com.herumi.mcl.Mcl;

import org.crowdnotifier.android.sdk.model.*;

public class CryptoUtils {

	private static final int HASH_BYTES = 32;
	private static final int NONCE_BYTES = 32;


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

	public List<EncryptedVenueVisit> getEncryptedVenueVisit(long arrivalTime, long departureTime, VenueInfo venueInfo) {

		G2 masterPublicKey = new G2();
		masterPublicKey.deserialize(venueInfo.getMasterPublicKey());

		//scan
		ArrayList<EncryptedVenueVisit> encryptedVenueVisits = new ArrayList<>();

		ArrayList<Integer> hourCounters = getAffectedHours(arrivalTime, departureTime);
		for (Integer hour : hourCounters) {

			byte[] identity = generateIdentity(hour, venueInfo);

			byte[] message =
					(new Gson().toJson(new Payload(arrivalTime, departureTime, venueInfo.getNotificationKey()))).getBytes();

			EncryptedData encryptedData = encryptInternal(message, identity, masterPublicKey);
			encryptedVenueVisits
					.add(new EncryptedVenueVisit(0, new DayDate(departureTime), encryptedData.getC1(), encryptedData.getC2(),
							encryptedData.getC3(), encryptedData.getNonce()));
		}

		return encryptedVenueVisits;
	}

	public List<ExposureEvent> searchAndDecryptMatches(ProblematicEventInfo eventInfo, List<EncryptedVenueVisit> venueVisits) {

		List<ExposureEvent> exposureEvents = new ArrayList<>();

		for (EncryptedVenueVisit venueVisit : venueVisits) {

			G1 secretKeyForIdentity = new G1();
			secretKeyForIdentity.deserialize(eventInfo.getSecretKeyForIdentity());

			byte[] msg_p = decryptInternal(
					new EncryptedData(venueVisit.getC1(), venueVisit.getC2(), venueVisit.getC3(), venueVisit.getNonce()),
					secretKeyForIdentity, eventInfo.getIdentity());
			if (msg_p == null) continue;

			Payload payload = new Gson().fromJson(new String(msg_p), Payload.class);

			byte[] decryptedMessage = crypto_secretbox_open_easy(payload.getNotificationKey(), eventInfo.getEncryptedMessage(),
					eventInfo.getNonce());

			String decryptedMessageString = new String(decryptedMessage);

			ExposureEvent exposureEvent = new ExposureEvent(venueVisit.getId(), payload.getArrivalTime(),
					payload.getDepartureTime(), decryptedMessageString);

			exposureEvents.add(exposureEvent);
		}
		return exposureEvents;
	}

	public byte[] decryptInternal(EncryptedData encryptedData, G1 secretKeyForIdentity, byte[] identity) {
		G2 c1 = new G2();
		c1.deserialize(encryptedData.getC1());

		GT gt_temp = new GT();
		Mcl.pairing(gt_temp, secretKeyForIdentity, c1);

		byte[] hash = crypto_hash_sha256(gt_temp.serialize());
		byte[] x_p = xor(encryptedData.getC2(), hash);

		byte[] msg_p = new byte[encryptedData.getC3().length - Box.MACBYTES];
		int result = sodium.crypto_secretbox_open_easy(msg_p, encryptedData.getC3(), encryptedData.getC3().length,
				encryptedData.getNonce(), crypto_hash_sha256(x_p));
		if (result != 0) return null;

		//Additional verification
		Fr r_p = new Fr();
		r_p.setHashOf(concatenate(x_p, concatenate(identity, msg_p)));

		G2 c1_p = new G2();
		Mcl.mul(c1_p, baseG2(), r_p);

		if (!c1.equals(c1_p)) {
			return null;
		}

		if (!secretKeyForIdentity.isValidOrder() || secretKeyForIdentity.isZero()) {
			return null;
		}
		return msg_p;
	}

	public EncryptedData encryptInternal(byte[] message, byte[] identity, G2 masterPublicKey) {

		byte[] nonceX = randombytes_buf();

		Fr r = new Fr();
		r.setHashOf(concatenate(nonceX, concatenate(identity, message)));

		G2 c1 = new G2();
		Mcl.mul(c1, baseG2(), r);

		G1 g1_temp = new G1();
		Mcl.hashAndMapToG1(g1_temp, identity);

		GT gt1_temp = new GT();
		Mcl.pairing(gt1_temp, g1_temp, masterPublicKey);

		GT gt_temp = new GT();
		Mcl.pow(gt_temp, gt1_temp, r);

		byte[] c2_pair = crypto_hash_sha256(gt_temp.serialize());
		byte[] c2 = xor(nonceX, c2_pair);

		byte[] nonce = randombytes_buf();

		byte[] c3 = crypto_secretbox_easy(crypto_hash_sha256(nonceX), message, nonce);

		return new EncryptedData(c1.serialize(), c2, c3, nonce);
	}

	public byte[] generateIdentity(Qr.QRCodeContent qrCodeContent, byte[] nonce1, byte[] nonce2, int hour) {
		byte[] hash1 = crypto_hash_sha256(concatenate(qrCodeContent.toByteArray(), nonce1));
		return crypto_hash_sha256(concatenate(hash1, concatenate(String.valueOf(hour).getBytes(), nonce2)));
	}

	public byte[] generateIdentity(int hour, VenueInfo venueInfo) {
		byte[] hash1 = crypto_hash_sha256(
				concatenate(venueInfoToInfoBytes(venueInfo), venueInfo.getNonce1()));
		return crypto_hash_sha256(concatenate(hash1,
				concatenate(String.valueOf(hour).getBytes(), venueInfo.getNonce2())));
	}

	private byte[] crypto_secretbox_easy(byte[] secretKey, byte[] message, byte[] nonce) {
		byte[] encrytpedMessage = new byte[message.length + Box.MACBYTES];
		int result = sodium.crypto_secretbox_easy(encrytpedMessage, message, message.length, nonce, secretKey);
		if (result != 0) { throw new RuntimeException("crypto_secretbox_easy returned a value != 0"); }
		return encrytpedMessage;
	}

	private byte[] crypto_secretbox_open_easy(byte[] key, byte[] cipherText, byte[] nonce) {
		byte[] decryptedMessage = new byte[cipherText.length - Box.MACBYTES];
		int result = sodium.crypto_secretbox_open_easy(decryptedMessage, cipherText, cipherText.length, nonce, key);
		if (result != 0) { throw new RuntimeException("crypto_secretbox_open_easy returned a value != 0"); }
		return decryptedMessage;
	}

	private byte[] xor(byte[] a, byte[] b) {
		if (a.length != b.length) throw new RuntimeException("Cannot xor two byte arrays of different length");
		byte[] c = new byte[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i] = (byte) (a[i] ^ b[i]);
		}
		return c;
	}

	private byte[] randombytes_buf() {
		byte[] nonce = new byte[NONCE_BYTES];
		sodium.randombytes_buf(nonce, NONCE_BYTES);
		return nonce;
	}

	private byte[] crypto_hash_sha256(byte[] in) {
		byte[] out = new byte[HASH_BYTES];
		int result = sodium.crypto_hash_sha256(out, in, in.length);
		if (result != 0) { throw new RuntimeException("crypto_hash_sha256 returned a value != 0"); }
		return out;
	}

	public ArrayList<Integer> getAffectedHours(long arrivalTime, long departureTime) {
		long ONE_HOUR_IN_MILLISECONDS = 1000L * 60 * 60;
		long startHour = arrivalTime / ONE_HOUR_IN_MILLISECONDS;
		long endHour = departureTime / ONE_HOUR_IN_MILLISECONDS;
		ArrayList<Integer> result = new ArrayList<>();
		for (int i = (int) startHour; i <= endHour; i++) {
			result.add(i);
		}
		return result;
	}

	private byte[] venueInfoToInfoBytes(VenueInfo venueInfo) {
		Qr.QRCodeContent qrCodeContent = Qr.QRCodeContent.newBuilder()
				.setName(venueInfo.getName())
				.setLocation(venueInfo.getLocation())
				.setRoom(venueInfo.getRoom())
				.setVenueType(venueInfo.getVenueType())
				.setNotificationKey(ByteString.copyFrom(venueInfo.getNotificationKey()))
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

	private G2 baseG2() {
		G2 baseG2 = new G2();
		baseG2.setStr("1 3527010695874666181871391160110601448900299527927752" +
				"40219908644239793785735715026873347600343865175952761926303160 " +
				"305914434424421370997125981475378163698647032547664755865937320" +
				"6291635324768958432433509563104347017837885763365758 " +
				"198515060228729193556805452117717163830086897821565573085937866" +
				"5066344726373823718423869104263333984641494340347905 " +
				"927553665492332455747201965776037880757740193453592970025027978" +
				"793976877002675564980949289727957565575433344219582");
		return baseG2;
	}

}
