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
import org.crowdnotifier.android.sdk.model.Exposure;

public class ExposureStorage {

	private static final String KEY_CROWDNOTIFIER_STORE = "KEY_CROWDNOTIFIER_STORE";
	private static final String KEY_EXPOSURES = "KEY_EXPOSURES";
	private static final Type EXPOSURE_LIST_TYPE = new TypeToken<ArrayList<Exposure>>() { }.getType();

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

	public boolean addEntry(Exposure exposure) {
		List<Exposure> exposureList = getEntries();
		if (hasExposureWithId(exposure.getId())) return false;
		exposureList.add(exposure);
		saveToPrefs(exposureList);
		return true;
	}

	public boolean hasExposureWithId(long id) {
		return getExposureWithId(id) != null;
	}

	public Exposure getExposureWithId(long id) {
		for (Exposure exposure : getEntries()) {
			if (exposure.getId() == id) {
				return exposure;
			}
		}
		return null;
	}

	public List<Exposure> getEntries() {
		return gson.fromJson(sharedPreferences.getString(KEY_EXPOSURES, "[]"), EXPOSURE_LIST_TYPE);
	}

	public void removeEntriesBefore(int maxDaysToKeep) {
		List<Exposure> exposureList = getEntries();
		DayDate lastDateToKeep = new DayDate().subtractDays(maxDaysToKeep);
		Iterator<Exposure> iterator = exposureList.iterator();
		while (iterator.hasNext()) {
			if (new DayDate(iterator.next().getEndTime()).isBefore(lastDateToKeep)) {
				iterator.remove();
			}
		}
		saveToPrefs(exposureList);
	}

	private void saveToPrefs(List<Exposure> exposureList) {
		sharedPreferences.edit().putString(KEY_EXPOSURES, gson.toJson(exposureList)).apply();
	}

	public void clear() {
		saveToPrefs(new ArrayList<>());
	}

}
