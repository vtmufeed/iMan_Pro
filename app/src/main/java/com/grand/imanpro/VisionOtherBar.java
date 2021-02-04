package com.grand.imanpro;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static android.widget.Toast.LENGTH_SHORT;

public class VisionOtherBar extends AppCompatActivity {
    SQLiteDatabase db = null;
    String ip = "", loc = "", seq_slno = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "", user = "";
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vision_other_bar);
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
                loc = cursor.getString(cursor.getColumnIndex("loc"));
            }
            if (loc == null)
                Toast.makeText(this, "Location is not Set! Please contact system administrator", LENGTH_SHORT).show();
        }
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.INVISIBLE);
        final EditText edittext = (EditText) findViewById(R.id.txt_barcode);
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if(!edittext.getText().toString().isEmpty())
                        enter();
                    return true;
                }
                return false;
            }
        });
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
    public void enter() {
        EditText e2 = (EditText) findViewById(R.id.txt_barcode);
        if (!e2.getEditableText().toString().equals("")) {
            if (isNetworkAvailable()) {
                EditText editText = (EditText) findViewById(R.id.txt_barcode);
                OPERATION_NAME = "GoldVsVisionOtherBar";
                new MyTask().execute(editText.getText().toString(), loc);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please turn on your DATA or WIFI", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }
        }
    }
    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        Object response=null;
        Integer fl=0;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

            PropertyInfo pi = new PropertyInfo();
            pi.setName("barcode");
            pi.setType(String.class);
            pi.setValue(params[0]);
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("loc_code");
            pi.setType(String.class);
            pi.setValue(params[1]);
            request.addProperty(pi);

            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                    SoapEnvelope.VER11);
            envelope.dotNet = true;

            envelope.setOutputSoapObject(request);

            HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS);
            System.out.println(SOAP_ADDRESS);


            try {
                httpTransport.call(SOAP_ACTION, envelope);
                response = envelope.getResponse();
            } catch (Exception exception) {
                response = "error " + exception.toString();
            }
            return response.toString();
        }

        @Override
        protected void onProgressUpdate(String... value) {
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(result);
            //Button btn=findViewById(R.id.btnSaveSame);
            if (result.contains("Barcode Not Found")) {
                Toast.makeText(getApplicationContext(), "Barcode Does not Exist in Vision", Toast.LENGTH_LONG).show();
                //btn.setEnabled(false);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);
            }
            else
            {
                //btn.setEnabled(true);
                String desc = "",oth_bar= "";
                EditText txt_desc = (EditText) findViewById(R.id.txt_prod_desc);
                EditText txt_oth_bar = (EditText) findViewById(R.id.txt_oth_bar);
                if (fl != 0) {
                    Toast toast = Toast.makeText(getApplicationContext(), "unable to connect server", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    //clear();
                    //btn.setEnabled(false);
                } else {
                    try {
                        JSONObject jsonRootObject = new JSONObject(result);
                        desc = jsonRootObject.getString("desc");
                        oth_bar = jsonRootObject.getString("bar");
                        txt_desc.setText(desc);
                        txt_oth_bar.setText(oth_bar.replace(",","\n"));
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Barcode Does not Exist ", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        //btn.setEnabled(false);
                    }
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
    public void ClearPressed(View view)
    {
        clear();
    }
    public void clear() {
        EditText edt=(EditText)findViewById(R.id.txt_barcode);
        edt.setText("");
        edt.requestFocus();
        edt=(EditText)findViewById(R.id.txt_prod_desc);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_oth_bar);
        edt.setText("");
    }
}