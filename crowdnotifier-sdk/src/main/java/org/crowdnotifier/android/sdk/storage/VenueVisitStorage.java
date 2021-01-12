package org.crowdnotifier.android.sdk.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.crowdnotifier.android.sdk.model.DayDate;
import org.crowdnotifier.android.sdk.model.EncryptedVenueVisit;

public class VenueVisitStorage {

	private static final String KEY_CROWDNOTIFIER_STORE = "KEY_CROWDNOTIFIER_STORE";
	private static final String KEY_VENUE_VISITS = "KEY_VENUE_VISITS_V2";
	private static final Type VENUE_LIST_TYPE = new TypeToken<ArrayList<EncryptedVenueVisit>>() { }.getType();

	private static VenueVisitStorage instance;

	private SharedPreferences sharedPreferences;
	private Gson gson = new Gson();

	private VenueVisitStorage(Context context) {
		try {
			String KEY_ALIAS = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
			sharedPreferences = EncryptedSharedPreferences.create(KEY_CROWDNOTIFIER_STORE,
					KEY_ALIAS,
					context,
					EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
					EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
		} catch (GeneralSecurityException | IOException ex) {
			ex.printStackTrace();
		}
	}

	public static synchronized VenueVisitStorage getInstance(Context context) {
		if (instance == null) {
			instance = new VenueVisitStorage(context);
		}
		return instance;
	}

	/**
	 * Sets a single ID to all entries in the passed list and stores all entries.
	 * @return the ID
	 */
	public long addEntries(List<EncryptedVenueVisit> newVenueVisits){
		List<EncryptedVenueVisit> venueVisitList = getEntries();
		long newId = getMaxId(venueVisitList) + 1;
		for (EncryptedVenueVisit newVenueVisit : newVenueVisits){
			newVenueVisit.setId(newId);
			venueVisitList.add(newVenueVisit);
		}
		saveToPrefs(venueVisitList);
		return newId;
	}

	public boolean updateEntries(List<EncryptedVenueVisit> newVenueVisits) {
		if (newVenueVisits == null || newVenueVisits.isEmpty()) return false;
		List<EncryptedVenueVisit> venueVisitList = getEntries();
		List<EncryptedVenueVisit> oldEntries = getVenueVisitsWithId(venueVisitList, newVenueVisits.get(0).getId());
		if (oldEntries.isEmpty()) return false;
		venueVisitList.removeAll(oldEntries);
		venueVisitList.addAll(newVenueVisits);
		saveToPrefs(venueVisitList);
		return true;
	}


	public List<EncryptedVenueVisit> getEntries() {
		return gson.fromJson(sharedPreferences.getString(KEY_VENUE_VISITS, "[]"), VENUE_LIST_TYPE);
	}

	public void removeEntriesBefore(int maxDaysToKeep) {
		List<EncryptedVenueVisit> venueVisitList = getEntries();
		DayDate lastDateToKeep = new DayDate().subtractDays(maxDaysToKeep);
		Iterator<EncryptedVenueVisit> iterator = venueVisitList.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getDayDate().isBefore(lastDateToKeep)) {
				iterator.remove();
			}
		}
		saveToPrefs(venueVisitList);
	}

	private long getMaxId(List<EncryptedVenueVisit> venueVisitList) {
		long maxId = 0;
		for (EncryptedVenueVisit venueVisit : venueVisitList) {
			if (venueVisit.getId() > maxId) {
				maxId = venueVisit.getId();
			}
		}
		return maxId;
	}

	private List<EncryptedVenueVisit> getVenueVisitsWithId(List<EncryptedVenueVisit> venueVisitList, long id) {
		ArrayList<EncryptedVenueVisit> result = new ArrayList<>();
		for (EncryptedVenueVisit venueVisit : venueVisitList) {
			if (venueVisit.getId() == id) {
				result.add(venueVisit);
			}
		}
		return result;
	}

	private void saveToPrefs(List<EncryptedVenueVisit> venueVisitList) {
		sharedPreferences.edit().putString(KEY_VENUE_VISITS, gson.toJson(venueVisitList)).apply();
	}

	public void clear() {
		saveToPrefs(new ArrayList<>());
	}

}
