package design.cornholio;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

  public static final String BUSTING_PREFERENCE_KEY = "busting_preference";
  public static final String SCORE_LIMIT_PREFERENCE_KEY = "score_limit_preference";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.preferenceFragment, new PreferenceFragment())
            .commit();
    setContentView(R.layout.activity_settings);
  }
}