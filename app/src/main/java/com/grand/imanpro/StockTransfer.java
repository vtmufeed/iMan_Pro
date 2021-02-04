package com.grand.imanpro;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

public class StockTransfer extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {
    SQLiteDatabase db = null;
    public String user = "", loc = "", mode = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_transfer);

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
        loadNewNo();
    }

    public void loadNewNo() {
        OPERATION_NAME = "getTransPendNo";
        new MyTask().execute();
    }

    public void showDateDialog(View view) {
        showDatePicker();
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, this,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        EditText edt = (EditText) findViewById(R.id.txt_new_date);
        String mon = "";
        mon = new Ops().getMonthString(month);
        edt.setText(dayOfMonth + "-" + mon + "-" + year);
    }

    public void gotoStockTransScan() {
        Intent intent = new Intent(getApplicationContext(), StockTrasnferScan.class);
        String temp_tra_id = "";
        EditText edt = (EditText) findViewById(R.id.txt_new_tra_id);
        intent.putExtra("tra_id", edt.getText().toString());
        temp_tra_id = getDocNo();
        intent.putExtra("temp_tra_id", temp_tra_id);
        edt = (EditText) findViewById(R.id.txt_new_date);
        intent.putExtra("tra_date", edt.getText().toString());
        Spinner spinner = (Spinner) findViewById(R.id.spinner_new_tra_loc);
        intent.putExtra("tra_loc", spinner.getSelectedItem().toString().split("-")[0].trim());
        startActivity(intent);
    }

    public String getDocNo() {
        Spinner spinner = (Spinner) findViewById(R.id.spinner_new_tra_type);
        EditText edt = (EditText) findViewById(R.id.txt_new_tra_id);
        return spinner.getSelectedItem().toString().substring(0, 3).toUpperCase() + "/" + edt.getText().toString();
    }

    public void newNextPressed(View view) {
        if (mode.equals("new")) {
            EditText edt = (EditText) findViewById(R.id.txt_new_tra_id);
            String tra_id = edt.getText().toString();

            Spinner spinner = (Spinner) findViewById(R.id.spinner_new_tra_type);
            String tra_type = spinner.getSelectedItem().toString();

            edt = (EditText) findViewById(R.id.txt_new_date);
            String tra_date = edt.getText().toString();

            spinner = (Spinner) findViewById(R.id.spinner_new_tra_loc);
            String tra_sub_loc = spinner.getSelectedItem().toString();
            tra_sub_loc = tra_sub_loc.split("-")[0].trim();

            edt = (EditText) findViewById(R.id.txt_new_tra_remarks);
            String tra_remarks = edt.getText().toString();

            if (tra_id.length() == 0 || tra_type.length() == 0 || tra_date.equals("Tap to Choose Date") || tra_date.length() == 0 ||
                    tra_sub_loc.length() == 0 || tra_remarks.length() == 0)
                Toast.makeText(this, "Please fill required fields", LENGTH_SHORT).show();
            else {
                OPERATION_NAME = "insertTransHeader";
                new MyTask().execute(tra_id, tra_type, tra_date, loc, tra_sub_loc, user, tra_remarks, new Ops().getMacAddress());
            }
        } else if (mode.equals("edit")) {
            gotoStockTransScan();
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
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("insertTransHeader")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("id");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("trans_type");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("trans_date");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(params[3]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("sub_loc");
                pi.setType(String.class);
                pi.setValue(params[4]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(params[5]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("remarks");
                pi.setType(String.class);
                pi.setValue(params[6]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mac_address");
                pi.setType(String.class);
                pi.setValue(params[7]);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                        SoapEnvelope.VER11);
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
            if (OPERATION_NAME.equals("getTransPendNo")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mac");
                pi.setType(String.class);
                pi.setValue(new Ops().getMacAddress());
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                        SoapEnvelope.VER11);
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
            if (OPERATION_NAME.equals("getTransNextNo")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                        SoapEnvelope.VER11);
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
            System.out.println(SOAP_ADDRESS + " (" + OPERATION_NAME + ")");
            System.out.println(response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            if (OPERATION_NAME.equals("insertTransHeader")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    gotoStockTransScan();
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("getTransPendNo")) {
                if (result.toUpperCase().contains("No pending trans".toUpperCase())) {
                    mode = "new";
                    OPERATION_NAME = "getTransNextNo";
                    new MyTask().execute();
                } else if (result.toUpperCase().contains("success".toUpperCase())) {
                    mode = "edit";
                    String[] data = result.split(",");
                    EditText edt = (EditText) findViewById(R.id.txt_new_tra_id);
                    edt.setText(data[1]);
                    edt = (EditText) findViewById(R.id.txt_new_date);
                    edt.setEnabled(false);
                    edt.setText(data[4]);
                    edt = (EditText) findViewById(R.id.txt_new_tra_remarks);
                    edt.setEnabled(false);
                    edt.setText(data[5]);
                    String trans_type = data[2];

                    List<String> spinnerArray = new ArrayList<String>();
                    spinnerArray.add(data[3]);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item, spinnerArray);
                    adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    Spinner spinner = (Spinner) findViewById(R.id.spinner_new_tra_loc);
                    spinner.setAdapter(adapter);
                    spinner.setSelection(0);
                    spinner.setEnabled(false);

                    spinnerArray = new ArrayList<String>();
                    spinnerArray.add(trans_type);
                    adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item, spinnerArray);
                    adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    spinner = (Spinner) findViewById(R.id.spinner_new_tra_type);
                    spinner.setAdapter(adapter);
                    spinner.setSelection(0);
                    spinner.setEnabled(false);
                    closeKeyboard();
                } else {
                    Toast.makeText(getApplicationContext(), "Server Error: " + result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("getTransNextNo")) {
                if (result.toUpperCase().contains("Server Error".toUpperCase())) {
                    Toast.makeText(getApplicationContext(), "Server Error: " + result, LENGTH_SHORT).show();
                } else {
                    EditText edt = (EditText) findViewById(R.id.txt_new_tra_id);
                    edt.setText(result);
                    loadLocs();
                }
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
    public void closeKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private class MyTask1 extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;

            SOAP_ACTION = "http://tempuri.org/getTransLoc";
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, "getTransLoc");

            PropertyInfo pi = new PropertyInfo();
            pi.setName("loc");
            pi.setType(String.class);
            pi.setValue(loc);
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

            System.out.println(response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            try {
                ArrayList<String> arrayList = new ArrayList<>();
                System.out.println(result);
                JSONObject json = new JSONObject(result);

                //LOAD LOCATIONS
                JSONObject json_loc = new JSONObject(json.getString("LOC"));
                JSONArray leaders = json_loc.getJSONArray("IMAN_TRANSFER_LOC");
                Integer j = 0;
                for (int i = 0; i <= leaders.length() - 1; i++) {
                    j++;
                    JSONObject jsonas = leaders.getJSONObject(i);
                    String trans_loc = jsonas.getString("LOC_CODE") + " - " + jsonas.getString("LOC_NAME");
                    System.out.println(trans_loc);
                    arrayList.add(trans_loc);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item, arrayList);
                adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                Spinner spinner = (Spinner) findViewById(R.id.spinner_new_tra_loc);
                spinner.setAdapter(adapter);
                spinner.setSelection(0);

                //LOAD TRANS TYPES
                arrayList = new ArrayList<>();
                JSONObject json_trans_type = new JSONObject(json.getString("TYPE"));
                leaders = json_trans_type.getJSONArray("IMAN_TRANSFER_TYPES");
                for (int i = 0; i <= leaders.length() - 1; i++) {
                    j++;
                    JSONObject jsonas = leaders.getJSONObject(i);
                    String trans_type = jsonas.getString("TYPE_NAME");
                    System.out.println(trans_type);
                    arrayList.add(trans_type);
                }
                adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item, arrayList);
                adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                spinner = (Spinner) findViewById(R.id.spinner_new_tra_type);
                spinner.setAdapter(adapter);
                spinner.setSelection(0);
            } catch (JSONException e) {
                System.out.println(e.getMessage());
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void loadLocs() {
        new MyTask1().execute();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
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

    public void newCancelPressed(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
