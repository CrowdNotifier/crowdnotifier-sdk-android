package org.crowdnotifier.android.sdk;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;
import com.goterl.lazycode.lazysodium.interfaces.Box;

import org.crowdnotifier.android.sdk.model.Qr;
import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.crowdnotifier.android.sdk.model.ExposureEvent;
import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.Base64Util;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MatchingTest {

	private Context context;
	private SodiumAndroid sodium;


	@Before
	public void init() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		VenueVisitStorage.getInstance(context).clear();
		ExposureStorage.getInstance(context).clear();

		sodium = new LazySodiumAndroid(new SodiumAndroid()).getSodium();
	}

	@Test
	public void testMatching() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		String message = "This is a message";
		byte[] nonce = getRandomValue(Box.NONCEBYTES);
		byte[] encryptedMessage = encryptMessage(notificationKey, message, nonce);
		byte[] r1 = getRandomValue(Box.SECRETKEYBYTES);
		byte[] r2 = getRandomValue(Box.SECRETKEYBYTES);
		String s = "a";
		long validFrom = 0;
		long validTo = 1000;
		byte[] infoBytes = getInfoBytes(s, s, s, notificationKey, Qr.QRCodeContent.VenueType.OTHER, validFrom, validTo);
		byte[] venuePublicKey = new byte[Box.PUBLICKEYBYTES];
		byte[] venuePrivateKey = new byte[Box.SECRETKEYBYTES];
		int result = sodium.crypto_box_seed_keypair(venuePublicKey, venuePrivateKey, hashInfo(infoBytes, r1, r2));
		if (result != 0) { throw new RuntimeException("crypto_box_seed_keypair returned a value != 0"); }
		VenueInfo venueInfo =
				new VenueInfo(s, s, s, Qr.QRCodeContent.VenueType.OTHER, venuePublicKey, notificationKey, r1, validFrom, validTo);

		CrowdNotifier.addCheckIn(time - 2 * 60 * 60 * 1000l, time, venueInfo, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents
				.add(new ProblematicEventInfo(venuePrivateKey, time - 1 * 60 * 60 * 1000l, time - 10, encryptedMessage,
						nonce, r2));

		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposureEvents.size());
		assertEquals(time - 10, exposureEvents.get(0).getEndTime());
		assertEquals(time - 1 * 60 * 60 * 1000l, exposureEvents.get(0).getStartTime());
		assertEquals(message, exposureEvents.get(0).getMessage());
	}


	@Test
	public void testMatchingEnteredBefore() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		String message = "This is a message";
		byte[] nonce = getRandomValue(Box.NONCEBYTES);
		byte[] encryptedMessage = encryptMessage(notificationKey, message, nonce);
		byte[] r1 = getRandomValue(Box.SECRETKEYBYTES);
		byte[] r2 = getRandomValue(Box.SECRETKEYBYTES);
		String s = "a";
		long validFrom = 0;
		long validTo = 1000;
		byte[] infoBytes = getInfoBytes(s, s, s, notificationKey, Qr.QRCodeContent.VenueType.OTHER, validFrom, validTo);
		byte[] venuePublicKey = new byte[Box.PUBLICKEYBYTES];
		byte[] venuePrivateKey = new byte[Box.SECRETKEYBYTES];
		int result = sodium.crypto_box_seed_keypair(venuePublicKey, venuePrivateKey, hashInfo(infoBytes, r1, r2));
		if (result != 0) { throw new RuntimeException("crypto_box_seed_keypair returned a value != 0"); }
		VenueInfo venueInfo =
				new VenueInfo(s, s, s, Qr.QRCodeContent.VenueType.OTHER, venuePublicKey, notificationKey, r1, validFrom, validTo);

		CrowdNotifier.addCheckIn(time - 2 * 60 * 60 * 1000l, time, venueInfo, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents
				.add(new ProblematicEventInfo(venuePrivateKey, time - 3 * 60 * 60 * 1000l, time - 10, encryptedMessage,
						nonce, r2));
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposureEvents.size());
		assertEquals(time - 10, exposureEvents.get(0).getEndTime());
		assertEquals(time - 2 * 60 * 60 * 1000l, exposureEvents.get(0).getStartTime());
		assertEquals(message, exposureEvents.get(0).getMessage());
	}

	@Test
	public void testMatchingExitedBefore() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		String message = "This is a message";
		byte[] nonce = getRandomValue(Box.NONCEBYTES);
		byte[] encryptedMessage = encryptMessage(notificationKey, message, nonce);
		byte[] r1 = getRandomValue(Box.SECRETKEYBYTES);
		byte[] r2 = getRandomValue(Box.SECRETKEYBYTES);
		String s = "a";
		long validFrom = 0;
		long validTo = 1000;
		byte[] infoBytes = getInfoBytes(s, s, s, notificationKey, Qr.QRCodeContent.VenueType.OTHER, validFrom, validTo);
		byte[] venuePublicKey = new byte[Box.PUBLICKEYBYTES];
		byte[] venuePrivateKey = new byte[Box.SECRETKEYBYTES];
		int result = sodium.crypto_box_seed_keypair(venuePublicKey, venuePrivateKey, hashInfo(infoBytes, r1, r2));
		if (result != 0) { throw new RuntimeException("crypto_box_seed_keypair returned a value != 0"); }
		VenueInfo venueInfo =
				new VenueInfo(s, s, s, Qr.QRCodeContent.VenueType.OTHER, venuePublicKey, notificationKey, r1, validFrom, validTo);

		CrowdNotifier.addCheckIn(time - 2 * 60 * 60 * 1000l, time - 30 * 60 * 1000l, venueInfo, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents
				.add(new ProblematicEventInfo(venuePrivateKey, time - 1 * 60 * 60 * 1000l, time - 10, encryptedMessage,
						nonce, r2));
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposureEvents.size());
		assertEquals(time - 30 * 60 * 1000l, exposureEvents.get(0).getEndTime());
		assertEquals(time - 1 * 60 * 60 * 1000l, exposureEvents.get(0).getStartTime());
		assertEquals(message, exposureEvents.get(0).getMessage());
	}

	@Test
	public void testMatchingSameTime() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		String message = "This is a message";
		byte[] nonce = getRandomValue(Box.NONCEBYTES);
		byte[] encryptedMessage = encryptMessage(notificationKey, message, nonce);
		byte[] r1 = getRandomValue(Box.SECRETKEYBYTES);
		byte[] r2 = getRandomValue(Box.SECRETKEYBYTES);
		String s = "a";
		long validFrom = 0;
		long validTo = 1000;
		byte[] infoBytes = getInfoBytes(s, s, s, notificationKey, Qr.QRCodeContent.VenueType.OTHER, validFrom, validTo);
		byte[] venuePublicKey = new byte[Box.PUBLICKEYBYTES];
		byte[] venuePrivateKey = new byte[Box.SECRETKEYBYTES];
		int result = sodium.crypto_box_seed_keypair(venuePublicKey, venuePrivateKey, hashInfo(infoBytes, r1, r2));
		if (result != 0) { throw new RuntimeException("crypto_box_seed_keypair returned a value != 0"); }
		VenueInfo venueInfo =
				new VenueInfo(s, s, s, Qr.QRCodeContent.VenueType.OTHER, venuePublicKey, notificationKey, r1, validFrom, validTo);

		CrowdNotifier.addCheckIn(time - 1 * 60 * 60 * 1000l, time - 10, venueInfo, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents
				.add(new ProblematicEventInfo(venuePrivateKey, time - 1 * 60 * 60 * 1000l, time - 10, encryptedMessage,
						nonce, r2));
		List<ExposureEvent> exposureEvents = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposureEvents.size());
		assertEquals(time - 10, exposureEvents.get(0).getEndTime());
		assertEquals(time - 1 * 60 * 60 * 1000l, exposureEvents.get(0).getStartTime());
		assertEquals(message, exposureEvents.get(0).getMessage());
	}


	private byte[] getRandomValue(int bytes) {
		byte[] nonce = new byte[bytes];
		sodium.randombytes_buf(nonce, nonce.length);
		return nonce;
	}

	private byte[] hashInfo(byte[] infoBytes, byte[] r1, byte[] r2) {

		byte[] infoConcatR1 = concatenate(infoBytes, r1);
		byte[] h1 = new byte[32];
		int result = sodium.crypto_hash_sha256(h1, infoConcatR1, infoConcatR1.length);
		if (result != 0) { throw new RuntimeException("crypto_hash_sha256 returned a value != 0"); }

		byte[] h1ConcatR2 = concatenate(h1, r2);
		byte[] seed = new byte[32];
		result = sodium.crypto_hash_sha256(seed, h1ConcatR2, h1ConcatR2.length);
		if (result != 0) { throw new RuntimeException("crypto_hash_sha256 returned a value != 0"); }

		return seed;
	}

	private byte[] encryptMessage(byte[] secretKey, String message, byte[] nonce) {
		byte[] messageBytes = message.getBytes();
		byte[] encrytpedMessage = new byte[messageBytes.length + Box.MACBYTES];
		sodium.crypto_secretbox_easy(encrytpedMessage, messageBytes, messageBytes.length, nonce, secretKey);
		return encrytpedMessage;
	}

	private byte[] getInfoBytes(String name, String location, String room, byte[] notificationKey,
			Qr.QRCodeContent.VenueType venueType, long validFrom, long validTo) {
		Qr.QRCodeContent qrCodeContent = Qr.QRCodeContent.newBuilder()
				.setName(name)
				.setLocation(location)
				.setRoom(room)
				.setNotificationKey(ByteString.copyFrom(notificationKey))
				.setVenueType(venueType)
				.setValidFrom(validFrom)
				.setValidTo(validTo)
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