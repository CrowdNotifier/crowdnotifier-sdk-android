package org.crowdnotifier.android.sdk_test_app;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.crowdnotifier.android.sdk.CrowdNotifier;

public class MainFragment extends Fragment {

	private MainViewModel mViewModel;

	public static MainFragment newInstance() {
		return new MainFragment();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		CrowdNotifier.cleanupOldData(getContext(),14);
		return inflater.inflate(R.layout.main_fragment, container, false);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
		// TODO: Use the ViewModel
	}

}