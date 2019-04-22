package design.cornholio;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

public class PreferenceFragment extends PreferenceFragmentCompat {

  public static final String TAG = PreferenceFragment.class.getSimpleName();

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.preferences, rootKey);
  }
}