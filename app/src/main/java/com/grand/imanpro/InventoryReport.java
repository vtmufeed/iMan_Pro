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
import android.widget.ProgressBar;
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
import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static android.widget.Toast.LENGTH_SHORT;

public class InventoryReport extends AppCompatActivity {
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
    String offline_barcode_searched="";
    //EditText edt_barcode;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_report);
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        tv_current_page = (TextView) findViewById(R.id.txt_current_page);

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
        EditText edt = (EditText) findViewById(R.id.txt_rep_inv_no);
        edt.setText(intent.getStringExtra("inv_no"));
        edt = (EditText) findViewById(R.id.txt_rep_inv_name);
        edt.setText(intent.getStringExtra("inv_name"));
        btn_search=(Button)findViewById(R.id.btn_search);
        mode = intent.getStringExtra("mode");
        if (mode.equals("offline")) {
            btn_search.setVisibility(View.VISIBLE);
            line_per_page = 21.0;
        }
        else {
            btn_search.setVisibility(View.GONE);
            line_per_page = 22.0;
        }
        if (mode.equals("offline")) {
            setPageNos("all");
        }
        tv_current_page.setText("1");
        closeKeyboard();

        /*edt_barcode = (EditText) findViewById(R.id.txt_inv_rep_barcode);
        edt_barcode.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (!edt_barcode.getText().toString().isEmpty()) {
                        if (mode.equals("offline")) {
                            OPERATION_NAME = "getOffInvReport";
                            String barcode = edt_barcode.getText().toString();
                            new MyTask().execute(barcode);
                        }
                    }
                    return true;
                }
                return false;
            }
        });*/
        if (mode.equals("offline")) {
            OPERATION_NAME = "getOffInvReport";
            new MyTask().execute();
        } else {
            OPERATION_NAME = "getInvReport";
            new MyTask().execute(intent.getStringExtra("inv_no"));
        }
    }
    public void setPageNos(String barcode)
    {
        offset = 0; rowcount = 0; current_page = 1;
        Cursor cursor = db.rawQuery("SELECT *FROM Inventory_Physical_Stock", null);
        if(barcode!="all"&&barcode.length()>0)
            cursor = db.rawQuery("SELECT *FROM Inventory_Physical_Stock where BARCODE='"+barcode+"'", null);
        cursor.moveToFirst();
        rowcount = cursor.getCount();
        TextView textView = (TextView) findViewById(R.id.txt_report_count);
        textView.setText("Count\n" + String.valueOf(rowcount));
        textView = (TextView) findViewById(R.id.txt_total_pages);
        String total_pages = String.valueOf((int) Math.ceil(rowcount / line_per_page));
        textView.setText(total_pages);
        tv_current_page.setText("1");
    }
    public void searchBarcodePressed(View view) {
        Context context = getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

// Add a TextView here for the "Title" label, as noted in the comments
        final EditText barcode = new EditText(context);
        barcode.setHint("Scan Barcode");
        barcode.setText(offline_barcode_searched);
        barcode.setTextColor(Color.parseColor("#000000"));
        barcode.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(barcode); // Notice this is an add method

        barcode.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String bar = barcode.getText().toString();
                    offline_barcode_searched=bar;
                    System.out.println(bar);
                    setPageNos(offline_barcode_searched);
                    alert_dialog.dismiss();
                    OPERATION_NAME = "getOffInvReport";
                    new MyTask().execute();

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
                setPageNos("all");
                dialog.cancel();
                OPERATION_NAME = "getOffInvReport";
                offline_barcode_searched="";
                new MyTask().execute();

            }
        });

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                setPageNos("all");
                OPERATION_NAME = "getOffInvReport";
                offline_barcode_searched="";
                new MyTask().execute();
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
            if (OPERATION_NAME.equals("getOffInvReport")) {
                response="";
            }
            if (OPERATION_NAME.equals("getInvReport")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                PropertyInfo pi = new PropertyInfo();
                pi.setName("inv_no");
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
                    query = "delete from Inventory";
                    db.execSQL(query);

                    JSONObject json = new JSONObject(response.toString());
                    JSONArray array_cat = json.getJSONArray("Table1");
                    //System.out.println(params[0]);
                    System.out.println(array_cat.length());
                    rowcount = 0;
                    for (int i = 0; i < array_cat.length(); i++) {
                        rowcount++;
                        JSONObject row_cat = array_cat.getJSONObject(i);
                        String gold_code = row_cat.getString("INVD_GOLD_CODE");
                        ///System.out.println(gold_code);
                        String barcode = row_cat.getString("BARCODE");
                        String su = row_cat.getString("INVD_SU");
                        //System.out.println(su);
                        String desc = row_cat.getString("INVD_DESCRIPTION");
                        //System.out.println(desc);
                        String sys = row_cat.getString("SYS_QTY");
                        //System.out.println(sys);
                        String phy = row_cat.getString("PHY_QTY");
                        //System.out.println(phy);
                        String var = row_cat.getString("VAR_QTY");
                        //System.out.println(var);
                        String var_val = row_cat.getString("VAR_VAL");
                        //System.out.println(var_val);
                        //System.out.println("--------------------------------");
                        insertData(i + 1, barcode,gold_code, su, desc, sys, phy, var, var_val);
                    }
                } catch (Exception ex) {
                    response = "Local Processing Error: " + ex.getMessage();
                }
            }
            if (OPERATION_NAME.equals("insertLocalDB")) {
                response = "success";
            }
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            if (OPERATION_NAME.equals("getOffInvReport")) {
                Cursor cursor;
                System.out.println("offline_barcode_searched: "+offline_barcode_searched);
                String query="";
                if (offline_barcode_searched.length() == 0)
                    query="SELECT *FROM Inventory_Physical_Stock ORDER BY SLNO DESC LIMIT " + line_per_page + " OFFSET " + offset;
                else {
                    query="SELECT *FROM Inventory_Physical_Stock where BARCODE='" + offline_barcode_searched + "' ORDER BY SLNO DESC LIMIT " + line_per_page + " OFFSET " + offset;
                }
                System.out.println("query: "+query);
                cursor = db.rawQuery(query,null);
                System.out.println("offline_barcode_searched"+offline_barcode_searched);
            //    if (cursor.getCount() > 0) {
                    TableLayout tab = (TableLayout) findViewById(R.id.tab);
                    tab.removeAllViews();
                    TableRow tr = new TableRow(getApplicationContext());
                    tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    TextView textView1 = new TextView(getApplicationContext());
                    textView1.setTypeface(null, Typeface.BOLD);
                    textView1.setTextColor(Color.WHITE);
                    textView1.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView1.setText(" Sl No  ");
                    textView1.setGravity(Gravity.CENTER);
                    textView1.setPadding(2, 2, 2, 2);
                    textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView1);

                    TextView lbl_terminal = new TextView(getApplicationContext());
                    lbl_terminal.setTypeface(null, Typeface.BOLD);
                    lbl_terminal.setTextColor(Color.WHITE);
                    lbl_terminal.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    lbl_terminal.setText(" Terminal  ");
                    lbl_terminal.setGravity(Gravity.CENTER);
                    lbl_terminal.setPadding(2, 2, 2, 2);
                    lbl_terminal.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(lbl_terminal);

                    TextView lbl_shelf = new TextView(getApplicationContext());
                    lbl_shelf.setTypeface(null, Typeface.BOLD);
                    lbl_shelf.setTextColor(Color.WHITE);
                    lbl_shelf.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    lbl_shelf.setText(" Shelf ID  ");
                    lbl_shelf.setGravity(Gravity.CENTER);
                    lbl_shelf.setPadding(2, 2, 2, 2);
                    lbl_shelf.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(lbl_shelf);

                    textView1 = new TextView(getApplicationContext());
                    textView1.setTypeface(null, Typeface.BOLD);
                    textView1.setTextColor(Color.WHITE);
                    textView1.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView1.setText(" Barcode  ");
                    textView1.setGravity(Gravity.CENTER);
                    textView1.setPadding(2, 2, 2, 2);
                    textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView1);

                    TextView textView2 = new TextView(getApplicationContext());
                    textView2.setTypeface(null, Typeface.BOLD);
                    textView2.setText(" Gold Code  ");
                    textView2.setPadding(2, 2, 2, 2);
                    textView2.setTextColor(Color.WHITE);
                    textView2.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView2.setGravity(Gravity.CENTER);
                    textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView2);


                    TextView textView4 = new TextView(getApplicationContext());
                    textView4.setText(" Stock SU  ");
                    textView4.setTypeface(null, Typeface.BOLD);
                    textView4.setTextColor(Color.WHITE);
                    textView4.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView4.setPadding(2, 2, 2, 2);
                    textView4.setGravity(Gravity.CENTER);
                    textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView4);

                    TextView textView7 = new TextView(getApplicationContext());
                    textView7.setText(" Item Description  ");
                    textView7.setTypeface(null, Typeface.BOLD);
                    textView7.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView7.setTextColor(Color.WHITE);
                    textView7.setPadding(2, 2, 2, 2);
                    textView7.setGravity(Gravity.CENTER);
                    textView7.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView7);


                    TextView textView9 = new TextView(getApplicationContext());
                    textView9.setText(" Unit Cost  ");
                    textView9.setPadding(2, 2, 2, 2);
                    textView9.setTypeface(null, Typeface.BOLD);
                    textView9.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView9.setTextColor(Color.WHITE);
                    textView9.setGravity(Gravity.CENTER);
                    textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView9);


                    TextView textView10 = new TextView(getApplicationContext());
                    textView10.setText(" Phy Stock  ");
                    textView10.setTextColor(Color.WHITE);
                    textView10.setTypeface(null, Typeface.BOLD);
                    textView10.setGravity(Gravity.CENTER);
                    textView10.setPadding(2, 2, 2, 2);
                    textView10.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView10);

                    TextView textView11 = new TextView(getApplicationContext());
                    textView11.setText(" Conv  ");
                    textView11.setTextColor(Color.WHITE);
                    textView11.setTypeface(null, Typeface.BOLD);
                    textView11.setGravity(Gravity.CENTER);
                    textView11.setPadding(2, 2, 2, 2);
                    textView11.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView11.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView11);

                    TextView textView12 = new TextView(getApplicationContext());
                    textView12.setText(" RSP  ");
                    textView12.setTextColor(Color.WHITE);
                    textView12.setTypeface(null, Typeface.BOLD);
                    textView12.setGravity(Gravity.CENTER);
                    textView12.setPadding(2, 2, 2, 2);
                    textView12.setBackground(getResources().getDrawable(
                            R.drawable.cell_shape_head));
                    textView12.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tr.addView(textView12);
                    tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                    cursor.moveToFirst();

                    for (int i = 0; i < cursor.getCount(); i++) {
                        tr = new TableRow(getApplicationContext());
                        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                        textView1 = new TextView(getApplicationContext());
                        textView1.setText(cursor.getString(cursor.getColumnIndex("SLNO")) + " ");
                        textView1.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        textView1.setPadding(2, 2, 2, 2);
                        textView1.setGravity(Gravity.CENTER);
                        textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView1.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView1);

                        lbl_terminal = new TextView(getApplicationContext());
                        lbl_terminal.setText(cursor.getString(cursor.getColumnIndex("TERMINAL")) + " ");
                        lbl_terminal.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        lbl_terminal.setPadding(2, 2, 2, 2);
                        lbl_terminal.setGravity(Gravity.CENTER);
                        lbl_terminal.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        lbl_terminal.setTextColor(Color.parseColor("#000000"));
                        tr.addView(lbl_terminal);

                        lbl_shelf = new TextView(getApplicationContext());
                        lbl_shelf.setText(cursor.getString(cursor.getColumnIndex("SHELF")) + " ");
                        lbl_shelf.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        lbl_shelf.setPadding(2, 2, 2, 2);
                        lbl_shelf.setGravity(Gravity.CENTER);
                        lbl_shelf.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        lbl_shelf.setTextColor(Color.parseColor("#000000"));
                        tr.addView(lbl_shelf);

                        textView1 = new TextView(getApplicationContext());
                        textView1.setText(cursor.getString(cursor.getColumnIndex("BARCODE")) + " ");
                        textView1.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        textView1.setPadding(2, 2, 2, 2);
                        textView1.setGravity(Gravity.CENTER);
                        textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView1.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView1);

                        textView2 = new TextView(getApplicationContext());
                        textView2.setText(cursor.getString(cursor.getColumnIndex("GOLD_CODE")) + " ");
                        textView2.setPadding(2, 2, 2, 2);
                        textView2.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView2.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView2);

                        textView4 = new TextView(getApplicationContext());
                        textView4.setText(cursor.getString(cursor.getColumnIndex("STOCK_SU")) + "  ");
                        textView4.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView4.setPadding(2, 2, 2, 2);
                        textView4.setGravity(Gravity.CENTER);
                        textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView4.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView4);

                        textView9 = new TextView(getApplicationContext());
                        textView9.setText(cursor.getString(cursor.getColumnIndex("PROD_DESC")) + " ");
                        textView9.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView9.setPadding(2, 2, 2, 2);
                        textView9.setGravity(Gravity.CENTER);
                        textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView9.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView9);

                        textView10 = new TextView(getApplicationContext());
                        textView10.setText(cursor.getString(cursor.getColumnIndex("UNIT_COST")) + "  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);

                        textView10 = new TextView(getApplicationContext());
                        textView10.setText(cursor.getString(cursor.getColumnIndex("PHY_STOCK")) + "  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);

                        textView11 = new TextView(getApplicationContext());
                        textView11.setText(cursor.getString(cursor.getColumnIndex("CONV")) + "  ");
                        textView11.setGravity(Gravity.CENTER);
                        textView11.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView11.setPadding(2, 2, 2, 2);
                        textView11.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView11.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView11);

                        textView12 = new TextView(getApplicationContext());
                        textView12.setText(cursor.getString(cursor.getColumnIndex("RSP")) + "  ");
                        textView12.setGravity(Gravity.CENTER);
                        textView12.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView12.setPadding(2, 2, 2, 2);
                        textView12.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                        textView12.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView12);

                        tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                        cursor.moveToNext();
                   // }
                }
                closeKeyboard();
            }
            if (OPERATION_NAME.equals("insertLocalDB")) {
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
                textView7.setText(" Item Description  ");
                textView7.setTypeface(null, Typeface.BOLD);
                textView7.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView7.setTextColor(Color.WHITE);
                textView7.setPadding(2, 2, 2, 2);
                textView7.setGravity(Gravity.CENTER);
                textView7.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView7);


                TextView textView9 = new TextView(getApplicationContext());
                textView9.setText(" Sys Stock  ");
                textView9.setPadding(2, 2, 2, 2);
                textView9.setTypeface(null, Typeface.BOLD);
                textView9.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView9.setTextColor(Color.WHITE);
                textView9.setGravity(Gravity.CENTER);
                textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView9);


                TextView textView10 = new TextView(getApplicationContext());
                textView10.setText(" Phy Stock  ");
                textView10.setTextColor(Color.WHITE);
                textView10.setTypeface(null, Typeface.BOLD);
                textView10.setGravity(Gravity.CENTER);
                textView10.setPadding(2, 2, 2, 2);
                textView10.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView10);

                TextView textView11 = new TextView(getApplicationContext());
                textView11.setText(" Var Qty  ");
                textView11.setTextColor(Color.WHITE);
                textView11.setTypeface(null, Typeface.BOLD);
                textView11.setGravity(Gravity.CENTER);
                textView11.setPadding(2, 2, 2, 2);
                textView11.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView11.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView11);

                TextView textView12 = new TextView(getApplicationContext());
                textView12.setText(" Var Val  ");
                textView12.setTextColor(Color.WHITE);
                textView12.setTypeface(null, Typeface.BOLD);
                textView12.setGravity(Gravity.CENTER);
                textView12.setPadding(2, 2, 2, 2);
                textView12.setBackground(getResources().getDrawable(
                        R.drawable.cell_shape_head));
                textView12.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
                tr.addView(textView12);
                tab.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

                Cursor cursor = db.rawQuery("select * from Inventory LIMIT " + line_per_page + " OFFSET " + offset, null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        tr = new TableRow(getApplicationContext());
                        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                        textView1 = new TextView(getApplicationContext());
                        textView1.setText(cursor.getString(cursor.getColumnIndex("slno")) + " ");
                        textView1.setBackground(getResources().getDrawable(R.drawable.cell_shape));
                        textView1.setPadding(2, 2, 2, 2);
                        textView1.setGravity(Gravity.CENTER);
                        textView1.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView1.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView1);

                        textView2 = new TextView(getApplicationContext());
                        textView2.setText(cursor.getString(cursor.getColumnIndex("barcode")) + " ");
                        textView2.setPadding(2, 2, 2, 2);
                        textView2.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView2.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView2);

                        textView2 = new TextView(getApplicationContext());
                        textView2.setText(cursor.getString(cursor.getColumnIndex("gold_code")) + " ");
                        textView2.setPadding(2, 2, 2, 2);
                        textView2.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView2.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView2);

                        textView4 = new TextView(getApplicationContext());
                        textView4.setText(cursor.getString(cursor.getColumnIndex("SU")) + "  ");
                        textView4.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView4.setPadding(2, 2, 2, 2);
                        textView4.setGravity(Gravity.CENTER);
                        textView4.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView4.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView4);

                        textView9 = new TextView(getApplicationContext());
                        textView9.setText(cursor.getString(cursor.getColumnIndex("P_DESC")) + " ");
                        textView9.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView9.setPadding(2, 2, 2, 2);
                        textView9.setGravity(Gravity.CENTER);
                        textView9.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView9.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView9);

                        textView10 = new TextView(getApplicationContext());
                        textView10.setText(cursor.getString(cursor.getColumnIndex("SYS_QTY")) + "  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);

                        textView10 = new TextView(getApplicationContext());
                        textView10.setText(cursor.getString(cursor.getColumnIndex("PHY_QTY")) + "  ");
                        textView10.setGravity(Gravity.CENTER);
                        textView10.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView10.setPadding(2, 2, 2, 2);
                        textView10.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView10.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView10);

                        textView11 = new TextView(getApplicationContext());
                        textView11.setText(cursor.getString(cursor.getColumnIndex("VAR_QTY")) + "  ");
                        textView11.setGravity(Gravity.CENTER);
                        textView11.setBackground(getResources().getDrawable(
                                R.drawable.cell_shape));
                        textView11.setPadding(2, 2, 2, 2);
                        textView11.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.MATCH_PARENT));
                        textView11.setTextColor(Color.parseColor("#000000"));
                        tr.addView(textView11);

                        textView12 = new TextView(getApplicationContext());
                        textView12.setText(cursor.getString(cursor.getColumnIndex("VAR_VAL")) + "  ");
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
            if (OPERATION_NAME.equals("getInvReport")) {
                TextView textView = (TextView) findViewById(R.id.txt_report_count);
                textView.setText("Count\n" + String.valueOf(rowcount));
                System.out.println("Row count " + rowcount);
                textView = (TextView) findViewById(R.id.txt_total_pages);
                String total_pages = String.valueOf((int) Math.ceil(rowcount / line_per_page));
                textView.setText(total_pages);
                OPERATION_NAME = "insertLocalDB";
                new MyTask().execute(result);
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
            if (mode.equals("offline")) {
                OPERATION_NAME = "getOffInvReport";
                new MyTask().execute();
            } else {
                OPERATION_NAME = "insertLocalDB";
                new MyTask().execute();
            }
        }
    }

    public void previousPressed(View view) {
        System.out.println("rowcount: " + rowcount);
        System.out.println("offset: " + offset);
        if (offset < rowcount && offset != 0) {
            current_page--;
            tv_current_page.setText(String.valueOf(current_page));
            offset -= line_per_page;
            if (mode.equals("offline")) {
                OPERATION_NAME = "getOffInvReport";
                new MyTask().execute();
            } else {
                OPERATION_NAME = "insertLocalDB";
                new MyTask().execute();
            }
        }
    }
}