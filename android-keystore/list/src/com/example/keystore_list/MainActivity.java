package com.example.keystore_list;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Enumeration;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.TextView;
import android.os.Process;

public class MainActivity extends Activity
{
  private static final String TAG = "keystore_list";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    log("my uid is " + Process.myUid());
    listKeys();
  }

  private void log(String s)
  {
    TextView tv = (TextView) findViewById(R.id.log);
    tv.setText(tv.getText() + "\n" + s);
  }

  private void listKeys()
  {
    try
    {
      KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
      ks.load(null);

      Enumeration<String> aliases = ks.aliases();
      ArrayList<String> names = new ArrayList<String>();
      while (aliases.hasMoreElements())
      {
        names.add(aliases.nextElement());
      }

      log("found " + names.size() + " keys");
      for (int i = 0; i < names.size(); i++)
      {
        log("key " + i + ":");
        log(ks.getEntry(names.get(i), null)
            .toString());
        log("=====");
      }
    } catch (Exception e)
    {
      Log.wtf(TAG, "listKeys failed", e);
    }
  }
}
