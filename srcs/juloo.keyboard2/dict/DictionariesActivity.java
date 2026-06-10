package juloo.keyboard2.dict;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import juloo.keyboard2.R;

public class DictionariesActivity extends Activity
{
  static final int REQ_IMPORT_FILE = 1;

  DictionaryListView _list_view;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dictionaries_activity);
    _list_view = (DictionaryListView)findViewById(R.id.dict_list_view);

    findViewById(R.id.dict_import_file_btn).setOnClickListener(
        new View.OnClickListener()
        {
          @Override
          public void onClick(View v) { startImportFromFile(); }
        });

    findViewById(R.id.dict_import_url_btn).setOnClickListener(
        new View.OnClickListener()
        {
          @Override
          public void onClick(View v) { showImportUrlDialog(); }
        });
  }

  void startImportFromFile()
  {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    startActivityForResult(intent, REQ_IMPORT_FILE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode != Activity.RESULT_OK || data == null) return;
    Uri uri = data.getData();
    if (uri == null || requestCode != REQ_IMPORT_FILE) return;
    String filename = get_display_name(uri);
    String locale = detect_locale(filename);
    if (locale != null)
      _list_view.install_from_uri(uri, locale);
    else
      show_locale_dialog(new UriInstallCallback(uri));
  }

  void showImportUrlDialog()
  {
    final EditText input = new EditText(this);
    input.setHint(R.string.dict_import_url_hint);
    new AlertDialog.Builder(this)
      .setTitle(R.string.dict_import_url_title)
      .setView(input)
      .setPositiveButton(android.R.string.ok,
          new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface d, int which)
            {
              String url = input.getText().toString().trim();
              if (url.isEmpty()) return;
              String filename = url.substring(url.lastIndexOf('/') + 1);
              String locale = detect_locale(filename);
              if (locale != null)
                _list_view.install_from_url(url, locale);
              else
                show_locale_dialog(new UrlInstallCallback(url));
            }
          })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  /** Show a dialog listing all supported locales for the user to pick. */
  void show_locale_dialog(final LocaleCallback callback)
  {
    final SupportedDictionaries ds = new SupportedDictionaries(getResources());
    new AlertDialog.Builder(this)
      .setTitle(R.string.dict_import_select_locale)
      .setItems(ds.names,
          new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface d, int which)
            {
              callback.install(ds.locales[which]);
            }
          })
      .setNegativeButton(android.R.string.cancel, null)
      .show();
  }

  /** Extract the display name from a content URI. */
  String get_display_name(Uri uri)
  {
    Cursor cursor = getContentResolver().query(
        uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
    if (cursor != null && cursor.moveToFirst())
    {
      String name = cursor.getString(0);
      cursor.close();
      if (name != null) return name;
    }
    String path = uri.getLastPathSegment();
    return (path != null) ? path : "";
  }

  /** Return the locale name if the filename (minus .dict) is a known locale, else null. */
  String detect_locale(String filename)
  {
    if (filename == null || filename.isEmpty()) return null;
    String name = filename.endsWith(".dict")
      ? filename.substring(0, filename.length() - 5)
      : filename;
    SupportedDictionaries ds = new SupportedDictionaries(getResources());
    return (ds.find(name) >= 0) ? name : null;
  }

  interface LocaleCallback { void install(String locale); }

  final class UriInstallCallback implements LocaleCallback
  {
    final Uri _uri;
    UriInstallCallback(Uri uri) { _uri = uri; }
    @Override public void install(String locale)
    { _list_view.install_from_uri(_uri, locale); }
  }

  final class UrlInstallCallback implements LocaleCallback
  {
    final String _url;
    UrlInstallCallback(String url) { _url = url; }
    @Override public void install(String locale)
    { _list_view.install_from_url(_url, locale); }
  }
}
