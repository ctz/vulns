package com.example.keystore_generate;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Calendar;

import javax.security.auth.x500.X500Principal;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.security.KeyPairGeneratorSpec;
import android.util.Log;
import android.widget.TextView;
import android.app.Activity;

public class MainActivity extends Activity
{
  private static final String TAG = "keystore_generate"; 
  
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    log("my uid is " + Process.myUid());

    if (getKey() != null)
    {
      log("key already exists:");
      log(getKey().toString());
    } else {
      generateKeyAsync();
    }
  }
  
  KeyStore.Entry getKey()
  {
    try
    {
      KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
      ks.load(null);
      return ks.getEntry("key", null);
    } catch (Exception e) {
      Log.wtf(TAG, "getKey failed", e);
      return null;
    }
  }

  private void log(String s)
  {
    TextView tv = (TextView) findViewById(R.id.log);
    tv.setText(tv.getText() + "\n" + s);
  }

  private void generateKeyAsync()
  {
    new AsyncTask<Void, Void, Void>()
    {
      
      @Override
      protected void onPreExecute()
      {
        log("starting key generation...");
      }
      
      @Override
      protected void onPostExecute(Void r)
      {
        log("key generation complete");
        log(getKey().toString());
      }

      @Override
      protected Void doInBackground(Void... params)
      {
        KeyPairGeneratorSpec spec;
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 10);

        try
        {
          spec = new KeyPairGeneratorSpec.Builder(MainActivity.this)
              .setAlias("key")
              .setSubject(new X500Principal("CN=whatever"))
              .setStartDate(start.getTime())
              .setEndDate(end.getTime())
              .setSerialNumber(BigInteger.valueOf(1))
              .build();
        } catch (Exception e)
        {
          Log.wtf(TAG, "Cannot make a KeyPairGeneratorSpec", e);
          throw new Error("Cannot make a KeyPairGeneratorSpec", e);
        }

        KeyPair kp = null;

        try
        {
          KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
          kpg.initialize(spec);
          kp = kpg.generateKeyPair();
        } catch (Exception e)
        {
          Log.wtf(TAG, "Cannot generate key", e);
          throw new Error("Cannot generate key", e);
        }
        return null;
      }
      
    }.execute();
  }
}
