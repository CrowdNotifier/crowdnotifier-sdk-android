package org.crowdnotifier.android.sdk.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.crypto.tink.subtle.Hkdf;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.herumi.mcl.Fr;
import com.herumi.mcl.G1;
import com.herumi.mcl.G2;
import com.herumi.mcl.GT;
import com.herumi.mcl.Mcl;

import org.crowdnotifier.android.sdk.model.*;
import org.crowdnotifier.android.sdk.model.v3.AssociatedData;
import org.crowdnotifier.android.sdk.model.v3.CrowdNotifierData;
import org.crowdnotifier.android.sdk.model.v3.QRCodePayload;
import org.crowdnotifier.android.sdk.model.v3.TraceLocation;

import static org.crowdnotifier.android.sdk.utils.QrUtils.QR_CODE_VERSION_3;

/**
 * This class contains all cryptographic calculations, such as encrypting VenueVisits or matching stored encrypted VenueVisits with
 * ProblematicEventInfos.
 */
public class CryptoUtils {

	private static final int HASH_BYTES = 32;
	private static final int NONCE_BYTES = 32;
	private static final int CRYPTOGRAPHIC_SEED_BYTES = 32;


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

		G2 masterPublicKey = new G2();
		masterPublicKey.deserialize(venueInfo.getPublicKey());

		ArrayList<IBECiphertext> ibeCiphertextsEntries = new ArrayList<>();

		ArrayList<Integer> hourCounters = getAffectedHours(arrivalTime, departureTime);
		for (Integer hour : hourCounters) {

			// getAffectedhours() generates hours since UNIX epoch. As the new format requires these to be in seconds since
			// UNIX epoch, we multiply by 60 minutes * 60 seconds = 3600.
			byte[] identity = generateIdentity(venueInfo.getQrCodePayload(), hour * 3600L, 3600);

			byte[] message =
					(new Gson().toJson(new Payload(arrivalTime, departureTime, venueInfo.getNotificationKey()))).getBytes();

			ibeCiphertextsEntries.add(encryptInternal(masterPublicKey, identity, message, hour * 3600L));
		}

		return new EncryptedVenueVisit(ibeCiphertextsEntries);
	}

	public List<ExposureEvent> searchAndDecryptMatches(ProblematicEventInfo eventInfo, List<EncryptedVenueVisit> venueVisits) {

		List<ExposureEvent> exposureEvents = new ArrayList<>();

		for (EncryptedVenueVisit venueVisit : venueVisits) {

			G1 secretKeyForIdentity = new G1();
			secretKeyForIdentity.deserialize(eventInfo.getSecretKeyForIdentity());

			for (IBECiphertext ibeCiphertext : venueVisit.getIbeCiphertextEntries()) {
				long startOfDay = ibeCiphertext.getDayDate().getStartOfDayTimestamp();
				long endOfDay = ibeCiphertext.getDayDate().getNextDay().getStartOfDayTimestamp();
				if (!doIntersect(startOfDay, endOfDay, eventInfo.getStartTimestamp(), eventInfo.getEndTimestamp())) continue;

				byte[] msg_p = decryptInternal(ibeCiphertext, secretKeyForIdentity, eventInfo.getIdentity());
				if (msg_p == null) continue;

				Payload payload = new Gson().fromJson(new String(msg_p), Payload.class);

				byte[] decryptedMessage =
						crypto_secretbox_open_easy(payload.getNotificationKey(), eventInfo.getEncryptedAssociatedData(),
								eventInfo.getCipherTextNonce());

				String decryptedMessageString;
				byte[] countryData = null;
				try {
					AssociatedData associatedData = AssociatedData.parseFrom(decryptedMessage);
					decryptedMessageString = associatedData.getMessage();
					countryData = associatedData.getCountryData().toByteArray();
				} catch (InvalidProtocolBufferException e) {
					decryptedMessageString = new String(decryptedMessage);
				}

				ExposureEvent exposureEvent = new ExposureEvent(venueVisit.getId(), payload.getArrivalTime(),
						payload.getDepartureTime(), decryptedMessageString, countryData);

				exposureEvents.add(exposureEvent);
				break;
			}
		}
		return exposureEvents;
	}

	private boolean doIntersect(long startTime1, long endTime1, long startTime2, long endTime2) {
		return startTime1 <= endTime2 && endTime1 >= startTime2;
	}

	public byte[] decryptInternal(IBECiphertext ibeCiphertext, G1 secretKeyForIdentity, byte[] identity) {
		G2 c1 = new G2();
		c1.deserialize(ibeCiphertext.getC1());

		GT gt_temp = new GT();
		Mcl.pairing(gt_temp, secretKeyForIdentity, c1);

		byte[] hash = crypto_hash_sha256(gt_temp.serialize());
		byte[] x_p = xor(ibeCiphertext.getC2(), hash);

		byte[] msg_p = new byte[ibeCiphertext.getC3().length - Box.MACBYTES];
		int result = sodium.crypto_secretbox_open_easy(msg_p, ibeCiphertext.getC3(), ibeCiphertext.getC3().length,
				ibeCiphertext.getNonce(), crypto_hash_sha256(x_p));
		if (result != 0) return null;

		//Additional verification
		Fr r_p = new Fr();
		r_p.setHashOf(concatenate(x_p, identity, msg_p));

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

	public IBECiphertext encryptInternal(G2 masterPublicKey, byte[] identity, byte[] message, long intervalStart) {

		byte[] x = getRandomValue(NONCE_BYTES);

		Fr r = new Fr();
		r.setHashOf(concatenate(x, identity, message));

		G2 c1 = new G2();
		Mcl.mul(c1, baseG2(), r);

		G1 g1_temp = new G1();
		Mcl.hashAndMapToG1(g1_temp, identity);

		GT gt1_temp = new GT();
		Mcl.pairing(gt1_temp, g1_temp, masterPublicKey);

		GT gt_temp = new GT();
		Mcl.pow(gt_temp, gt1_temp, r);

		byte[] c2_pair = crypto_hash_sha256(gt_temp.serialize());
		byte[] c2 = xor(x, c2_pair);

		byte[] nonce = getRandomValue(NONCE_BYTES);

		byte[] c3 = crypto_secretbox_easy(crypto_hash_sha256(x), message, nonce);

		return new IBECiphertext(c1.serialize(), c2, c3, nonce, new DayDate(intervalStart * 1000));
	}

	public byte[] generateIdentity(QRCodePayload qrCodePayload, long startOfInterval, int intervalLength) {
		return generateIdentity(qrCodePayload.toByteArray(), startOfInterval, intervalLength);
	}

	public byte[] generateIdentity(byte[] qrCodePayload, long startOfInterval, int intervalLength) {
		if (intervalLength < 900 || intervalLength > 86400) {
			throw new RuntimeException("intervalLength must be between 900 and 86400");
		}
		NoncesAndNotificationKey cryptoData = getNoncesAndNotificationKey(qrCodePayload);

		byte[] preid = crypto_hash_sha256(
				concatenate("CN-PREID".getBytes(StandardCharsets.US_ASCII), qrCodePayload, cryptoData.noncePreId));

		byte[] timeKey = crypto_hash_sha256(concatenate("CN-TIMEKEY".getBytes(StandardCharsets.US_ASCII),
				int32ToBytesBigEndian(intervalLength), int64ToBytesBigEndian(startOfInterval), cryptoData.nonceTimekey));

		return crypto_hash_sha256(concatenate("CN-ID".getBytes(StandardCharsets.US_ASCII), preid,
				int32ToBytesBigEndian(intervalLength), int64ToBytesBigEndian(startOfInterval), timeKey));
	}

	public ArrayList<byte[]> generateIdentities(VenueInfo venueInfo, long startTimestamp, long endTimestamp) {
		ArrayList<Integer> hourCounters = getAffectedHours(startTimestamp, endTimestamp);
		ArrayList<byte[]> identities = new ArrayList<>();
		for (Integer hour : hourCounters) {
			// getAffectedhours() generates hours since UNIX epoch. As the new format requires these to be in seconds since
			// UNIX epoch, we multiply by 60 minutes * 60 seconds = 3600.
			byte[] identity = generateIdentity(venueInfo.getQrCodePayload(), hour * 3600L, 3600);
			identities.add(identity);
		}
		return identities;
	}

	public NoncesAndNotificationKey getNoncesAndNotificationKey(QRCodePayload qrCodePayload) {
		return getNoncesAndNotificationKey(qrCodePayload.toByteArray());
	}

	public NoncesAndNotificationKey getNoncesAndNotificationKey(byte[] qrCodePayload) {
		try {
			byte[] hkdfOutput = Hkdf.computeHkdf("HMACSHA256", qrCodePayload, new byte[0],
					"CrowdNotifier_v3".getBytes(StandardCharsets.US_ASCII), 96);
			byte[] noncePreId = Arrays.copyOfRange(hkdfOutput, 0, 32);
			byte[] nonceTimekey = Arrays.copyOfRange(hkdfOutput, 32, 64);
			byte[] notificationKey = Arrays.copyOfRange(hkdfOutput, 64, 96);
			return new NoncesAndNotificationKey(noncePreId, nonceTimekey, notificationKey);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("HKDF threw GeneralSecurityException");
		}
	}

	/**
	 * Generates Base64 encoded String of an Entry QR Code
	 */
	public String generateEntryQrCode(String description, String address, byte[] countryData, long validFrom, long validTo,
			byte[] masterPublicKey) {

		TraceLocation traceLocation = TraceLocation.newBuilder()
				.setVersion(QR_CODE_VERSION_3)
				.setStartTimestamp(validFrom)
				.setEndTimestamp(validTo)
				.setDescription(description)
				.setAddress(address)
				.build();

		CrowdNotifierData crowdNotifierData = CrowdNotifierData.newBuilder()
				.setVersion(QR_CODE_VERSION_3)
				.setCryptographicSeed(ByteString.copyFrom(getRandomValue(CRYPTOGRAPHIC_SEED_BYTES)))
				.setPublicKey(ByteString.copyFrom(masterPublicKey))
				.build();

		QRCodePayload qrCodePayload = QRCodePayload.newBuilder()
				.setVersion(QR_CODE_VERSION_3)
				.setCrowdNotifierData(crowdNotifierData)
				.setLocationData(traceLocation)
				.setCountryData(ByteString.copyFrom(countryData))
				.build();

		return Base64Util.toBase64(qrCodePayload.toByteArray());
	}

	private byte[] int64ToBytesBigEndian(long l) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putLong(l);
		return byteBuffer.array();
	}

	private byte[] int32ToBytesBigEndian(int i) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putInt(i);
		return byteBuffer.array();
	}

	private byte[] crypto_secretbox_easy(byte[] secretKey, byte[] message, byte[] nonce) {
		byte[] encryptedMessage = new byte[message.length + Box.MACBYTES];
		int result = sodium.crypto_secretbox_easy(encryptedMessage, message, message.length, nonce, secretKey);
		if (result != 0) { throw new RuntimeException("crypto_secretbox_easy returned a value != 0"); }
		return encryptedMessage;
	}

	private byte[] crypto_secretbox_open_easy(byte[] key, byte[] cipherText, byte[] nonce) {
		byte[] decryptedMessage = new byte[cipherText.length - Box.MACBYTES];
		int result = sodium.crypto_secretbox_open_easy(decryptedMessage, cipherText, cipherText.length, nonce, key);
		if (result != 0) return new byte[0];
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

	public byte[] getRandomValue(int bytes) {
		byte[] nonce = new byte[bytes];
		sodium.randombytes_buf(nonce, nonce.length);
		return nonce;
	}

	private byte[] crypto_hash_sha256(byte[] in) {
		byte[] out = new byte[HASH_BYTES];
		int result = sodium.crypto_hash_sha256(out, in, in.length);
		if (result != 0) { throw new RuntimeException("crypto_hash_sha256 returned a value != 0"); }
		return out;
	}

	/**
	 * @return a List of Integers containing all hours since UNIX epoch that intersect with the (arrivalTime, departureTime)
	 * interval.
	 */
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

	private byte[] concatenate(byte[]... byteArrays) {
		try {
			byte[] result = new byte[0];
			for (byte[] byteArray : byteArrays) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream(result.length + byteArray.length);
				outputStream.write(result);
				outputStream.write(byteArray);
				result = outputStream.toByteArray();
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Byte array concatenation failed");
		}
	}

	public G2 baseG2() {
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

	public class NoncesAndNotificationKey {
		public final byte[] noncePreId;
		public final byte[] nonceTimekey;
		public final byte[] notificationKey;

		public NoncesAndNotificationKey(byte[] noncePreId, byte[] nonceTimekey, byte[] notificationKey) {
			this.noncePreId = noncePreId;
			this.nonceTimekey = nonceTimekey;
			this.notificationKey = notificationKey;
		}

	}

}
