package com.grand.imanpro;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static android.widget.Toast.LENGTH_SHORT;

public class StockTrasnferScan extends AppCompatActivity {
    SQLiteDatabase db=null;
    public String user="",loc="",tra_id="",temp_tra_id="",mail_id="";
    public int ok_to_save=0;
    public String SOAP_ACTION = "";
    public  String OPERATION_NAME = "";
    public String p_stock_unit,inv_date;
    public  final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public  String SOAP_ADDRESS = "",global_barcode="";
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_transfer_scan);

        dialog=new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                loc=cursor.getString(cursor.getColumnIndex("loc"));
                String ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
            }
        }

        user=getUser();
        Intent intent=getIntent();
        temp_tra_id = intent.getStringExtra("temp_tra_id");
        String tra_date =intent.getStringExtra("tra_date");
        String tra_loc =intent.getStringExtra("tra_loc");
        tra_id=intent.getStringExtra("tra_id");

        EditText edt=(EditText)findViewById(R.id.txt_tra_scan_id);
        edt.setText(temp_tra_id);
        edt=(EditText)findViewById(R.id.txt_tra_scan_date);
        edt.setText(tra_date);
        edt=(EditText)findViewById(R.id.txt_tra_scan_loc);
        edt.setText(tra_loc);

        edt=(EditText)findViewById(R.id.txt_tra_scan_bar);
        edt.requestFocus();

        final EditText edittext = (EditText) findViewById(R.id.txt_tra_scan_bar);
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if(!edittext.getText().toString().isEmpty()) {
                        OPERATION_NAME="getTransProdDetl";
                        String barcode=edittext.getText().toString();
                        new MyTask().execute(barcode,tra_id);
                    }
                    return true;
                }
                return false;
            }
        });
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
    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            dialog.show();
            super.onPreExecute();
            /*ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);*/
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            System.out.println(SOAP_ADDRESS+" ("+OPERATION_NAME+")");
            if(OPERATION_NAME.equals("validTransfer"))
            {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("tra_id");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(params[1]);
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
            if(OPERATION_NAME.equals("delTransfer"))
            {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("tra_id");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(params[1]);
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
            if(OPERATION_NAME.equals("sendTransMail"))
            {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("tra_id");
                pi.setType(String.class);
                pi.setValue(tra_id);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mail_id");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("temp_tra_id");
                pi.setType(String.class);
                pi.setValue(temp_tra_id);
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
            if (OPERATION_NAME.equals("insertTransProdDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("tra_id");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("barcode");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("gold_code");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("su");
                pi.setType(String.class);
                pi.setValue(params[3]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("desc");
                pi.setType(String.class);
                pi.setValue(params[4]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("conv");
                pi.setType(String.class);
                pi.setValue(params[5]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("rsp");
                pi.setType(String.class);
                pi.setValue(params[6]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("unit_cost");
                pi.setType(String.class);
                pi.setValue(params[7]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("stock_unit");
                pi.setType(String.class);
                pi.setValue(params[8]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("qty");
                pi.setType(String.class);
                pi.setValue(params[9]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("stock");
                pi.setType(String.class);
                pi.setValue(params[10]);
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
            if(OPERATION_NAME.equals("getTransProdDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("barcode");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);
                global_barcode=params[0];

                pi = new PropertyInfo();
                pi.setName("tra_id");
                pi.setType(String.class);
                pi.setValue(params[1]);
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
            System.out.println(response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            if(OPERATION_NAME.equals("validTransfer"))
            {
                if(result.toUpperCase().contains("SUCCESS"))
                {
                    EditText edt=(EditText)findViewById(R.id.txt_tra_scan_bar);
                    edt.setEnabled(false);
                    edt=(EditText)findViewById(R.id.txt_tra_scan_qty);
                    edt.setEnabled(false);
                    Button btn=(Button)findViewById(R.id.btn_del_trans);
                    btn.setEnabled(false);
                    btn=(Button)findViewById(R.id.btn_save_trans);
                    btn.setEnabled(false);
                    clearAll(null);
                    Toast.makeText(getApplicationContext(),"Validated!",LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
                }
            }
            if(OPERATION_NAME.equals("delTransfer"))
            {
                if(result.toUpperCase().contains("SUCCESS"))
                {
                    Toast.makeText(getApplicationContext(),"Deleted!",LENGTH_SHORT).show();
                    Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                    startActivity(intent);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
                }
            }
            if(OPERATION_NAME.equals("sendTransMail"))
            {
                if(result.toUpperCase().contains("SUCCESS"))
                {
                    db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
                    Cursor cursor = db.rawQuery("select * from RecentMailIds where MAIL_ID='" + mail_id + "'", null);
                    if (cursor.getCount() <= 0) {
                        db.execSQL("INSERT INTO RecentMailIds (MAIL_ID)VALUES('" + mail_id.trim() + "')");
                    }
                    Toast.makeText(getApplicationContext(),"Mail Sent!",LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("insertTransProdDetl")) {
                if(result.toUpperCase().contains("SUCCESS"))
                {
                    Toast.makeText(getApplicationContext(),"Saved!",LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
                }
                clearAll(null);
            }
            if (OPERATION_NAME.equals("getTransProdDetl")) {
                if(result.contains("Error: "))
                {
                    ok_to_save=0;
                    Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
                    clearAll(null);
                }
                else
                {
                    closeKeyboard();
                    String[]array=result.split(",");
                    String p_gold_code=array[0];
                    String p_stock_su=array[1];
                    String p_desc=array[2];
                    String p_rsp=array[3];
                    String p_conv=array[4];
                    String p_stock_qty=array[5];
                    String p_cost=array[6];
                    p_stock_unit=array[7];
                    String p_phy_entered=array[8];
                    if(Double.parseDouble(p_phy_entered)==0)
                        p_phy_entered="";

                    Double d_rsp=Double.parseDouble(p_rsp);
                    EditText edt;
                    if(d_rsp>0&&global_barcode.substring(0,3).equals("270"))
                    {
                        String left_part=global_barcode.substring(7,10);
                        String right_part=global_barcode.substring(10,12);
                        edt=(EditText)findViewById(R.id.txt_tra_scan_qty);
                        if(Double.parseDouble(right_part)!=0&&p_stock_unit.contains("Piece")) {
                            Toast.makeText(getApplicationContext(), "Invalid Price / Barcode", LENGTH_SHORT).show();
                            edt.setText("");
                        }
                        else {
                            Double value=0.0;
                            if(Double.parseDouble(right_part)==0)
                                value=Double.parseDouble(left_part);
                            else
                                value=Double.parseDouble(left_part+"."+right_part);
                            String  s_qty=String.valueOf(value/d_rsp);
                            Double  qty=value/d_rsp;
                            edt.setText(String.valueOf(qty));
                        }
                    }

                    //stock_val=Double.parseDouble(p_stock_val);

                    edt=(EditText)findViewById(R.id.txt_tra_scan_prod);
                    edt.setText(p_gold_code);
                    edt=(EditText)findViewById(R.id.txt_tra_scan_su);
                    edt.setText(p_stock_su);
                    edt=(EditText)findViewById(R.id.txt_tra_scan_desc);
                    edt.setText(p_desc);
                    edt=(EditText)findViewById(R.id.txt_tra_scan_rsp);
                    edt.setText(p_rsp);
                    edt=(EditText)findViewById(R.id.txt_tra_scan_conv);
                    edt.setText(p_conv);
                    edt=(EditText)findViewById(R.id.txt_tra_scan_stock);
                    edt.setText(p_stock_qty);
                    edt=(EditText)findViewById(R.id.txt_tra_scan_lpp);
                    edt.setText(p_cost);

                    edt=(EditText)findViewById(R.id.txt_tra_scan_qty);
                    if(p_stock_unit.toUpperCase().contains("KILO"))
                    {
                        edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        //edt.setText("0.000");
                    }
                    else {
                        edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        //edt.setText("0");
                    }
                    TextView txt=(TextView)findViewById(R.id.txt_exist_qty);
                    if(p_phy_entered.trim().length()>0)
                        txt.setText("Existing Qty Entered: "+p_phy_entered);
                    else
                        txt.setText("Existing Qty Entered: 0");
                    //edt.requestFocus();
                }
            }
            dialog.cancel();
        }
    }
    public void backPressed(View view){
        Intent intent=new Intent(this,StockTransfer.class);
        startActivity(intent);
    }
    public void reportPressed(View view){
        Intent intent=new Intent(this, StockTransferReport.class);
        EditText edt=(EditText)findViewById(R.id.txt_tra_scan_id);
        intent.putExtra("temp_tra_id",edt.getText().toString());
        edt=(EditText)findViewById(R.id.txt_tra_scan_date);
        intent.putExtra("tra_date",edt.getText().toString());
        edt=(EditText)findViewById(R.id.txt_tra_scan_loc);
        intent.putExtra("tra_loc",edt.getText().toString());
        intent.putExtra("tra_id",tra_id);
        startActivity(intent);
    }
    public void addPressed(View view){
        setStock("+");
    }
    public void minusPressed(View view){
        setStock("-");
    }
    public void closeKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public void setStock(String sign){
        Button btn=(Button)findViewById(R.id.btn_plus);
        btn.requestFocus();
        String format="#0.000";
        if(p_stock_unit!=null) {
            if (p_stock_unit.toUpperCase().contains("KILO"))
                format = "#0.000";
            else
                format = "#0";
        }
        NumberFormat formatter = new DecimalFormat(format);
        Double stock=0.0;
        EditText edt=(EditText)findViewById(R.id.txt_tra_scan_qty);
        String phy=edt.getText().toString();
        if(phy.trim().length()<=0)
            stock=0.0;
        else
            stock=Double.parseDouble(phy);
        switch(sign)
        {
            case "+": {
                stock++;
                break;
            }
            case "-": {
                stock--;
                break;
            }
        }
        //if(stock>=0)
            edt.setText(formatter.format(stock));
    }
    public void validatePressed(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Validate Document");
        builder.setMessage("Are You Sure to Validate?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                OPERATION_NAME="validTransfer";
                new MyTask().execute(tra_id,user);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }
    public void deletePressed(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Document");
        builder.setMessage("Are You Sure to Delete?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                OPERATION_NAME="delTransfer";
                new MyTask().execute(tra_id,user);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }
    public void clearAll(View view)
    {
        EditText edt=(EditText)findViewById(R.id.txt_tra_scan_su);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_desc);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_rsp);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_conv);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_lpp);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_stock);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_qty);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_prod);
        edt.setText("");
        edt=(EditText)findViewById(R.id.txt_tra_scan_bar);
        edt.setText("");
        TextView txt=(TextView)findViewById(R.id.txt_exist_qty);
        txt.setText("");
        edt.requestFocus();
        ok_to_save=0;
    }
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
    }
    public void saveData()
    {
        ok_to_save = 1;
        EditText edt = (EditText) findViewById(R.id.txt_tra_scan_bar);
        String barcode = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_prod);
        String gold_code = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_su);
        String su = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_desc);
        String desc = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_conv);
        String conv = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_rsp);
        String rsp = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_lpp);
        String cost = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_stock);
        String stock = edt.getText().toString();

        edt = (EditText) findViewById(R.id.txt_tra_scan_qty);
        String s_qty = edt.getText().toString();

        OPERATION_NAME = "insertTransProdDetl";
        new MyTask().execute(this.tra_id, barcode, gold_code, su, desc, conv, rsp, cost, p_stock_unit, s_qty,stock);
    }
    public void savePressed(View view){
        EditText edt=(EditText)findViewById(R.id.txt_tra_scan_qty);
        String s_qty=edt.getText().toString();
        edt=(EditText)findViewById(R.id.txt_tra_scan_stock);
        String s_ho_stock=edt.getText().toString();
        Double ho_stock=Double.parseDouble(s_ho_stock);
        if(s_qty.trim().length()<=0)
        {
            Toast.makeText(this,"Physical Stock Can't be Zero",LENGTH_SHORT).show();
            ok_to_save=0;
        }
        else
        {
            Double qty=Double.parseDouble(s_qty);
            if(qty==0)
            {
                Toast.makeText(this,"Physical Stock Can't be Zero",LENGTH_SHORT).show();
                ok_to_save=0;
            }
            else
            {
                if(qty>ho_stock)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Warning");
                    builder.setMessage("Transfer Qty is Greater than Stock. Continue?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            saveData();
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
                    builder.show();
                }
                else {
                    saveData();
                }
                //if()
            }
        }
    }
    public void mailPressed(View view) {
        Context context = getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(layoutParams);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10,10,10,10);

        final EditText mailBox = new EditText(context);
        mailBox.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
        mailBox.setHint("Mail ID");
        mailBox.setText("");
        mailBox.setTextColor(Color.parseColor("#000000"));

// Add a TextView here for the "Title" label, as noted in the comments
        final RadioGroup radioGroup = new RadioGroup(this);
        final RadioButton button_other=new RadioButton(this);
        button_other.setText("Other");
        button_other.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //radioGroup.clearCheck();
                mailBox.setText("");
            }
        });
        button_other.setChecked(true);

        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from RecentMailIds order by slno desc limit 10", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                final String mail_id=cursor.getString(cursor.getColumnIndex("MAIL_ID"));
                final RadioButton radioButton=new RadioButton(this);
                radioButton.setText(mail_id);
                radioGroup.addView(radioButton);
                radioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        button_other.setChecked(false);
                        radioButton.setChecked(true);
                        int selectedId = radioGroup.getCheckedRadioButtonId();
                        RadioButton rdButton = (RadioButton) findViewById(selectedId);
                        mailBox.setText(mail_id);
                    }
                });
                cursor.moveToNext();
            }
        }
        radioGroup.addView(button_other);
        layout.addView(radioGroup);
        layout.addView(mailBox);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Mail ID");

        final EditText input = new EditText(this);
        final EditText input1 = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mail_id = mailBox.getText().toString();
                if (mail_id.trim().length() > 0) {
                    EditText inv_no = (EditText) findViewById(R.id.txt_scan_inv_no);
                    OPERATION_NAME = "sendTransMail";
                    new MyTask().execute(mail_id);
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter a valid mail ID", LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
