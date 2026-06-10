package juloo.keyboard2;

import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ConfigExportImport
{
  static final int FORMAT_VERSION = 1;

  /** Keys that are internal state and should not be round-tripped. */
  private static final Set<String> EXCLUDED_KEYS = new HashSet<String>()
  {{
    add("need_migration");
  }};

  /** Serialise all shareable preferences to a pretty-printed JSON string. */
  public static String export(SharedPreferences prefs) throws JSONException
  {
    JSONObject root = new JSONObject();
    root.put("format_version", FORMAT_VERSION);
    JSONObject entries = new JSONObject();
    for (Map.Entry<String, ?> e : prefs.getAll().entrySet())
    {
      String key = e.getKey();
      if (EXCLUDED_KEYS.contains(key)) continue;
      Object val = e.getValue();
      JSONObject entry = encodeEntry(val);
      if (entry != null)
        entries.put(key, entry);
    }
    root.put("prefs", entries);
    return root.toString(2);
  }

  /** Apply a previously exported JSON string back into SharedPreferences. */
  public static void importPrefs(SharedPreferences prefs, String json)
      throws JSONException
  {
    JSONObject root = new JSONObject(json);
    JSONObject entries = root.getJSONObject("prefs");
    SharedPreferences.Editor editor = prefs.edit();
    JSONArray keys = entries.names();
    if (keys == null) return;
    for (int i = 0; i < keys.length(); i++)
    {
      String key = keys.getString(i);
      if (EXCLUDED_KEYS.contains(key)) continue;
      JSONObject entry = entries.getJSONObject(key);
      decodeEntry(editor, key, entry);
    }
    editor.apply();
  }

  private static JSONObject encodeEntry(Object val) throws JSONException
  {
    if (val == null) return null;
    JSONObject entry = new JSONObject();
    if (val instanceof Boolean)
    {
      entry.put("type", "boolean");
      entry.put("value", (Boolean)val);
    }
    else if (val instanceof Float)
    {
      entry.put("type", "float");
      entry.put("value", (double)(Float)val);
    }
    else if (val instanceof Integer)
    {
      entry.put("type", "int");
      entry.put("value", (Integer)val);
    }
    else if (val instanceof Long)
    {
      entry.put("type", "long");
      entry.put("value", (Long)val);
    }
    else if (val instanceof String)
    {
      entry.put("type", "string");
      entry.put("value", (String)val);
    }
    else if (val instanceof Set)
    {
      entry.put("type", "stringset");
      JSONArray arr = new JSONArray();
      for (String s : (Set<String>)val)
        arr.put(s);
      entry.put("value", arr);
    }
    else return null;
    return entry;
  }

  private static void decodeEntry(SharedPreferences.Editor editor,
      String key, JSONObject entry) throws JSONException
  {
    String type = entry.getString("type");
    switch (type)
    {
      case "boolean":
        editor.putBoolean(key, entry.getBoolean("value")); break;
      case "float":
        editor.putFloat(key, (float)entry.getDouble("value")); break;
      case "int":
        editor.putInt(key, entry.getInt("value")); break;
      case "long":
        editor.putLong(key, entry.getLong("value")); break;
      case "string":
        editor.putString(key, entry.getString("value")); break;
      case "stringset":
        JSONArray arr = entry.getJSONArray("value");
        Set<String> set = new HashSet<>();
        for (int j = 0; j < arr.length(); j++)
          set.add(arr.getString(j));
        editor.putStringSet(key, set);
        break;
    }
  }
}
