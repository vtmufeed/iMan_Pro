package com.grand.imanpro;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class LabelScan extends AppCompatActivity {
    Integer prod_count=1,valid_flag=0;
    SQLiteDatabase db = null;
    String ip="",loc="",seq_slno="",lot_no,lot_printer="",label_type="";
    public String SOAP_ACTION = "";
    public  String OPERATION_NAME = "";
    public  final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public  String SOAP_ADDRESS = "";
    public String pending_lot="";
    ProgressDialog dialog;
    public String user="";
    android.support.v7.app.AlertDialog alert_dialog;
    //private AlertDialog progressDialog;
    String barcode="",prod_code="",su="",price="",arabic="",eng="",count="", conv="",stock="",slno="",lot_id="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_scan);

        //Context context=getApplicationContext();
        //progressDialog = new SpotsDialog(LabelScan.this, R.style.Custom);
        dialog=new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip ", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                String ip=ips[0];
                SOAP_ADDRESS="http://"+ip+"/iManWebService/Service.asmx";
                loc = cursor.getString(cursor.getColumnIndex("loc"));
                setLotNo();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "IP Not Found !!!  Please Contact Your System Administrator !!", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
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
        EditText edt_qty=(EditText)findViewById(R.id.txt_lbl_count);
        edt_qty.setImeOptions(EditorInfo.IME_ACTION_DONE);
        edt_qty.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    closeKeyboard();
                    return true;
                }
                return false;
            }
        });
        user=getUser();
    }
    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
    public void addPressed(View view) {
        setStock("+");
    }

    public void minusPressed(View view) {
        setStock("-");
    }
    public void setStock(String sign) {
        Integer count = 0;
        EditText edt = (EditText) findViewById(R.id.txt_lbl_count);
        String phy = edt.getText().toString();
        if (phy.trim().length() <= 0)
            count = 0;
        else
            count = Integer.parseInt(phy);
        switch (sign) {
            case "+": {
                count++;
                break;
            }
            case "-": {
                count--;
                break;
            }
        }
        edt.setText(String.valueOf(count));
    }
        //if(stock>=0)}
    public void viewPressed(View view)
    {
        TextView edt=(TextView)findViewById(R.id.txt_prod_count);
        Intent intent = new Intent(getApplicationContext(), LabelScanView.class);
        intent.putExtra("lot",lot_no);
        intent.putExtra("total_scan",edt.getText());
        startActivity(intent);
    }
    public void setLotNo(){
        Intent intent = getIntent();
        lot_printer = intent.getStringExtra("lot_printer");
        label_type = intent.getStringExtra("label_type");
        OPERATION_NAME="getLastPendLot";
        new LabelScan.MyTask().execute(loc,lot_printer);
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
                OPERATION_NAME="getProdDetl";
                new LabelScan.MyTask().execute(e2.getText().toString());
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "Please turn on your DATA or WIFI", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }
        }
    }
    public void clearAll()
    {
        EditText edt=(EditText)findViewById(R.id.txt_prod_code);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_su);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_lbl_count);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_prod_desc);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_prod_desc_ar);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_price);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_conv);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_stock);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_barcode);
        edt.setText("");
        edt.requestFocus();
    }
    public void ClearPressed(View view)
    {
        clearAll();
    }
    public void DeletePressed(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Lot");
        builder.setMessage("Are You Sure to Delete this Lot?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                OPERATION_NAME="deleteLot";
                new LabelScan.MyTask().execute();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }
    public void printPressed(View view)
    {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Print Lot");
        builder.setMessage("Confirm Print This Lot");
        builder.setPositiveButton("Print Now", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                OPERATION_NAME="printLot";
                new LabelScan.MyTask().execute();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }
    public void savePressed(View view)
    {
        String s_lbl_count;
        Double d_lbl_count;
        Double d_stock=0.0;
        EditText edt;
        System.out.println("Save Pressed");
        edt=(EditText)findViewById(R.id.txt_barcode);
        barcode=edt.getText().toString();
        edt=(EditText)findViewById(R.id.txt_prod_code);
        prod_code=edt.getText().toString();
        edt=(EditText)findViewById(R.id.txt_lbl_count);
        s_lbl_count=edt.getText().toString();
        edt=(EditText)findViewById(R.id.txt_stock);
        try{
            d_stock=Double.parseDouble(edt.getText().toString());
        }catch (Exception ex){}
        if(s_lbl_count.isEmpty()||s_lbl_count.equals(""))
            d_lbl_count=0.0;
        else
            d_lbl_count=Double.parseDouble(s_lbl_count);

        if(d_stock<d_lbl_count)
            Toast.makeText(this,"Stock is less than label count ..!",Toast.LENGTH_SHORT).show();
        else
        {
            if(valid_flag==1 && !barcode.isEmpty()&& !prod_code.isEmpty() &&d_lbl_count>0 && !s_lbl_count.contains(".") ){
                System.out.println("Flag 1");
                lot_id=lot_no;
                edt=(EditText)findViewById(R.id.txt_barcode);
                barcode=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_prod_code);
                prod_code=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_su);
                su=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_price);
                price=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_prod_desc_ar);
                arabic=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_prod_desc);
                eng=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_lbl_count);
                count=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_conv);
                conv=edt.getText().toString();
                edt=(EditText)findViewById(R.id.txt_stock);
                stock=edt.getText().toString();

                OPERATION_NAME="addData";
                new LabelScan.MyTask().execute();
            }
            else
                Toast.makeText(this,"Please check barcode or label count entered..!",Toast.LENGTH_SHORT).show();
        }
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
    private class MyTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //progressDialog.show();
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("downloadClearanceBar")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                System.out.println("Deleting barcode LOT "+lot_no+" Bar:"+params[0]);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("lot_id");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("op_code");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(params[3]);
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
            if (OPERATION_NAME.equals("deleteBarcode")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                System.out.println("Deleting barcode LOT "+lot_no+" Bar:"+params[0]);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("lot_no");
                pi.setType(String.class);
                pi.setValue(lot_no);
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
                    response = exception.toString();
                }
            }
            if (OPERATION_NAME.equals("printLot")) {
                dialog.setMessage("Printing, Please Wait...");
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                System.out.println("Printing LOT "+lot_no);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("lot_no");
                pi.setType(String.class);
                pi.setValue(lot_no);
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
            if (OPERATION_NAME.equals("getLastPendLot")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                System.out.println("Getting Unconfirmed Pending LOT, Loc: "+params[0]+" Printer: "+params[1]);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc_code");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("printer");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mac_address");
                pi.setType(String.class);
                pi.setValue(getMacAddress());
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("lbl_type");
                pi.setType(String.class);
                pi.setValue(label_type);
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
            if (OPERATION_NAME.equals("deleteLot")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                System.out.println("Deleting LOT "+lot_no);
                PropertyInfo pi = new PropertyInfo();
                pi.setName("lot_no");
                pi.setType(String.class);
                pi.setValue(lot_no);
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
            if (OPERATION_NAME.equals("getNextLot")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                System.out.println("In Async Task getNextLot");

                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                PropertyInfo pi = new PropertyInfo();
                pi.setName("lot_loc");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("lot_printer");
                pi.setType(String.class);
                pi.setValue(lot_printer);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("lot_lbl_type");
                pi.setType(String.class);
                pi.setValue(label_type);
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
                System.out.println(SOAP_ADDRESS);

                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                    lot_no=response.toString();
                    System.out.println(lot_no);
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            if (OPERATION_NAME.equals("getProdDetl")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc_code");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("lot_no");
                pi.setType(String.class);
                pi.setValue(lot_no);
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
                    response = exception.toString();
                }
            }
            if(OPERATION_NAME.equals("addData"))
            {
                System.out.println("In Async Task");
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("lot_id");
                pi.setType(String.class);
                pi.setValue(lot_id);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("barcode");
                pi.setType(String.class);
                pi.setValue(barcode);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("prod_code");
                pi.setType(String.class);
                pi.setValue(prod_code);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("su");
                pi.setType(String.class);
                pi.setValue(su);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("price");
                pi.setType(String.class);
                pi.setValue(price);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("arabic");
                pi.setType(String.class);
                pi.setValue(arabic);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("eng");
                pi.setType(String.class);
                pi.setValue(eng);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("count");
                pi.setType(String.class);
                pi.setValue(count);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("conv");
                pi.setType(String.class);
                pi.setValue(conv);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("stock");
                pi.setType(String.class);
                pi.setValue(stock);
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
            System.out.println(OPERATION_NAME+" : "+response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @Override
        protected void onPostExecute(String result) {
            EditText edt;
            if(OPERATION_NAME.equals("downloadClearanceBar")) {
                if(!result.isEmpty()) {
                    if(result.split(",")[0].equals("success")) {
                        Toast.makeText(getApplicationContext(), result.split(",")[1]+" Records Added",Toast.LENGTH_SHORT).show();
                        TextView textView = (TextView) findViewById(R.id.txt_prod_count);
                        textView.setText("Product Count: "+result.split(",")[2]);
                        textView = (TextView) findViewById(R.id.lbl_label_count);
                        textView.setText("Label Count: "+result.split(",")[3]);
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
                    }
                }
            }
            if(OPERATION_NAME.equals("getNextLot")) {
                if(!result.isEmpty()) {
                    System.out.println("New LOT: " +result);
                    TextView textView=(TextView)findViewById(R.id.txt_lot);
                    textView.setText("New Lot: "+result);
                    lot_no=result;
                    edt=(EditText)findViewById(R.id.txt_barcode);
                    edt.setEnabled(true);
                }
                else
                {
                    edt=(EditText)findViewById(R.id.txt_barcode);
                    edt.setEnabled(false);
                }
            }
            if (OPERATION_NAME.equals("deleteBarcode")) {
                if(result.contains("Success"))
                {
                    Toast.makeText(getApplicationContext(),"Barcode Deleted.!",Toast.LENGTH_SHORT).show();
                    String[] s=result.split(",");
                    TextView textView=(TextView)findViewById(R.id.txt_prod_count);
                    textView.setText("Product Count: "+s[1]);
                    textView = (TextView) findViewById(R.id.lbl_label_count);
                    textView.setText("Label Count: "+result.split(",")[2]);
                    clearAll();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Server Error: "+result,Toast.LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("printLot")) {
                if(result.equals("Success"))
                {
                    Toast.makeText(getApplicationContext(),"Print Request Sent.!",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), LabelSelectPrinter.class);
                    startActivity(intent);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Server Error: "+result,Toast.LENGTH_LONG).show();
                }
            }
            if(OPERATION_NAME.equals("getLastPendLot"))
            {
                if(result.contains("Success"))
                {
                    pending_lot=result;
                    System.out.println("Pending LOT: " +pending_lot);
                    String[] s=result.split(",");
                    lot_no=pending_lot=s[1];
                    String[] r=result.split(",");
                    TextView textView=(TextView)findViewById(R.id.txt_lot);
                    textView.setText("Pending Lot: "+r[1]);
                    textView=(TextView)findViewById(R.id.txt_prod_count);
                    textView.setText("Product Count: "+s[2]);
                    edt=(EditText)findViewById(R.id.txt_barcode);
                    edt.setEnabled(true);
                    textView = (TextView) findViewById(R.id.lbl_label_count);
                    textView.setText("Label Count: "+result.split(",")[3]);
                }
                else if(result.contains("No Pending LOT"))
                {
                    OPERATION_NAME="getNextLot";
                    new LabelScan.MyTask().execute();
                }
                else
                {
                    edt=(EditText)findViewById(R.id.txt_barcode);
                    edt.setEnabled(false);
                    Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("deleteLot")) {
                if(result.equals("Success"))
                {
                    Toast.makeText(getApplicationContext(),"Lot Deleted",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), LabelSelectPrinter.class);
                    startActivity(intent);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Server Error: "+result,Toast.LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("getProdDetl")) {
                if(result.equals("Product Not Found"))
                {
                    Toast.makeText(getApplicationContext(),"Product Not Found",Toast.LENGTH_SHORT).show();
                    edt=(EditText)findViewById(R.id.txt_lbl_count);
                    edt.setEnabled(false);
                    edt=(EditText)findViewById(R.id.txt_barcode);
                    edt.requestFocus();
                    edt.setText("");
                    valid_flag=0;
                }
                else
                {
                    valid_flag=1;
                    String[] data=result.split(",");
                    String prod_code=data[0];
                    String su=data[1];
                    String prod_desc=data[2];
                    String prod_desc_ar=data[3];
                    String price=data[4];
                    String conv=data[5];
                    String stock=data[6];
                    if(result.contains("Exist in table"))
                    {
                        EditText editText=(EditText)findViewById(R.id.txt_lbl_count);
                        editText.setText(data[7]);
                    }
                    edt=(EditText)findViewById(R.id.txt_prod_code);
                    edt.setText(prod_code);
                    edt=(EditText)findViewById(R.id.txt_su);
                    edt.setText(su);
                    edt=(EditText)findViewById(R.id.txt_prod_desc);
                    edt.setText(prod_desc);
                    edt=(EditText)findViewById(R.id.txt_prod_desc_ar);
                    edt.setText(prod_desc_ar);
                    edt=(EditText)findViewById(R.id.txt_price);
                    edt.setText(price);
                    edt=(EditText)findViewById(R.id.txt_conv);
                    edt.setText(conv);
                    edt=(EditText)findViewById(R.id.txt_stock);
                    edt.setText(stock);
                    edt=(EditText)findViewById(R.id.txt_lbl_count);
                    if(Double.parseDouble(stock)>0)
                        edt.setEnabled(true);
                    else
                        edt.setEnabled(false);
                    edt.requestFocus();
                    /*InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY,0);*/
                }
                closeKeyboard();
            }
            if (OPERATION_NAME.equals("addData")) {
                if(result.contains("Success")) {
                    clearAll();
                    Toast.makeText(getApplicationContext(),"Saved!",Toast.LENGTH_SHORT).show();
                    String s[]=result.split(",");
                    TextView textView=(TextView)findViewById(R.id.txt_prod_count);
                    textView.setText("Product Count: "+s[1]);
                    textView = (TextView) findViewById(R.id.lbl_label_count);
                    textView.setText("Label Count: "+result.split(",")[2]);
                }
                else {
                    Toast.makeText(getApplicationContext(),"Server Error; "+ result, Toast.LENGTH_SHORT).show();
                    prod_count--;
                }
            }
            dialog.cancel();
            dialog.setMessage("Loading...");
            //progressDialog.dismiss();
        }
    }
    public void showClearanceDialog(View view)
    {
        Context context = getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

// Add a TextView here for the "Title" label, as noted in the comments
        final EditText edt_op_code = new EditText(context);
        edt_op_code.setHint("Operation Code");
        edt_op_code.setText("");
        edt_op_code.setTextColor(Color.parseColor("#000000"));
        edt_op_code.setInputType(InputType.TYPE_CLASS_TEXT);
        edt_op_code.setMaxLines(1);
        layout.addView(edt_op_code); // Notice this is an add method

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("Download Clearance Barcodes");
        builder.setView(layout);

        builder.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                String op_code=edt_op_code.getText().toString();
                edt_op_code.setText("");
                OPERATION_NAME = "downloadClearanceBar";
                new LabelScan.MyTask().execute(lot_no,op_code,loc,user);
            }
        });

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                edt_op_code.clearFocus();
                dialog.cancel();
            }
        });
        alert_dialog = builder.show();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    public void deleteBarcode()
    {
        EditText edt=(EditText)findViewById(R.id.txt_barcode);
        String barcode=edt.getText().toString();
        OPERATION_NAME="deleteBarcode";
        new LabelScan.MyTask().execute(barcode);
    }
    public void DeleteBarPressed(View view)
    {
        EditText edt=(EditText)findViewById(R.id.txt_barcode);
        String barcode=edt.getText().toString();
        if(barcode.equals(""))
            Toast.makeText(this,"Please enter barcode",Toast.LENGTH_SHORT).show();
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Exit");
            builder.setMessage("Unsaved changes will be lost! Continue?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    deleteBarcode();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit");
        builder.setMessage("Unsaved changes will be lost! Continue?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(getApplicationContext(), LabelSelectPrinter.class);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
