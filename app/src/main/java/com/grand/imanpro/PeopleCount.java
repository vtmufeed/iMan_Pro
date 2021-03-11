package com.grand.imanpro;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class PeopleCount extends AppCompatActivity {
    Integer people_strength=0;
    SQLiteDatabase db = null;
    String ip="",loc="";
    public String SOAP_ACTION = "";
    public  String OPERATION_NAME = "";
    public  final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public  String SOAP_ADDRESS = "";
    public String pending_lot="";
    ProgressDialog dialog;
    public String user="";
    android.support.v7.app.AlertDialog alert_dialog;
    MediaPlayer player;
    //private AlertDialog progressDialog;
    String barcode="",prod_code="",su="",price="",arabic="",eng="",count="", conv="",stock="",slno="",lot_id="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_people_count);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                loc = cursor.getString(cursor.getColumnIndex("loc"));
                people_strength = Integer.parseInt(cursor.getString(cursor.getColumnIndex("people_strength")));
                String ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
                TextView txt=(TextView)findViewById(R.id.txt_capacity);
                txt.setText("Maximum Capacity: "+people_strength);
            }
        }
        OPERATION_NAME="pplCountInsertData";
        new MyTask().execute("0");
    }
    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            dialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("pplCountInsertData")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc_code");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("val");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("max");
                pi.setType(String.class);
                pi.setValue(people_strength);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;

                envelope.setOutputSoapObject(request);

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS);
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
            System.out.println(result);
            if (OPERATION_NAME.equals("pplCountInsertData")) {
                if(result.contains("success")) {
                    System.out.println("success");
                    int people_inside= Integer.parseInt(result.split(",")[1]);
                    EditText edt = (EditText) findViewById(R.id.txt_ppl_inside);
                    edt.setText(Integer.toString(people_inside));
                    edt = (EditText) findViewById(R.id.txt_slot_avail);
                    edt.setText(Integer.toString(people_strength-people_inside));
                    System.out.println("Error Flag : "+result.split(",")[2]);
                    if(result.split(",")[2].equals("1"))
                    {
                        Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_SHORT).show();
                        player = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
                        player.start();
                    }
                }
                else
                {
                    Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
                }
            }
            dialog.dismiss();
        }
    }
    public String getUser() {
        String user_id = "", user_name = "";
        SQLiteDatabase db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from current_user", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                user_id = cursor.getString(cursor.getColumnIndex("id"));
                user_name = cursor.getString(cursor.getColumnIndex("name"));
            }
        }
        return user_id;
    }
    public void addPressed(View view)
    {
        OPERATION_NAME="pplCountInsertData";
        new MyTask().execute("1");
    }
    public void minusPressed(View view)
    {
        OPERATION_NAME="pplCountInsertData";
        new MyTask().execute("-1");
    }
    public void refreshPressed(View view)
    {
        OPERATION_NAME="pplCountInsertData";
        new MyTask().execute("0");
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
}
