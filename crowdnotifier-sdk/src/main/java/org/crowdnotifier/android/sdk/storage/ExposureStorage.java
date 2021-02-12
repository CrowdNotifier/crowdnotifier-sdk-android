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
import org.crowdnotifier.android.sdk.model.ExposureEvent;

public class ExposureStorage {

	private static final String KEY_CROWDNOTIFIER_STORE = "KEY_CROWDNOTIFIER_STORE";
	private static final String KEY_EXPOSURES = "KEY_EXPOSURES";
	private static final Type EXPOSURE_LIST_TYPE = new TypeToken<ArrayList<ExposureEvent>>() { }.getType();

	private static ExposureStorage instance;

	private SharedPreferences sharedPreferences;
	private Gson gson = new Gson();

	private ExposureStorage(Context context) {
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

	public static synchronized ExposureStorage getInstance(Context context) {
		if (instance == null) {
			instance = new ExposureStorage(context);
		}
		return instance;
	}

	public boolean addEntry(ExposureEvent exposureEvent) {
		List<ExposureEvent> exposureEventList = getEntries();
		if (hasExposureWithId(exposureEvent.getId())) return false;
		exposureEventList.add(exposureEvent);
		saveToPrefs(exposureEventList);
		return true;
	}

	public boolean hasExposureWithId(long id) {
		return getExposureWithId(id) != null;
	}

	public ExposureEvent getExposureWithId(long id) {
		for (ExposureEvent exposureEvent : getEntries()) {
			if (exposureEvent.getId() == id) {
				return exposureEvent;
			}
		}
		return null;
	}

	public List<ExposureEvent> getEntries() {
		return gson.fromJson(sharedPreferences.getString(KEY_EXPOSURES, "[]"), EXPOSURE_LIST_TYPE);
	}

	public void removeEntriesBefore(int maxDaysToKeep) {
		List<ExposureEvent> exposureEventList = getEntries();
		DayDate lastDateToKeep = new DayDate().subtractDays(maxDaysToKeep);
		Iterator<ExposureEvent> iterator = exposureEventList.iterator();
		while (iterator.hasNext()) {
			if (new DayDate(iterator.next().getEndTime()).isBefore(lastDateToKeep)) {
				iterator.remove();
			}
		}
		saveToPrefs(exposureEventList);
	}

	public void removeExposure(long id) {
		List<ExposureEvent> exposureEventList = getEntries();
		Iterator<ExposureEvent> iterator = exposureEventList.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getId() == id) {
				iterator.remove();
				break;
			}
		}
		saveToPrefs(exposureEventList);
	}

	private void saveToPrefs(List<ExposureEvent> exposureEventList) {
		sharedPreferences.edit().putString(KEY_EXPOSURES, gson.toJson(exposureEventList)).apply();
	}

	public void clear() {
		saveToPrefs(new ArrayList<>());
	}

}
