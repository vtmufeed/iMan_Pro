package com.grand.imanpro;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
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


public class LabelScanView extends AppCompatActivity {
    SQLiteDatabase db = null;
    int p=1;
    public int cnt=0;
    public int cnt2=20;
    public String SOAP_ACTION = "";

    public  String OPERATION_NAME = "";

    public  final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";

    public  String SOAP_ADDRESS = "";
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_scan_view);
        Intent intent=getIntent();
        String lot=intent.getStringExtra("lot");
        TextView textView=(TextView ) findViewById(R.id.total);
        textView.setText(intent.getStringExtra("total_scan"));
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

        //db = openOrCreateDatabase("barcode", MODE_PRIVATE, null);

        reportView(lot);
        /*EditText editText1=(EditText) findViewById(R.id.editText);
        editText1.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                boolean handled = false;
                {
                    EditText editText=(EditText) findViewById(R.id.editText);
                    TableLayout tab=(TableLayout) findViewById(R.id.tab);
                    tab.removeAllViews();
                    if(!editText.getEditableText().toString().equals("")){
                        cnt=0;
                        cnt2=20;
                        reportView(editText.getEditableText().toString());
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Pls enter Barcode", Toast.LENGTH_LONG).show();
                    }



                }
                return handled;
            }
        });
        TextView editText=(TextView) findViewById(R.id.total);

        /*Cursor cursor = db.rawQuery("select count(*) from bar_count  ", null);
        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            editText.setText(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();*/
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
    public void loadPrinter()
    {
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if(cursor.getCount()>0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                //loc=cursor.getString(cursor.getColumnIndex("loc"));
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                String ip=ips[0];
                SOAP_ADDRESS="http://"+ip+"/iManWebService/Service.asmx";
                cursor.moveToNext();
            }
            cursor.close();
        }
        /*if(!loc.isEmpty())
        {
            OPERATION_NAME="getPrintersLoc";
            new MyTask().execute(loc);
        }*/
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void reportView(String lot){
        Cursor cursor=null;
        /*if(barcode.equals(" "))
        {
            cursor = db.rawQuery("select * from bar_count  order by  date desc limit "+cnt+","+cnt2+"", null);
        }
        else
        {
            cursor = db.rawQuery("select * from bar_count  where bar_code='"+barcode+"' order by  date desc limit "+cnt+","+cnt2+"", null);
        }*/


        if (0== 0) {
            //cursor.moveToFirst();
            OPERATION_NAME="getAppView";
            new MyTask().execute(lot);
        }
        //cursor.close();

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
            if (OPERATION_NAME.equals("getAppView")) {
                SOAP_ACTION = "http://tempuri.org/getAppView";
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                PropertyInfo pi = new PropertyInfo();
                pi.setName("lot_id");
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
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            if (OPERATION_NAME.equals("getAppView")) {
                TableLayout tab=(TableLayout) findViewById(R.id.tab);
                TableRow tr = new TableRow(getApplicationContext());
                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));


                TextView textView1=new TextView(getApplicationContext());
                textView1.setTypeface(null, Typeface.BOLD);
                textView1.setTextColor(Color.WHITE);
                textView1.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView1.setText(" Barcode  ");
                textView1.setGravity(Gravity.CENTER);
                textView1.setPadding(2, 2, 2, 2);
                textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView1);

                TextView textView2=new TextView(getApplicationContext());
                textView2.setTypeface(null, Typeface.BOLD);
                textView2.setText(" Product Code  ");
                textView2.setPadding(2, 2, 2, 2);
                textView2.setTextColor(Color.WHITE);
                textView2.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView2.setGravity(Gravity.CENTER);
                textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView2);


                TextView textView4=new TextView(getApplicationContext());
                textView4.setText(" SU  ");
                textView4.setTypeface(null, Typeface.BOLD);
                textView4.setTextColor(Color.WHITE);
                textView4.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView4.setPadding(2, 2, 2, 2);
                textView4.setGravity(Gravity.CENTER);
                textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView4);

                TextView textView5=new TextView(getApplicationContext());
                textView5.setTypeface(null, Typeface.BOLD);
                textView5.setPadding(2, 2, 2, 2);
                textView5.setText(" UOM  ");
                textView5.setTextColor(Color.WHITE);
                textView5.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView5.setGravity(Gravity.CENTER);
                textView5.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView5);


                TextView textView7=new TextView(getApplicationContext());
                textView7.setText(" Item Description  ");
                textView7.setTypeface(null, Typeface.BOLD);
                textView7.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView7.setTextColor(Color.WHITE);
                textView7.setPadding(2, 2, 2, 2);
                textView7.setGravity(Gravity.CENTER);
                textView7.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView7);



                TextView textView9=new TextView(getApplicationContext());
                textView9.setText(" Price  ");
                textView9.setPadding(2, 2, 2, 2);
                textView9.setTypeface(null, Typeface.BOLD);
                textView9.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView9.setTextColor(Color.WHITE);
                textView9.setGravity(Gravity.CENTER);
                textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView9);


                TextView textView10=new TextView(getApplicationContext());
                textView10.setText(" Label Count  ");
                textView10.setTextColor(Color.WHITE);
                textView10.setTypeface(null, Typeface.BOLD);
                textView10.setGravity(Gravity.CENTER);
                textView10.setPadding(2, 2, 2, 2);
                textView10.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView10);
                tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                System.out.println(result);
                try {
                    JSONObject json = new JSONObject(result);
                    JSONArray leaders = json.getJSONArray("LABEL_PRINT_DETL");
                    Integer j = 0;
                    for (int i = 0; i < leaders.length(); i++) {
                        j++;
                        JSONObject jsonrow = leaders.getJSONObject(i);
                        System.out.println(jsonrow.getString("LOTD_BARCODE"));
                        //String fname = jsonrow.getString("LBL_PRINTER_ID") + " - " + jsonrow.getString("LBL_PRINTER_NAME");

                        tr = new TableRow(getApplicationContext());
                        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));


                        textView1=new TextView(getApplicationContext());
                        textView1.setText(jsonrow.getString("LOTD_BARCODE")+" ");
                        textView1.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        textView1.setPadding(2, 2, 2, 2);
                        textView1.setGravity(Gravity.CENTER);
                        textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView1.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView1);

                        textView2=new TextView(getApplicationContext());
                        textView2.setText(jsonrow.getString("LOTD_PROD_CODE")+" ");
                        textView2.setPadding(2, 2, 2, 2);
                        textView2.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView2.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView2);

                        textView4=new TextView(getApplicationContext());
                        textView4.setText(jsonrow.getString("LOTD_PROD_SU")+"  ");
                        textView4.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView4.setPadding(2, 2, 2, 2);
                        textView4.setGravity(Gravity.CENTER);
                        textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView4.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView4);

                        textView5=new TextView(getApplicationContext());
                        textView5.setText(jsonrow.getString("LOTD_UOM")+" ");
                        textView5.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView5.setGravity(Gravity.CENTER);
                        textView5.setPadding(2, 2, 2, 2);
                        textView5.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView5.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView5);

                        textView9=new TextView(getApplicationContext());
                        textView9.setText(jsonrow.getString("LOTD_ENG")+" ");
                        textView9.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView9.setPadding(2, 2, 2, 2);
                        textView9.setGravity(Gravity.CENTER);
                        textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView9.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView9);

                        textView10=new TextView(getApplicationContext());
                        textView10.setText(jsonrow.getString("LOTD_PRICE")+"  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);

                        textView10=new TextView(getApplicationContext());
                        textView10.setText(jsonrow.getString("LOTD_COUNT")+"  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);
                        tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                    }
                } catch (JSONException e) {
                    System.out.println(e.getMessage());
                }
                /*for (int i = 0; i < cursor.getCount(); i++) {


                    cursor.moveToNext();
                }*/
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);
            /*ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
            progressBar.setVisibility(View.INVISIBLE);*/
        }
    }
}
