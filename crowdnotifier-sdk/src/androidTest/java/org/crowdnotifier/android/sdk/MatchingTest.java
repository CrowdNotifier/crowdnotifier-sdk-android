package org.crowdnotifier.android.sdk;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.crowdnotifier.android.sdk.model.Exposure;
import org.crowdnotifier.android.sdk.model.ProblematicEventInfo;
import org.crowdnotifier.android.sdk.storage.ExposureStorage;
import org.crowdnotifier.android.sdk.storage.VenueVisitStorage;
import org.crowdnotifier.android.sdk.utils.Base64Util;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MatchingTest {

	private Context context;

	@Before
	public void init() {
		context = InstrumentationRegistry.getInstrumentation().getTargetContext();

		VenueVisitStorage.getInstance(context).clear();
		ExposureStorage.getInstance(context).clear();
	}

	@Test
	public void testMatching() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		byte[] venuePublicKey = Base64Util.fromBase64("gQNC_DrEvdK8iVjnDpLRJNcqMa_ujrWbZORbZrd_ogU");
		byte[] venuePrivateKey =
				Base64Util.fromBase64("JYK6EYh6rtB3X5SJOlY98ditkGJBgwpcrCxSZGhXhmeBA0L8OsS90ryJWOcOktEk1yoxr-6OtZtk5Ftmt3-iBQ");

		CrowdNotifier.addVenueVisit(time - 2 * 60 * 60 * 1000l, time, notificationKey, venuePublicKey, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents.add(new ProblematicEventInfo(venuePrivateKey, time - 1 * 60 * 60 * 1000l, time - 10));
		List<Exposure> exposures = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposures.size());
		assertEquals(time - 10, exposures.get(0).getEndTime());
		assertEquals(time - 1 * 60 * 60 * 1000l, exposures.get(0).getStartTime());
	}


	@Test
	public void testMatchingEnteredBefore() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		byte[] venuePublicKey = Base64Util.fromBase64("gQNC_DrEvdK8iVjnDpLRJNcqMa_ujrWbZORbZrd_ogU");
		byte[] venuePrivateKey =
				Base64Util.fromBase64("JYK6EYh6rtB3X5SJOlY98ditkGJBgwpcrCxSZGhXhmeBA0L8OsS90ryJWOcOktEk1yoxr-6OtZtk5Ftmt3-iBQ");

		CrowdNotifier.addVenueVisit(time - 2 * 60 * 60 * 1000l, time, notificationKey, venuePublicKey, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents.add(new ProblematicEventInfo(venuePrivateKey, time - 3 * 60 * 60 * 1000l, time - 10));
		List<Exposure> exposures = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposures.size());
		assertEquals(time - 10, exposures.get(0).getEndTime());
		assertEquals(time - 2 * 60 * 60 * 1000l, exposures.get(0).getStartTime());
	}

	@Test
	public void testMatchingExitedBefore() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		byte[] venuePublicKey = Base64Util.fromBase64("gQNC_DrEvdK8iVjnDpLRJNcqMa_ujrWbZORbZrd_ogU");
		byte[] venuePrivateKey =
				Base64Util.fromBase64("JYK6EYh6rtB3X5SJOlY98ditkGJBgwpcrCxSZGhXhmeBA0L8OsS90ryJWOcOktEk1yoxr-6OtZtk5Ftmt3-iBQ");

		CrowdNotifier.addVenueVisit(time - 2 * 60 * 60 * 1000l, time - 30 * 60 * 1000l, notificationKey, venuePublicKey, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents.add(new ProblematicEventInfo(venuePrivateKey, time - 1 * 60 * 60 * 1000l, time - 10));
		List<Exposure> exposures = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposures.size());
		assertEquals(time - 30 * 60 * 1000l, exposures.get(0).getEndTime());
		assertEquals(time - 1 * 60 * 60 * 1000l, exposures.get(0).getStartTime());
	}

	@Test
	public void testMatchingSameTime() {
		long time = System.currentTimeMillis();
		byte[] notificationKey = Base64Util.fromBase64("HYZf4ROIMIp12Jr521JS3fttAmV4y1vATkx3MhTFB-E");
		byte[] venuePublicKey = Base64Util.fromBase64("gQNC_DrEvdK8iVjnDpLRJNcqMa_ujrWbZORbZrd_ogU");
		byte[] venuePrivateKey =
				Base64Util.fromBase64("JYK6EYh6rtB3X5SJOlY98ditkGJBgwpcrCxSZGhXhmeBA0L8OsS90ryJWOcOktEk1yoxr-6OtZtk5Ftmt3-iBQ");

		CrowdNotifier.addVenueVisit(time - 1 * 60 * 60 * 1000l, time - 10, notificationKey, venuePublicKey, context);

		ArrayList<ProblematicEventInfo> problematicEvents = new ArrayList<>();
		problematicEvents.add(new ProblematicEventInfo(venuePrivateKey, time - 1 * 60 * 60 * 1000l, time - 10));
		List<Exposure> exposures = CrowdNotifier.checkForMatches(problematicEvents, context);

		assertEquals(1, exposures.size());
		assertEquals(time - 10, exposures.get(0).getEndTime());
		assertEquals(time - 1 * 60 * 60 * 1000l, exposures.get(0).getStartTime());
	}

}