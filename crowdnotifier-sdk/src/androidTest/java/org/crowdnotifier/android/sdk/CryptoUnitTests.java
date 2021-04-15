package org.crowdnotifier.android.sdk;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.InputStreamReader;

import com.google.gson.Gson;

import org.crowdnotifier.android.sdk.utils.CryptoUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

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


	@Test
	public void testGenerateIdentity() {

		for (TestVectors.IdentityTest identityTest : testVectors.identityTestVector) {
			byte[] identity = cryptoUtils.generateIdentityV3(identityTest.infoBytes, identityTest.startOfInterval);
			
			assertArrayEquals(identityTest.identity, identity);
		}
	}

	@Test
	public void testHKDF() {

		for (TestVectors.HKDFTest hkdfTest : testVectors.hkdfTestVector) {
			CryptoUtils.NoncesAndNotificationKey noncesAndNotificationKey =
					cryptoUtils.getNoncesAndNotificationKey(hkdfTest.infoBytes);

			assertArrayEquals(hkdfTest.nonce1, noncesAndNotificationKey.nonce1);
			assertArrayEquals(hkdfTest.nonce2, noncesAndNotificationKey.nonce2);
			assertArrayEquals(hkdfTest.notificationKey, noncesAndNotificationKey.notificationKey);
		}
	}

}