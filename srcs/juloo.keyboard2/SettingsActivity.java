package juloo.keyboard2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SettingsActivity extends PreferenceActivity
{
  static final int REQ_EXPORT = 1;
  static final int REQ_IMPORT = 2;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    // The preferences can't be read when in direct-boot mode. Avoid crashing
    // and don't allow changing the settings.
    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
    addPreferencesFromResource(R.xml.settings);

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    findPreference("margin_bottom_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("margin_bottom_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_landscape_unfolded").setEnabled(foldableDevice);

    findPreference("config_export").setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener()
        {
          @Override
          public boolean onPreferenceClick(Preference pref)
          {
            startExport();
            return true;
          }
        });

    findPreference("config_import").setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener()
        {
          @Override
          public boolean onPreferenceClick(Preference pref)
          {
            startImport();
            return true;
          }
        });
  }

  void startExport()
  {
    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/json");
    intent.putExtra(Intent.EXTRA_TITLE, "unexpected_keyboard_config.json");
    startActivityForResult(intent, REQ_EXPORT);
  }

  void startImport()
  {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    startActivityForResult(intent, REQ_IMPORT);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode != Activity.RESULT_OK || data == null) return;
    Uri uri = data.getData();
    if (uri == null) return;
    if (requestCode == REQ_EXPORT)
      performExport(uri);
    else if (requestCode == REQ_IMPORT)
      performImport(uri);
  }

  void performExport(Uri uri)
  {
    try
    {
      SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
      String json = ConfigExportImport.export(prefs);
      OutputStream os = getContentResolver().openOutputStream(uri);
      OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
      w.write(json);
      w.close();
      Toast.makeText(this, R.string.config_export_success, Toast.LENGTH_SHORT).show();
    }
    catch (Exception e)
    {
      Toast.makeText(this, R.string.config_io_error, Toast.LENGTH_LONG).show();
    }
  }

  void performImport(Uri uri)
  {
    try
    {
      StringBuilder sb = new StringBuilder();
      InputStream is = getContentResolver().openInputStream(uri);
      BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      String line;
      while ((line = r.readLine()) != null)
        sb.append(line).append('\n');
      r.close();
      SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
      ConfigExportImport.importPrefs(prefs, sb.toString());
      // Run migrations in case the imported config is from an older version.
      Config.migrate(prefs);
      DirectBootAwarePreferences.copy_preferences_to_protected_storage(this, prefs);
      Toast.makeText(this, R.string.config_import_success, Toast.LENGTH_SHORT).show();
      recreate();
    }
    catch (Exception e)
    {
      Toast.makeText(this, R.string.config_io_error, Toast.LENGTH_LONG).show();
    }
  }

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
    finish();
  }

  protected void onStop()
  {
    DirectBootAwarePreferences
      .copy_preferences_to_protected_storage(this,
          getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
