package com.grand.imanpro;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

public class StockTransferReport extends AppCompatActivity {
    SQLiteDatabase db=null;
    public String user="",loc="";
    public int ok_to_save=0;
    Double stock_val=0.0;
    public String SOAP_ACTION = "";
    public  String OPERATION_NAME = "";
    public String p_stock_unit,inv_date;
    public  final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public  String SOAP_ADDRESS = "";
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_transfer_report);
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

        Intent intent=getIntent();
        EditText edt=(EditText)findViewById(R.id.txt_tra_rep_id);
        edt.setText(intent.getStringExtra("temp_tra_id"));
        edt=(EditText)findViewById(R.id.txt_tra_rep_loc);
        edt.setText(intent.getStringExtra("tra_loc"));
        edt=(EditText)findViewById(R.id.txt_tra_rep_date);
        edt.setText(intent.getStringExtra("tra_date"));

        OPERATION_NAME="getTransReport";
        new MyTask().execute(intent.getStringExtra("tra_id"));
    }
    public void closeKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public void insertData(int slno,String TRD_BARCODE,String TRD_ART_CODE,String TRD_STOCK_SU,String TRD_DESCRIPTION,String TRD_BARCODE_CONV,String TRD_QTY,String TRD_UNIT_COST, String TRD_AMOUNT)
    {
        String query="insert into Transfer values("+slno+",'"+TRD_BARCODE+"',"+TRD_ART_CODE+",'"+TRD_STOCK_SU+"','"+TRD_DESCRIPTION+"',"+TRD_BARCODE_CONV+","+TRD_QTY+","+TRD_UNIT_COST+","+TRD_AMOUNT+")";
        db.execSQL(query);
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
            if (OPERATION_NAME.equals("getTransReport")) {
                SOAP_ACTION = "http://tempuri.org/"+OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                PropertyInfo pi = new PropertyInfo();
                pi.setName("tra_id");
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
                    //System.out.println(response);
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            if(OPERATION_NAME.equals("insertLocalDB"))
            {
                try {
                    String query="";
                    query="delete from Transfer";
                    db.execSQL(query);

                    JSONObject json = new JSONObject(params[0]);
                    JSONArray array_cat = json.getJSONArray("IMAN_TRANSFER_DETL");
                    //System.out.println(params[0]);
                    System.out.println(array_cat.length());
                    int count=0;
                    for (int i = 0; i < array_cat.length(); i++) {
                        count++;
                        JSONObject row_cat = array_cat.getJSONObject(i);
                        String TRD_BARCODE = row_cat.getString("TRD_BARCODE");
                        System.out.println(TRD_BARCODE);
                        String TRD_ART_CODE = row_cat.getString("TRD_ART_CODE");
                        String TRD_STOCK_SU = row_cat.getString("TRD_STOCK_SU");
                        String TRD_DESCRIPTION = row_cat.getString("TRD_DESCRIPTION");
                        String TRD_BARCODE_CONV = row_cat.getString("TRD_BARCODE_CONV");
                        String TRD_QTY = row_cat.getString("TRD_QTY");
                        String TRD_UNIT_COST = row_cat.getString("TRD_UNIT_COST");
                        String TRD_AMOUNT = row_cat.getString("TRD_AMOUNT");
                        System.out.println("--------------------------------");
                        insertData(count, TRD_BARCODE, TRD_ART_CODE,TRD_STOCK_SU,TRD_DESCRIPTION,TRD_BARCODE_CONV,TRD_QTY,TRD_UNIT_COST,TRD_AMOUNT);
                    }
                }catch (Exception ex){
                    response="Local Processing Error: "+ex.getMessage();
                }
                response="success";
            }
            return response.toString();
        }
        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            if(OPERATION_NAME.equals("insertLocalDB"))
            {
                TableLayout tab=(TableLayout) findViewById(R.id.tab);
                TableRow tr = new TableRow(getApplicationContext());
                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));


                TextView textView1=new TextView(getApplicationContext());
                textView1.setTypeface(null, Typeface.BOLD);
                textView1.setTextColor(Color.WHITE);
                textView1.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView1.setText(" Sl No.  ");
                textView1.setGravity(Gravity.CENTER);
                textView1.setPadding(2, 2, 2, 2);
                textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView1);

                TextView textView2=new TextView(getApplicationContext());
                textView2.setTypeface(null, Typeface.BOLD);
                textView2.setText(" Barcode  ");
                textView2.setPadding(2, 2, 2, 2);
                textView2.setTextColor(Color.WHITE);
                textView2.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView2.setGravity(Gravity.CENTER);
                textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView2);


                TextView textView3=new TextView(getApplicationContext());
                textView3.setText(" Gold Code  ");
                textView3.setTypeface(null, Typeface.BOLD);
                textView3.setTextColor(Color.WHITE);
                textView3.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView3.setPadding(2, 2, 2, 2);
                textView3.setGravity(Gravity.CENTER);
                textView3.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView3);

                TextView textView4=new TextView(getApplicationContext());
                textView4.setText(" Stock SU  ");
                textView4.setTypeface(null, Typeface.BOLD);
                textView4.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView4.setTextColor(Color.WHITE);
                textView4.setPadding(2, 2, 2, 2);
                textView4.setGravity(Gravity.CENTER);
                textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView4);



                TextView textView5=new TextView(getApplicationContext());
                textView5.setText(" Item Description  ");
                textView5.setPadding(2, 2, 2, 2);
                textView5.setTypeface(null, Typeface.BOLD);
                textView5.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView5.setTextColor(Color.WHITE);
                textView5.setGravity(Gravity.CENTER);
                textView5.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView5);


                TextView textView6=new TextView(getApplicationContext());
                textView6.setText(" Conv  ");
                textView6.setTextColor(Color.WHITE);
                textView6.setTypeface(null, Typeface.BOLD);
                textView6.setGravity(Gravity.CENTER);
                textView6.setPadding(2, 2, 2, 2);
                textView6.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView6.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView6);

                TextView textView7=new TextView(getApplicationContext());
                textView7.setText(" Qty (Pcs)  ");
                textView7.setTextColor(Color.WHITE);
                textView7.setTypeface(null, Typeface.BOLD);
                textView7.setGravity(Gravity.CENTER);
                textView7.setPadding(2, 2, 2, 2);
                textView7.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView7.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView7);

                TextView textView8=new TextView(getApplicationContext());
                textView8.setText(" Unit Cost  ");
                textView8.setTextColor(Color.WHITE);
                textView8.setTypeface(null, Typeface.BOLD);
                textView8.setGravity(Gravity.CENTER);
                textView8.setPadding(2, 2, 2, 2);
                textView8.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView8.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView8);

                TextView textView9=new TextView(getApplicationContext());
                textView9.setText(" Amount  ");
                textView9.setTextColor(Color.WHITE);
                textView9.setTypeface(null, Typeface.BOLD);
                textView9.setGravity(Gravity.CENTER);
                textView9.setPadding(2, 2, 2, 2);
                textView9.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView9);
                tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

                Cursor cursor = db.rawQuery("select * from Transfer", null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        tr = new TableRow(getApplicationContext());
                        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                        textView1=new TextView(getApplicationContext());
                        textView1.setText(cursor.getString(cursor.getColumnIndex("slno"))+" ");
                        textView1.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        textView1.setPadding(2, 2, 2, 2);
                        textView1.setGravity(Gravity.CENTER);
                        textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView1.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView1);

                        textView2=new TextView(getApplicationContext());
                        textView2.setText(cursor.getString(cursor.getColumnIndex("TRD_BARCODE"))+" ");
                        textView2.setPadding(2, 2, 2, 2);
                        textView2.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView2.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView2);

                        textView3=new TextView(getApplicationContext());
                        textView3.setText(cursor.getString(cursor.getColumnIndex("TRD_ART_CODE"))+"  ");
                        textView3.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView3.setPadding(2, 2, 2, 2);
                        textView3.setGravity(Gravity.CENTER);
                        textView3.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView3.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView3);

                        textView4=new TextView(getApplicationContext());
                        textView4.setText(cursor.getString(cursor.getColumnIndex("TRD_STOCK_SU"))+" ");
                        textView4.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView4.setPadding(2, 2, 2, 2);
                        textView4.setGravity(Gravity.CENTER);
                        textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView4.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView4);

                        textView5=new TextView(getApplicationContext());
                        textView5.setText(cursor.getString(cursor.getColumnIndex("TRD_DESCRIPTION"))+"  ");
                        textView5.setGravity(Gravity.CENTER);
                        textView5.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView5.setPadding(2, 2, 2, 2);
                        textView5.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView5.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView5);

                        textView6=new TextView(getApplicationContext());
                        textView6.setText(cursor.getString(cursor.getColumnIndex("TRD_BARCODE_CONV"))+"  ");
                        textView6.setGravity(Gravity.CENTER);
                        textView6.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView6.setPadding(2, 2, 2, 2);
                        textView6.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView6.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView6);

                        textView7=new TextView(getApplicationContext());
                        textView7.setText(cursor.getString(cursor.getColumnIndex("TRD_QTY"))+"  ");
                        textView7.setGravity(Gravity.CENTER);
                        textView7.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView7.setPadding(2, 2, 2, 2);
                        textView7.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView7.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView7);

                        textView8=new TextView(getApplicationContext());
                        textView8.setText(cursor.getString(cursor.getColumnIndex("TRD_UNIT_COST"))+"  ");
                        textView8.setGravity(Gravity.CENTER);
                        textView8.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView8.setPadding(2, 2, 2, 2);
                        textView8.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView8.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView8);

                        textView9=new TextView(getApplicationContext());
                        textView9.setText(cursor.getString(cursor.getColumnIndex("TRD_AMOUNT"))+"  ");
                        textView9.setGravity(Gravity.CENTER);
                        textView9.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView9.setPadding(2, 2, 2, 2);
                        textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView9.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView9);

                        tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                        cursor.moveToNext();
                    }
                }
            }
            if (OPERATION_NAME.equals("getTransReport")) {
                //System.out.println(result);
                OPERATION_NAME="insertLocalDB";
                new MyTask().execute(result);
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);
            closeKeyboard();
        }
    }
}
