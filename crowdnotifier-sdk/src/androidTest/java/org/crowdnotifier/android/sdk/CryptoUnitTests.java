package org.crowdnotifier.android.sdk;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.InputStreamReader;

import com.google.gson.Gson;

import org.crowdnotifier.android.sdk.model.VenueInfo;
import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.crowdnotifier.android.sdk.utils.QrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This class tests the implementations of some central building blocks of the CrowdNotifier Protocol, such as the Identity
 * Generation and the derivation of Nonce1, Nonce2 and Notification Key using the HKDF function. It uses the test vector data
 * from the crowd_notifier_test_vectors.json File.
 */
@RunWith(AndroidJUnit4.class)
public class CryptoUnitTests {

	private Context context;
	private CryptoUtils cryptoUtils;
	private TestVectors testVectors;

	@Before
	public void init() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();
		cryptoUtils = CryptoUtils.getInstance();
		testVectors = new Gson().fromJson(new InputStreamReader(
						context.getResources().openRawResource(org.crowdnotifier.android.sdk.test.R.raw.crowd_notifier_test_vectors)),
				TestVectors.class);
	}


	/**
	 * Tests the Identity Generation method in the CryptoUtils class.
	 */
	@Test
	public void testGenerateIdentity() {

		for (TestVectors.IdentityTest identityTest : testVectors.identityTestVector) {
			byte[] identity = cryptoUtils.generateIdentity(identityTest.qrCodePayload, identityTest.startOfInterval, 3600);
			
			assertArrayEquals(identityTest.identity, identity);
		}
	}

	/**
	 * Tests the CryptoUtils.getNoncesAndNotificationKey() function which derivates Nonce1, Nonce2 and Notification Key from a
	 * given input byte array using the HKDF function.
	 */
	@Test
	public void testHKDF() {

		for (TestVectors.HKDFTest hkdfTest : testVectors.hkdfTestVector) {
			CryptoUtils.NoncesAndNotificationKey noncesAndNotificationKey =
					cryptoUtils.getNoncesAndNotificationKey(hkdfTest.qrCodePayload);

			assertArrayEquals(hkdfTest.noncePreId, noncesAndNotificationKey.noncePreId);
			assertArrayEquals(hkdfTest.nonceTimekey, noncesAndNotificationKey.nonceTimekey);
			assertArrayEquals(hkdfTest.notificationKey, noncesAndNotificationKey.notificationKey);
		}
	}


	/**
	 * Tests the CryptoUtils.getNoncesAndNotificationKey() function which derivates Nonce1, Nonce2 and Notification Key from a
	 * given input byte array using the HKDF function.
	 */
	@Test
	public void testGetQrInfo() throws QrUtils.QRException {

		String qrBase64 = "CAMSEwgDEgNncnIgy5avhQYoy_aenSYahgEIAxJglW5voTRVR" +
				"-jgYMiWLd04hjvyyFQG7QOyBLw0D7XbASlqlg0AviQMqgjbABZk9PcCip27szrqFyv_1YtKZE8eyzt7vtN4qKfJdWrItLRzRtjb83piN3cDt_yNo7siohQVGiCQeiTeE9G72brkE1Ur5eOwbeYdQC0ZVjXx6TfNGElQriIfCAEggOjdDSiA3MwUMODUAzCA3dsBMIC6twMwgPTuBg";

		assertNotNull(QrUtils.getQrInfo("https://qr.example.org?v=4#" + qrBase64, "qr.example.org"));
		assertNotNull(QrUtils.getQrInfo("https://qr.example.org/?v=4#" + qrBase64, "qr.example.org"));
		assertNotNull(QrUtils.getQrInfo("http://qr.example.org?v=4#" + qrBase64, "qr.example.org"));
		assertNotNull(QrUtils.getQrInfo("qr.example.org?v=4#" + qrBase64, "qr.example.org"));
		assertNotNull(QrUtils.getQrInfo("qr.example.org/?v=4&r=3#" + qrBase64, "qr.example.org"));

		try {
			QrUtils.getQrInfo("https://qr.example.org?v=3#" + qrBase64, "qr.example.org");
			assert false;
		} catch (QrUtils.InvalidQRCodeVersionException e) {}

		try {
			QrUtils.getQrInfo("https://www.qr.example.org?v=4#" + qrBase64, "qr.example.org");
			assert false;
		} catch (QrUtils.InvalidQRCodeFormatException e) {}

		try {
			QrUtils.getQrInfo("www.qr.example.org?v=4#" + qrBase64, "qr.example.org");
			assert false;
		} catch (QrUtils.InvalidQRCodeFormatException e) {}

		try {
			QrUtils.getQrInfo("example.org?v=4#" + qrBase64, "qr.example.org");
			assert false;
		} catch (QrUtils.InvalidQRCodeFormatException e) {}

	}

}