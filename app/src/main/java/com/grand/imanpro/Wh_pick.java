package com.grand.imanpro;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

public class Wh_pick extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {
    SQLiteDatabase db = null;
    public String user = "", loc = "", mode = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    String date_picker_source="";
    ProgressDialog dialog;
    LinearLayout layout_order_data;
    Button btn_next;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wh_pick);

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

        btn_next=(Button)findViewById(R.id.btn_next);
        layout_order_data=(LinearLayout)findViewById(R.id.layout_order_data);
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        Spinner spinner_sites=(Spinner)findViewById(R.id.spinner_order_site);
        spinner_sites.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                layout_order_data.setVisibility(View.INVISIBLE);
                Spinner spinner1=(Spinner)findViewById(R.id.spinner_orders);
                spinner1.setAdapter(null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        Spinner spinner_orders=(Spinner)findViewById(R.id.spinner_orders);
        spinner_orders.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Spinner spinner1=(Spinner)findViewById(R.id.spinner_orders);
                if (spinner1.getSelectedItem().toString().length() == 0 || spinner1.getSelectedItem().toString().contains("Choose Order")) {
                    layout_order_data.setVisibility(View.INVISIBLE);
                    //Toast.makeText(getApplicationContext(), "Please Select an Order", LENGTH_SHORT).show();
                } else {
                    btn_next.setEnabled(false);
                    OPERATION_NAME = "getOrderDetl";
                    new MyTask().execute(spinner1.getSelectedItem().toString());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });

        OPERATION_NAME = "getPickSites";
        new MyTask1().execute();
    }
    public void showDateDialogFrm(View view)
    {
        date_picker_source="FROM";
        showDatePicker();
    }
    public void showDateDialogTo(View view)
    {
        date_picker_source="TO";
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
    public void loadOrderNos(View view)
    {
        Spinner spinner = (Spinner)findViewById(R.id.spinner_order_site);
        String order_site = spinner.getSelectedItem().toString().split("-")[0].trim();

        EditText edt=(EditText)findViewById(R.id.txt_date_from);
        String from_date=edt.getText().toString();
        edt=(EditText)findViewById(R.id.txt_date_to);
        String to_date=edt.getText().toString();

        layout_order_data.setVisibility(View.INVISIBLE);

        System.out.println(order_site+","+loc+","+from_date+","+to_date+","+user);
        OPERATION_NAME="getPickOrders";
        new MyTask().execute(order_site,loc,from_date,to_date,user);
    }
    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        EditText edt_from = (EditText) findViewById(R.id.txt_date_from);
        EditText edt_to = (EditText) findViewById(R.id.txt_date_to);
        String mon = "";
        mon = new Ops().getMonthString(month);
        if(date_picker_source.equals("FROM"))
            edt_from.setText(dayOfMonth + "-" + mon + "-" + year);
        else
            edt_to.setText(dayOfMonth + "-" + mon + "-" + year);
        layout_order_data.setVisibility(View.INVISIBLE);
        Spinner spinner=(Spinner)findViewById(R.id.spinner_orders);
        spinner.setAdapter(null);
    }

    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (OPERATION_NAME.equals("getOrderItems")) {
                dialog.show();
            }
            else {
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("getOrderDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("order_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
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
            if (OPERATION_NAME.equals("getPickOrders")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("order_site");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("wh_site");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("order_from");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("order_to");
                pi.setType(String.class);
                pi.setValue(params[3]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(params[4]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mac_address");
                pi.setType(String.class);
                pi.setValue(getMacAddress());
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
            if (OPERATION_NAME.equals("getOrderItems")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("order_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(user);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mac_address");
                pi.setType(String.class);
                pi.setValue(getMacAddress());
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                        SoapEnvelope.VER11);
                envelope.dotNet = true;

                envelope.setOutputSoapObject(request);

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS,180000);

                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                } catch (Exception exception) {
                    response = exception.toString();
                }
                String data=response.toString();
                if(data.toUpperCase().contains("\"success\"".toUpperCase()))
                {
                    try {
                        System.out.println("suc");
                        final JSONObject json = new JSONObject(data);
                        JSONObject json_ord_detl = new JSONObject(json.getString("ORD_DTL"));
                        db.execSQL("delete from Wh_Order_Detl");
                        db.execSQL("delete from sqlite_sequence where name='Wh_Order_Detl';");
                        JSONArray array_cat = json_ord_detl.getJSONArray("IMAN_WH_PICK_DETL");
                        System.out.println(array_cat);
                        for (int i = 0; i < array_cat.length(); i++) {
                            JSONObject row = array_cat.getJSONObject(i);
                            String order_no = row.getString("ODT_ORD_NO");
                            String order_date = row.getString("ODT_ORDER_DATE");
                            String barcode = row.getString("ODT_BARCODE");
                            String gold_code = row.getString("ODT_GOLD_CODE");
                            String lv = row.getString("ODT_LV");
                            String ord_unit = row.getString("ODT_ORD_UNIT");
                            String desc = row.getString("ODT_DESCRIPTION");
                            String order_qty = row.getString("ODT_ORDER_QTY");
                            String prep_qty = row.getString("ODT_PREP_QTY");
                            String pick_qty = row.getString("ODT_PICK_QTY");
                            String stock_unit = row.getString("ODT_STOCK_UNIT");
                            String conv = row.getString("ODT_SELLING_COEF");
                            String order_conv = row.getString("ODT_ORDER_CONV");
                            String soh = row.getString("ODT_SOH");
                            String qty_in_order = row.getString("ODT_QTY_IN_ORD");
                            String po_line = row.getString("PO_LINE");
                            System.out.println(barcode);
                            if(pick_qty=="null")
                                db.execSQL("INSERT INTO Wh_Order_Detl (ORDER_NO,ORDER_DATE,BARCODE,GOLD_CODE,LV,ORD_UNIT,PROD_DESC,ORDER_QTY,PREP_QTY,PICK_QTY,STOCK_UNIT,CONV,ORDER_PACK,SOH,PO_LINE,QTY_IN_ORDER) VALUES('"+order_no+"','"+order_date+"','"+barcode+"','"+gold_code+"','"+lv+"','"+ord_unit+"','"+desc+"','"+order_qty+"','"+prep_qty+"',null,'"+stock_unit+"','"+conv+"','"+order_conv+"','"+soh +"','"+po_line+"','"+qty_in_order+"')");
                            else
                                db.execSQL("INSERT INTO Wh_Order_Detl (ORDER_NO,ORDER_DATE,BARCODE,GOLD_CODE,LV,ORD_UNIT,PROD_DESC,ORDER_QTY,PREP_QTY,PICK_QTY,STOCK_UNIT,CONV,ORDER_PACK,SOH,PO_LINE,QTY_IN_ORDER) VALUES('"+order_no+"','"+order_date+"','"+barcode+"','"+gold_code+"','"+lv+"','"+ord_unit+"','"+desc+"','"+order_qty+"','"+prep_qty+"','"+pick_qty+"','"+stock_unit+"','"+conv+"','"+order_conv+"','"+soh +"','"+po_line+"','"+qty_in_order+"')");
                        }

                        JSONObject json_bar_detl = new JSONObject(json.getString("BAR_DTL"));
                        db.execSQL("delete from Wh_Order_Bar_Detl;");
                        array_cat = json_bar_detl.getJSONArray("GRAND_ACTIVE_BARCODE");
                        System.out.println(array_cat.length());
                        for (int i = 0; i < array_cat.length(); i++) {
                            JSONObject row = array_cat.getJSONObject(i);
                            String gold_code = row.getString("GOLD_CODE");
                            String su = row.getString("SU");
                            String barcode = row.getString("BARCODE");
                            System.out.println("inserting barcode "+barcode);
                            db.execSQL("INSERT INTO Wh_Order_Bar_Detl (GOLD_CODE,SU,BARCODE) VALUES('"+gold_code+"','"+su+"','"+barcode+"')");
                        }
                        response="success,"+json.getString("ORD_DT");
                    } catch (Exception ex) {
                        response = "Local Processing Error: " + ex.getMessage();
                    }
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
            if (OPERATION_NAME.equals("getOrderDetl")) {
                if(result.contains("success")) {
                    layout_order_data.setVisibility(View.VISIBLE);
                    try {
                        JSONObject json = new JSONObject(result);
                        String wh_code=json.getString("wh_code");
                        String wh_name=json.getString("wh_name");
                        String order_date=json.getString("ord_date");
                        String deliv_date=json.getString("deliv_date");
                        String cre_date=json.getString("cre_date");
                        String items=json.getString("items");
                        Spinner spinner=(Spinner)findViewById(R.id.spinner_order_site);
                        String site=spinner.getSelectedItem().toString();

                        TextView textView=(TextView)findViewById(R.id.txt_data_wh);
                        textView.setText(wh_code+" - "+wh_name);
                        textView=(TextView)findViewById(R.id.txt_data_site);
                        textView.setText(site);
                        textView=(TextView)findViewById(R.id.txt_data_ord_date);
                        textView.setText(order_date);
                        textView=(TextView)findViewById(R.id.txt_data_deliv_date);
                        textView.setText(deliv_date);
                        textView=(TextView)findViewById(R.id.txt_data_cre_date);
                        textView.setText(cre_date);
                        textView=(TextView)findViewById(R.id.txt_data_items);
                        textView.setText(items);
                    } catch (JSONException e) {
                        layout_order_data.setVisibility(View.INVISIBLE);
                        Toast.makeText(getApplicationContext(), e.getMessage(), LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    btn_next.setEnabled(true);
                }
                else
                {
                    layout_order_data.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("getOrderItems")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    Intent intent=new Intent(getApplicationContext(),Wh_pick_items.class);
                    Spinner spinner=(Spinner)findViewById(R.id.spinner_orders);
                    intent.putExtra("order_no",spinner.getSelectedItem().toString());
                    spinner=(Spinner)findViewById(R.id.spinner_order_site);
                    intent.putExtra("order_site",spinner.getSelectedItem().toString().split("-")[0].trim());
                    intent.putExtra("deliv_date",result.split(",")[1]);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("getPickOrders")) {
                if (result.toUpperCase().contains("success,".toUpperCase())) {
                    result=result.replace("success,","");
                    JSONObject json = null;
                    try {
                        ArrayList<String> arrayList = new ArrayList<>();
                        arrayList.add("  --Choose Order--");
                        json = new JSONObject(result);
                    //LOAD LOCATIONS
                        JSONArray leaders = json.getJSONArray("Table1");
                        Integer j = 0;
                        for (int i = 0; i <= leaders.length() - 1; i++) {
                            j++;
                            JSONObject jsonas = leaders.getJSONObject(i);
                            String trans_loc = jsonas.getString("ORD_NUM");
                            System.out.println(trans_loc);
                            arrayList.add(trans_loc);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item, arrayList);
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                        Spinner spinner = (Spinner) findViewById(R.id.spinner_orders);
                        spinner.setAdapter(adapter);
                        spinner.setSelection(0);
                        closeKeyboard();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Server Error: " + result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("getOrderItems")) {
                dialog.dismiss();
            }
            else {
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
    public void NextPressed(View view)
    {
        Spinner spinner=(Spinner)findViewById(R.id.spinner_orders);
        if(spinner.getSelectedItem()!=null) {
            if (spinner.getSelectedItem().toString().length() == 0 || spinner.getSelectedItem().toString().contains("Choose Order")) {
                Toast.makeText(getApplicationContext(), "Please Select an Order", LENGTH_SHORT).show();
            } else {
                OPERATION_NAME = "getOrderItems";
                new MyTask().execute(spinner.getSelectedItem().toString());
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Please Search for Order", LENGTH_SHORT).show();
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

            SOAP_ACTION = "http://tempuri.org/getPickSites";
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, "getPickSites");

            PropertyInfo pi = new PropertyInfo();
            pi.setName("wh_loc");
            pi.setType(String.class);
            pi.setValue(loc);
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("user");
            pi.setType(String.class);
            pi.setValue(user);
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
                String today=json.getString("today");
                System.out.println("Today : "+today);
                EditText edt_from_dt=(EditText)findViewById(R.id.txt_date_from);
                EditText edt_to_dt=(EditText)findViewById(R.id.txt_date_to);
                edt_from_dt.setText(today);
                edt_to_dt.setText(today);

                String sites=json.getString("sites");
                JSONObject json_sites = new JSONObject(sites);
                //LOAD LOCATIONS
                JSONArray leaders = json_sites.getJSONArray("GOLD_LOC_LNK");
                Integer j = 0;
                for (int i = 0; i <= leaders.length() - 1; i++) {
                    j++;
                    JSONObject jsonas = leaders.getJSONObject(i);
                    String trans_loc = jsonas.getString("GLL_GL_LOC_CODE") + " - " + jsonas.getString("GLL_SITE_NAME");
                    System.out.println(trans_loc);
                    arrayList.add(trans_loc);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item, arrayList);
                adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                Spinner spinner = (Spinner) findViewById(R.id.spinner_order_site);
                spinner.setAdapter(adapter);
                spinner.setSelection(0);

            } catch (JSONException e) {
                System.out.println(e.getMessage());
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    public String getMacAddress(){
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

    public void newCancelPressed(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
