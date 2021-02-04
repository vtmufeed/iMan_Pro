package com.grand.imanpro;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LabelSelectPrinter extends AppCompatActivity {
    public String SOAP_ACTION = "";

    public  String OPERATION_NAME = "";

    public  final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";

    public  String SOAP_ADDRESS = "http://grasshoppernetwork.com/NewFile.asmx";
    SQLiteDatabase db = null;
    public String loc="",lot_no="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_label_select_printer);
        super.onCreate(savedInstanceState);
        if(isNetworkAvailable()) {
            db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
            loadPrinter();
        }
        else
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Please turn on your DATA or WIFI", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }
        @SuppressLint("ResourceType") ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item,R.array.lbl_type);
        Spinner spinner = (Spinner) findViewById(R.id.spinner_lbl_type);
        spinner.setAdapter(adapter);

        spinner = (Spinner) findViewById(R.id.spinner_printer);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Spinner printer = (Spinner) findViewById(R.id.spinner_printer);
                Spinner lbl_type = (Spinner) findViewById(R.id.spinner_lbl_type);
                if(printer.getSelectedItem().toString().toUpperCase().contains("SHELF"))
                {

                    lbl_type.setSelection(((ArrayAdapter<String>)lbl_type.getAdapter()).getPosition("SH - SHELF LABEL"));
                }
                else
                    lbl_type.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
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
    public void loadLblTypes(){
        OPERATION_NAME="getLabelTypes";
        new MyTask().execute();
    }
    public void loadPrinter()
    {
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if(cursor.getCount()>0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                loc=cursor.getString(cursor.getColumnIndex("loc"));
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                String ip=ips[0];
                SOAP_ADDRESS="http://"+ip+"/iManWebService/Service.asmx";
                cursor.moveToNext();
            }
            cursor.close();
        }
        if(!loc.isEmpty())
        {
            OPERATION_NAME="getPrintersLoc";
            new MyTask().execute(loc);
        }
    }
    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("getPrintersLoc")) {
                SOAP_ACTION = "http://tempuri.org/getPrintersLoc";
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc");
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
                    response = exception.toString();
                }
            }
            if (OPERATION_NAME.equals("getLabelTypes")) {
                SOAP_ACTION = "http://tempuri.org/getLabelTypes";
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
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
                    response = exception.toString();
                }
            }
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @Override
        protected void onPostExecute(String result) {
            if (OPERATION_NAME.equals("getPrintersLoc")) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add("Choose Printer...");
                System.out.println(result);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray leaders = json.getJSONArray("LABEL_PRINTERS");
                    Integer j = 0;
                    for (int i = 0; i <= leaders.length(); i++) {
                        j++;
                        JSONObject jsonas = leaders.getJSONObject(i);
                        String fname = jsonas.getString("LBL_PRINTER_ID") + " - " + jsonas.getString("LBL_PRINTER_NAME");
                        System.out.println(fname);
                        arrayList.add(fname);
                    }
                } catch (JSONException e) {
                    System.out.println(e.getMessage());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item,arrayList);
                adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                Spinner spinner = (Spinner) findViewById(R.id.spinner_printer);
                spinner.setAdapter(adapter);
                spinner.setSelection(0);
                loadLblTypes();
            }
            if (OPERATION_NAME.equals("getLabelTypes")) {
                ArrayList<String> arrayList = new ArrayList<>();
                System.out.println(result);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray leaders = json.getJSONArray("LABEL_TYPES");
                    Integer j = 0;
                    for (int i = 0; i <= json.length()+1; i++) {
                        j++;
                        JSONObject jsonas = leaders.getJSONObject(i);
                        String fname = jsonas.getString("LBL_TYPE_ID") + " - " + jsonas.getString("LBL_TYPE_DESC");
                        System.out.println(fname);
                        arrayList.add(fname);
                    }
                } catch (JSONException e) {
                    System.out.println(e.getMessage());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item,arrayList);
                adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                Spinner spinner = (Spinner) findViewById(R.id.spinner_lbl_type);
                spinner.setAdapter(adapter);
                spinner.setSelection(0);
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
    public void nextClicked(View view)
    {
        System.out.println("Next Clicked");
        Spinner spinner=(Spinner)findViewById(R.id.spinner_printer);
        String printer=spinner.getSelectedItem().toString();
        if(printer.equals("Choose Printer...")) {
            System.out.println("Please Choose a Printer!");
            Toast.makeText(this, "Please Choose a Printer!", Toast.LENGTH_SHORT).show();
        }
        else {
            String[] array=spinner.getSelectedItem().toString().split("-");
            String lot_printer=array[0].trim();

            spinner=(Spinner) findViewById(R.id.spinner_lbl_type);
            array=spinner.getSelectedItem().toString().split("-");
            String lot_lbl_type=array[0].trim();
/*
            spinner=(Spinner)findViewById(R.id.spinner_lbl_type);
            String lot_lbl_type=spinner.getSelectedItem().toString();
            if(lot_lbl_type.equals("BARCODE LABEL WITH PRICE"))
                lot_lbl_type="BW";
            if(lot_lbl_type.equals("BARCODE LABEL WITHOUT PRICE"))
                lot_lbl_type="BWO";
            if(lot_lbl_type.equals("SHELF LABEL"))
                lot_lbl_type="SH";*/

            Intent intent = new Intent(getApplicationContext(), LabelScan.class);
            intent.putExtra("lot_printer", lot_printer);
            intent.putExtra("label_type", lot_lbl_type);
            startActivity(intent);
        }
    }
    public void homeClicked(View view)
    {
        Intent intent =new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }
}
