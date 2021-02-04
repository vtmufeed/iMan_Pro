package com.grand.imanpro;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class Wh_rcv_report extends AppCompatActivity {
    SQLiteDatabase db = null;
    public String user = "", loc = "";
    public int ok_to_save = 0;
    Double stock_val = 0.0;
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public String p_stock_unit, inv_date;
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    ProgressDialog dialog;
    TextView tv_current_page;
    int offset = 0, rowcount = 0, current_page = 1;
    Double line_per_page = 0.0;
    String mode;
    AlertDialog alert_dialog;
    Button btn_search;
    String global_po_no;
    //EditText edt_barcode;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wh_rcv_report);
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
                String ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
            }
        }

        Intent intent = getIntent();
        line_per_page = 25.0;

        closeKeyboard();
        global_po_no=intent.getStringExtra("po_no");
        OPERATION_NAME = "getWhRecReport";
        new MyTask().execute(global_po_no,"download");
    }

    public void searchBarcodePressed(View view) {
        Context context = getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

// Add a TextView here for the "Title" label, as noted in the comments
        final EditText barcode = new EditText(context);
        barcode.setHint("Scan Barcode");
        barcode.setText("");
        barcode.setTextColor(Color.parseColor("#000000"));
        barcode.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
        layout.addView(barcode); // Notice this is an add method

        barcode.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String bar = barcode.getText().toString();
                    alert_dialog.dismiss();
                    OPERATION_NAME = "getOffInvReport";
                    new MyTask().execute(bar);
                    /*if (!barcode.getText().toString().isEmpty()) {
                        if (mode.equals("offline")) {
                            OPERATION_NAME = "getOffInvReport";
                            String bar = barcode.getText().toString();
                            new MyTask().execute(bar);
                        }
                    }*/
                    //return true;
                }
                return false;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Barcode");

        // Set up the input
        final EditText input = new EditText(this);
        final EditText input1 = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(layout);

        builder.setPositiveButton("Clear Search", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                OPERATION_NAME = "getOffInvReport";
                new MyTask().execute("");
            }
        });

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                OPERATION_NAME = "getOffInvReport";
                new MyTask().execute("");
            }
        });
        alert_dialog = builder.show();
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void insertData(int slno, String barcode, String prod_code, String su, String p_desc, String sys_stk, String phy_stk, String var_qty, String var_val) {
        String query = "insert into Inventory values(" + slno + ",'"+barcode+"'," + prod_code + "," + su + ",'" + p_desc + "'," + sys_stk + "," + phy_stk + "," + var_qty + "," + var_val + ")";
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
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("getWhRecReport")) {
                if(params[1].equals("download")) {
                    SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                    SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                    PropertyInfo pi = new PropertyInfo();
                    pi.setName("po_no");
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
                    System.out.println(response);
                    try {
                        String query = "";
                        db.execSQL("delete from Wh_Reception_Report");
                        db.execSQL("delete from sqlite_sequence where name='Wh_Reception_Report'");

                        JSONObject json = new JSONObject(response.toString());
                        JSONArray array_cat = json.getJSONArray("WH_RECEPTION_DETL");
                        //System.out.println(params[0]);
                        System.out.println(array_cat.length());
                        rowcount = 0;
                        for (int i = 0; i < array_cat.length(); i++) {
                            rowcount++;
                            JSONObject row_cat = array_cat.getJSONObject(i);
                            String WRD_BARCODE = row_cat.getString("WRD_BARCODE");
                            String WRD_GOLD_CODE = row_cat.getString("WRD_GOLD_CODE");
                            String WRD_SU = row_cat.getString("WRD_SU");
                            String WRD_PRICE_UNIT = row_cat.getString("WRD_PRICE_UNIT");
                            String WRD_DESCRIPTION = row_cat.getString("WRD_DESCRIPTION");
                            String WRD_ORDER_QTY = row_cat.getString("WRD_ORDER_QTY");
                            String WRD_SHIPPED_QTY = row_cat.getString("WRD_SHIPPED_QTY");
                            String WRD_RECEIVED_QTY = row_cat.getString("WRD_RECEIVED_QTY");
                            db.execSQL("insert into Wh_Reception_Report (WRD_BARCODE,WRD_GOLD_CODE,WRD_SU,WRD_PRICE_UNIT,WRD_DESCRIPTION,WRD_ORDER_QTY,WRD_SHIPPED_QTY,WRD_RECEIVED_QTY)values('" + WRD_BARCODE + "','" + WRD_GOLD_CODE + "','" + WRD_SU + "','" + WRD_PRICE_UNIT + "','" + WRD_DESCRIPTION + "','" + WRD_ORDER_QTY + "','" + WRD_SHIPPED_QTY + "','" + WRD_RECEIVED_QTY + "')");
                        }
                    } catch (Exception ex) {
                        response = "Local Processing Error: " + ex.getMessage();
                    }
                }
            }
            return params[1];
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            Cursor c1 = db.rawQuery("SELECT count(*) FROM Wh_Reception_Report", null);
            c1.moveToFirst();
            rowcount = Integer.parseInt(c1.getString(0));
            TextView textView = (TextView) findViewById(R.id.txt_report_count);
            textView.setText("Count\n" + String.valueOf(rowcount));
            textView = (TextView) findViewById(R.id.txt_total_pages);
            String total_pages = String.valueOf((int) Math.ceil(rowcount / line_per_page));
            System.out.println(String.valueOf((int) Math.ceil(rowcount / line_per_page)));
            textView.setText(total_pages);

            tv_current_page = (TextView) findViewById(R.id.txt_current_page);
            tv_current_page.setText("1");
            if (OPERATION_NAME.equals("getWhRecReport")) {
                TableLayout tab = (TableLayout) findViewById(R.id.tab);
                tab.removeAllViews();
                TableRow tr = new TableRow(getApplicationContext());
                tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));


                TextView textView1 = new TextView(getApplicationContext());
                textView1.setTypeface(null, Typeface.BOLD);
                textView1.setTextColor(Color.WHITE);
                textView1.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView1.setText(" Sl No.  ");
                textView1.setGravity(Gravity.CENTER);
                textView1.setPadding(2, 2, 2, 2);
                textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView1);

                TextView textView2 = new TextView(getApplicationContext());
                textView2.setTypeface(null, Typeface.BOLD);
                textView2.setText(" Barcode  ");
                textView2.setPadding(2, 2, 2, 2);
                textView2.setTextColor(Color.WHITE);
                textView2.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView2.setGravity(Gravity.CENTER);
                textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView2);

                textView2 = new TextView(getApplicationContext());
                textView2.setTypeface(null, Typeface.BOLD);
                textView2.setText(" Product Code  ");
                textView2.setPadding(2, 2, 2, 2);
                textView2.setTextColor(Color.WHITE);
                textView2.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView2.setGravity(Gravity.CENTER);
                textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView2);


                TextView textView4 = new TextView(getApplicationContext());
                textView4.setText(" SU  ");
                textView4.setTypeface(null, Typeface.BOLD);
                textView4.setTextColor(Color.WHITE);
                textView4.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView4.setPadding(2, 2, 2, 2);
                textView4.setGravity(Gravity.CENTER);
                textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView4);

                TextView textView7 = new TextView(getApplicationContext());
                textView7.setText(" Price Unit  ");
                textView7.setTypeface(null, Typeface.BOLD);
                textView7.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView7.setTextColor(Color.WHITE);
                textView7.setPadding(2, 2, 2, 2);
                textView7.setGravity(Gravity.CENTER);
                textView7.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView7);

                TextView textView8 = new TextView(getApplicationContext());
                textView8.setText(" Item Description  ");
                textView8.setTypeface(null, Typeface.BOLD);
                textView8.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView8.setTextColor(Color.WHITE);
                textView8.setPadding(2, 2, 2, 2);
                textView8.setGravity(Gravity.CENTER);
                textView8.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView8);


                TextView textView9 = new TextView(getApplicationContext());
                textView9.setText(" Order Qty  ");
                textView9.setPadding(2, 2, 2, 2);
                textView9.setTypeface(null, Typeface.BOLD);
                textView9.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView9.setTextColor(Color.WHITE);
                textView9.setGravity(Gravity.CENTER);
                textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView9);


                TextView textView10 = new TextView(getApplicationContext());
                textView10.setText(" Shipped Qty  ");
                textView10.setTextColor(Color.WHITE);
                textView10.setTypeface(null, Typeface.BOLD);
                textView10.setGravity(Gravity.CENTER);
                textView10.setPadding(2, 2, 2, 2);
                textView10.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView10);

                TextView textView11 = new TextView(getApplicationContext());
                textView11.setText(" Received Qty  ");
                textView11.setTextColor(Color.WHITE);
                textView11.setTypeface(null, Typeface.BOLD);
                textView11.setGravity(Gravity.CENTER);
                textView11.setPadding(2, 2, 2, 2);
                textView11.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView11.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView11);

                tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                Cursor cursor = db.rawQuery("select * from Wh_Reception_Report LIMIT " + line_per_page + " OFFSET " + offset, null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        tr = new TableRow(getApplicationContext());
                        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                        textView1 = new TextView(getApplicationContext());
                        textView1.setText(cursor.getString(cursor.getColumnIndex("WRD_SLNO")) + " ");
                        textView1.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        textView1.setPadding(2, 2, 2, 2);
                        textView1.setGravity(Gravity.CENTER);
                        textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView1.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView1);

                        textView2 = new TextView(getApplicationContext());
                        textView2.setText(cursor.getString(cursor.getColumnIndex("WRD_BARCODE")) + " ");
                        textView2.setPadding(2, 2, 2, 2);
                        textView2.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView2.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView2);

                        textView2 = new TextView(getApplicationContext());
                        textView2.setText(cursor.getString(cursor.getColumnIndex("WRD_GOLD_CODE")) + " ");
                        textView2.setPadding(2, 2, 2, 2);
                        textView2.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView2.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView2);

                        textView4 = new TextView(getApplicationContext());
                        textView4.setText(cursor.getString(cursor.getColumnIndex("WRD_SU")) + "  ");
                        textView4.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView4.setPadding(2, 2, 2, 2);
                        textView4.setGravity(Gravity.CENTER);
                        textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView4.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView4);

                        textView9 = new TextView(getApplicationContext());
                        textView9.setText(cursor.getString(cursor.getColumnIndex("WRD_PRICE_UNIT")) + " ");
                        textView9.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView9.setPadding(2, 2, 2, 2);
                        textView9.setGravity(Gravity.CENTER);
                        textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView9.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView9);

                        textView10 = new TextView(getApplicationContext());
                        textView10.setText(cursor.getString(cursor.getColumnIndex("WRD_DESCRIPTION")) + "  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);

                        textView10 = new TextView(getApplicationContext());
                        textView10.setText(cursor.getString(cursor.getColumnIndex("WRD_ORDER_QTY")) + "  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);

                        textView11 = new TextView(getApplicationContext());
                        textView11.setText(cursor.getString(cursor.getColumnIndex("WRD_SHIPPED_QTY")) + "  ");
                        textView11.setGravity(Gravity.CENTER);
                        textView11.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView11.setPadding(2, 2, 2, 2);
                        textView11.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView11.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView11);

                        TextView textView12 = new TextView(getApplicationContext());
                        textView12.setText(cursor.getString(cursor.getColumnIndex("WRD_RECEIVED_QTY")) + "  ");
                        textView12.setGravity(Gravity.CENTER);
                        textView12.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView12.setPadding(2, 2, 2, 2);
                        textView12.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView12.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView12);

                        tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                        cursor.moveToNext();
                    }
                }
            }
            dialog.dismiss();
            closeKeyboard();
        }
    }

    public void nextPressed(View view) {
        System.out.println("rowcount: " + rowcount);
        System.out.println("offset: " + offset);
        if (rowcount > offset + line_per_page) {
            current_page++;
            tv_current_page.setText(String.valueOf(current_page));
            offset += line_per_page;
            OPERATION_NAME = "getWhRecReport";
            new MyTask().execute(global_po_no,"navigate");
        }
    }

    public void previousPressed(View view) {
        System.out.println("rowcount: " + rowcount);
        System.out.println("offset: " + offset);
        if (offset < rowcount && offset != 0) {
            current_page--;
            tv_current_page.setText(String.valueOf(current_page));
            offset -= line_per_page;
            OPERATION_NAME = "getWhRecReport";
            new MyTask().execute(global_po_no,"navigate");
        }
    }
}