package com.grand.imanpro;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

public class Wh_rcv_po extends AppCompatActivity {
    SQLiteDatabase db = null;
    public String user = "", loc = "", mode = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    String date_picker_source = "";
    ProgressDialog dialog;
    LinearLayout layout_order_data;
    Button btn_next;
    String global_po_no = "", global_supplier = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wh_rcv_po);

        user = getUser();
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                loc = cursor.getString(cursor.getColumnIndex("loc"));
                String ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
            }
        }

        btn_next = (Button) findViewById(R.id.btn_next);
        layout_order_data = (LinearLayout) findViewById(R.id.layout_order_data);
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        final EditText edittext = (EditText) findViewById(R.id.txt_po_no);
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (!edittext.getText().toString().isEmpty()) {
                        OPERATION_NAME = "getWhRcvOrderDetl";
                        String barcode = edittext.getText().toString();
                        new MyTask().execute(barcode, loc);
                    }
                    return true;
                }
                return false;
            }
        });
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if(OPERATION_NAME.equals("insertWhRecDetl"))
            {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("po_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("wh_code");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("order_date");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("deliv_date");
                pi.setType(String.class);
                pi.setValue(params[3]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(params[4]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("wh_name");
                pi.setType(String.class);
                pi.setValue(params[5]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("cc");
                pi.setType(String.class);
                pi.setValue(params[6]);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                        SoapEnvelope.VER11);
                envelope.dotNet = true;

                envelope.setOutputSoapObject(request);

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS, 180000);

                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            if (OPERATION_NAME.equals("getWhRcvOrderDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("order_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                        SoapEnvelope.VER11);
                envelope.dotNet = true;

                envelope.setOutputSoapObject(request);

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS, 180000);

                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            System.out.println(SOAP_ADDRESS + " (" + OPERATION_NAME + ")");
            System.out.println(response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(),result,LENGTH_SHORT);
            if (OPERATION_NAME.equals("insertWhRecDetl")) {
                //Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
                if(result.equals("success")) {
                    Intent intent = new Intent(getApplicationContext(), Wh_rcv_scan.class);
                    intent.putExtra("po_no", global_po_no);
                    intent.putExtra("supplier", global_supplier);
                    startActivity(intent);
                }
                else
                    Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
            }
            //Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            if (OPERATION_NAME.equals("getWhRcvOrderDetl")) {
                if (result.contains("success")) {
                    layout_order_data.setVisibility(View.VISIBLE);
                    try {
                        JSONObject json = new JSONObject(result);
                        global_po_no = json.getString("ORDER");
                        String sup_code = json.getString("SUP_CODE");
                        String sup_name = json.getString("SUP_NAME");
                        String po_date = json.getString("PO_DATE");
                        String delv_date = json.getString("DELV_DATE");
                        String po_status = json.getString("PO_STATUS");
                        String loc = json.getString("LOC");
                        String loc_name = json.getString("LOC_NAME");
                        String po_status_num = json.getString("PO_STS_NUM");
                        String cc = json.getString("SUP_CC");

                        global_supplier = sup_code + " - " + sup_name;

                        TextView textView = (TextView) findViewById(R.id.txt_wh_code);
                        textView.setText(sup_code);
                        textView = (TextView) findViewById(R.id.txt_wh_name);
                        textView.setText(sup_name);
                        textView = (TextView) findViewById(R.id.txt_site_code);
                        textView.setText(loc);
                        textView = (TextView) findViewById(R.id.txt_site_name);
                        textView.setText(loc_name);
                        textView = (TextView) findViewById(R.id.txt_ord_date);
                        textView.setText(po_date);
                        textView = (TextView) findViewById(R.id.txt_deliv_date);
                        textView.setText(delv_date);
                        textView = (TextView) findViewById(R.id.txt_order_status_num);
                        textView.setText(po_status_num);
                        textView = (TextView) findViewById(R.id.txt_order_status);
                        textView.setText(po_status);
                        textView = (TextView) findViewById(R.id.txt_cc);
                        textView.setText(cc);
                    } catch (JSONException e) {
                        global_po_no = "";
                        global_supplier = "";
                        layout_order_data.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(), e.getMessage(), LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    btn_next.setEnabled(true);
                } else {
                    global_po_no = "";
                    global_supplier = "";
                    layout_order_data.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            dialog.dismiss();
            //Toast.makeText(getApplicationContext(),result,LENGTH_SHORT);
        }
    }

    public void NextPressed(View view) {
        if (global_po_no.length() > 0 && layout_order_data.getVisibility() == View.VISIBLE) {
            TextView txt_status=(TextView)findViewById(R.id.txt_order_status);
            if(txt_status.getText().toString().equals("Awaiting delivery")) {
                //Toast.makeText(getApplicationContext(), "Ok", LENGTH_SHORT).show();
                EditText edt=findViewById(R.id.txt_po_no);
                String po_no=edt.getText().toString();
                TextView textView=(findViewById(R.id.txt_wh_code));
                String wh_code=textView.getText().toString();
                textView=(findViewById(R.id.txt_ord_date));
                String order_date=textView.getText().toString();
                textView=(findViewById(R.id.txt_deliv_date));
                String deliv_date=textView.getText().toString();
                textView=(findViewById(R.id.txt_wh_name));
                String wh_name=textView.getText().toString();
                textView=(findViewById(R.id.txt_cc));
                String cc=textView.getText().toString();
                OPERATION_NAME="insertWhRecDetl";
                new MyTask().execute(po_no,wh_code,order_date,deliv_date,user,wh_name,cc);
            }
            else
                Toast.makeText(getApplicationContext(), "Order Status Should be 'Awaiting delivery'", LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Please Search for Order", LENGTH_SHORT).show();
        }
    }

    public void clearPressed(View view) {
        EditText edt = (EditText) findViewById(R.id.txt_po_no);
        edt.setText("");
        global_po_no = "";
        global_supplier = "";
        layout_order_data.setVisibility(View.GONE);
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    public String getUser() {
        String user = "";
        SQLiteDatabase db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from current_user", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                user = cursor.getString(cursor.getColumnIndex("id"));
            }
        }
        return user;
    }
}
