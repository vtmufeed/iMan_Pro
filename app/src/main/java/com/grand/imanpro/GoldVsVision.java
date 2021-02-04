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
import android.widget.CheckBox;
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

public class GoldVsVision extends AppCompatActivity {
    SQLiteDatabase db = null;
    String ip = "", loc = "", seq_slno = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "", user = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gold_vs_vision);

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
                    if (!edittext.getText().toString().isEmpty())
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
                OPERATION_NAME = "GoldVsVision";
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
        Integer fl = 0;
        Object response = null;

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
            pi.setName("loc_code");
            pi.setType(String.class);
            pi.setValue(params[1]);
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("barcode");
            pi.setType(String.class);
            pi.setValue(params[0]);
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
                EditText edt = (EditText) findViewById(R.id.txt_barcode);
                edt.setText("");
                edt.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            } else if (result.toUpperCase().contains("error")) {
                Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            } else {
                //btn.setEnabled(true);
                String product_code = "", uom = "", rsp = "", desc = "", gold_uom = "", gold_price = "", gold_desc = "";
                EditText txt_prod_code = (EditText) findViewById(R.id.txt_prod_code);
                EditText txt_barcode = (EditText) findViewById(R.id.txt_barcode);
                EditText txt_vision_desc = (EditText) findViewById(R.id.txt_vision_desc);
                EditText txt_gold_desc = (EditText) findViewById(R.id.txt_gold_desc);
                EditText txt_vision_prc = (EditText) findViewById(R.id.txt_price);
                EditText txt_gold_prc = (EditText) findViewById(R.id.txt_conv);
                EditText txt_vision_uom = (EditText) findViewById(R.id.txt_vision_uom);
                EditText txt_gold_uom = (EditText) findViewById(R.id.txt_gold_uom);
                if (fl != 0) {
                    Toast toast = Toast.makeText(getApplicationContext(), "unable to connect server", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    //clear();
                    //btn.setEnabled(false);
                } else {
                    try {
                        JSONObject jsonRootObject = new JSONObject(result);
                        product_code = jsonRootObject.getString("itemcode");
                        uom = jsonRootObject.getString("uom");
                        rsp = jsonRootObject.getString("rsp");
                        desc = jsonRootObject.getString("desc");
                        gold_uom = jsonRootObject.getString("gold_uom");
                        gold_price = jsonRootObject.getString("gold_price");
                        gold_desc = jsonRootObject.getString("gold_desc");
                        seq_slno = jsonRootObject.getString("seq_slno");
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        //btn.setEnabled(false);
                    }
                    txt_prod_code.setText(product_code);
                    txt_vision_prc.setText(rsp);
                    txt_vision_uom.setText(uom);
                    txt_vision_desc.setText(desc);

                    txt_gold_prc.setText(gold_price);
                    txt_gold_uom.setText(gold_uom);
                    txt_gold_desc.setText(gold_desc);

                    txt_barcode.setEnabled(false);
                    CheckBox chk_gold = (CheckBox) findViewById(R.id.chk_gd);
                    CheckBox chk_vision = (CheckBox) findViewById(R.id.chk_vs);
                    if (gold_desc.contains("NOT FOUND")) {
                        chk_gold.setEnabled(false);
                        chk_vision.setEnabled(false);
                    } else {
                        chk_gold.setEnabled(true);
                        chk_vision.setEnabled(true);
                    }
                }
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void ClearPressed(View view) {
        clear();
    }

    public void clear() {
        EditText edt = (EditText) findViewById(R.id.txt_vision_desc);
        String vs_desc = edt.getText().toString();
        edt = (EditText) findViewById(R.id.txt_gold_desc);
        String gd_desc = edt.getText().toString();
        CheckBox chk_vision = (CheckBox) findViewById(R.id.chk_vs);
        CheckBox chk_gold = (CheckBox) findViewById(R.id.chk_gd);
        EditText barcode = (EditText) findViewById(R.id.txt_barcode);
        if (vs_desc != null && !vs_desc.isEmpty() && gd_desc != null && !gd_desc.isEmpty() && !gd_desc.contains("NOT FOUND")) {
            if (barcode.getText().toString() != null && !barcode.getText().toString().isEmpty()) {
                String act_desc = "";
                if (chk_vision.isChecked())
                    act_desc = "VISION";
                else if (chk_gold.isChecked())
                    act_desc = "GOLD";
                else
                    act_desc = "";
                OPERATION_NAME="GoldVsVisionSave";
                new MyTask1().execute(barcode.getText().toString(), act_desc, seq_slno);
            }
        }
        EditText item_code = (EditText) findViewById(R.id.txt_prod_code);
        barcode = (EditText) findViewById(R.id.txt_barcode);
        EditText cost = (EditText) findViewById(R.id.txt_price);
        EditText uom = (EditText) findViewById(R.id.txt_vision_uom);
        EditText prod_desc = (EditText) findViewById(R.id.txt_vision_desc);

        EditText GD_prod_desc = (EditText) findViewById(R.id.txt_gold_desc);
        EditText GD_prod_PRICE = (EditText) findViewById(R.id.txt_conv);
        EditText GD_uom = (EditText) findViewById(R.id.txt_gold_uom);
        // EditText curr_stk=(EditText) findViewById(R.id.Ed_curr_stk);
        item_code.setText("");
        barcode.setText("");
        cost.setText("");
        uom.setText("");
        prod_desc.setText("");
        GD_prod_PRICE.setText("");
        GD_uom.setText("");
        GD_prod_desc.setText("");
        //  curr_stk.setText("");
        barcode.setEnabled(true);
        barcode.setCursorVisible(true);
        barcode.requestFocus();
        chk_vision.setChecked(false);
        chk_gold.setChecked(false);
        //Button btn=findViewById(R.id.btnSaveSame);
        //btn.setEnabled(false);
    }

    public void chk_vs_checked(View view) {
        CheckBox chk = (CheckBox) findViewById(R.id.chk_vs);
        if (chk.isChecked()) {
            chk = (CheckBox) findViewById(R.id.chk_gd);
            chk.setChecked(false);
        }
    }

    public void chk_gd_checked(View view) {
        CheckBox chk = (CheckBox) findViewById(R.id.chk_gd);
        if (chk.isChecked()) {
            chk = (CheckBox) findViewById(R.id.chk_vs);
            chk.setChecked(false);
        }
    }

    private class MyTask1 extends AsyncTask<String, String, String> {
        String act_desc = "";
        Object response = null;
        public int fl = 0;
        public String flag = "";
        URL url;

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
            pi.setName("loc");
            pi.setType(String.class);
            pi.setValue(loc);
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("act_desc");
            pi.setType(String.class);
            pi.setValue(params[1]);
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("slno");
            pi.setType(String.class);
            pi.setValue(params[2]);
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
            if (result.contains("success")) {
                Toast.makeText(getApplicationContext(), "Saved!", Toast.LENGTH_LONG).show();
                //btn.setEnabled(false);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            } else {
                //btn.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Error! " + result, Toast.LENGTH_LONG).show();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public URI appendUri(String uri, String appendQuery) throws URISyntaxException {
        URI oldUri = new URI(uri);
        String newQuery = oldUri.getQuery();
        if (newQuery == null) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;
        }
        URI newUri = new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment());
        return newUri;
    }
}
