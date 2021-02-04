package com.grand.imanpro;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.w3c.dom.Text;

import static android.widget.LinearLayout.*;
import static android.widget.Toast.LENGTH_SHORT;

public class ArticleData extends AppCompatActivity {
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
    ProgressDialog dialog_download;
    String code_type = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_data);

        dialog_download = new ProgressDialog(this);
        dialog_download.setCancelable(false);
        dialog_download.setInverseBackgroundForced(false);

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

        final EditText edittext = (EditText) findViewById(R.id.txt_data_barcode);
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (!edittext.getText().toString().isEmpty()) {
                        OPERATION_NAME = "getArtDataDetl";
                        String barcode = edittext.getText().toString();
                        EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                        new MyTask().execute(barcode, loc);
                        //Toast.makeText(getApplicationContext(),"sdadsad",LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public void openQrCamera(View view) {
        code_type = "qr";
        if (checkPermission()) {
            try {
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("Scan QR Code");
                integrator.setResultDisplayDuration(0);
                integrator.setWide();  // Wide scanning rectangle, may work better for 1D barcodes
                integrator.setCameraId(0);  // Use a specific camera of the device
                integrator.initiateScan();
            } catch (Exception ex) {
                Toast.makeText(this, "Error: " + ex.getMessage().toString(), LENGTH_SHORT).show();
            }
        } else {
            requestPermission();
        }
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private static final int PERMISSION_REQUEST_CODE = 200;

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    if(code_type.equals("barcode"))
                        openCamera(null);
                    if(code_type.equals("qr"))
                        openQrCamera(null);
                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ArticleData.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void openCamera(View view) {
        code_type = "barcode";
        if (checkPermission()) {
            try {
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
                integrator.setPrompt("Scan a barcode");
                integrator.setResultDisplayDuration(0);
                integrator.setWide();  // Wide scanning rectangle, may work better for 1D barcodes
                integrator.setCameraId(0);  // Use a specific camera of the device
                integrator.initiateScan();
            } catch (Exception ex) {
                Toast.makeText(this, "Please give Camera permission in Settings", LENGTH_SHORT).show();
            }
        } else {
            requestPermission();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        if (code_type.equals("barcode")) {
            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            String codeContent;
            if (scanningResult != null) {
                //we have a result
                codeContent = scanningResult.getContents();
                if(codeContent!=null) {
                    if (codeContent.length() > 0) {
                        EditText editText = (EditText) findViewById(R.id.txt_data_barcode);
                        editText.setText(codeContent);
                        OPERATION_NAME = "getArtDataDetl";
                        new MyTask().execute(codeContent, loc);
                    }
                }
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        if (code_type.equals("qr")) {
            IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
            String codeContent;
            if (scanningResult != null) {
                //we have a result
                codeContent = scanningResult.getContents();

                if(codeContent!=null) {
                    if(codeContent.length()>0) {
                        if (codeContent.contains("EXCL/")) {
                            Toast.makeText(getApplicationContext(), codeContent, Toast.LENGTH_SHORT).show();
                            new downloadBarcodes().execute(codeContent);
                        } else {
                            Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                }
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private class downloadBarcodes extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog_download.show();
            /*ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);*/
        }

        @Override
        protected String doInBackground(String... params) {
            dialog_download.setMessage("Connecting Server...");
            Object response = null;
            String status = "";
            SOAP_ACTION = "http://tempuri.org/getExcelBarcodes";
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, "getExcelBarcodes");

            PropertyInfo pi = new PropertyInfo();
            pi.setName("excel_id");
            pi.setType(String.class);
            pi.setValue(params[0]);
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("user_id");
            pi.setType(String.class);
            pi.setValue("user");
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

            String result = response.toString();
            System.out.println(result);

            try {
                if (result.split(",")[0].equals("success")) {
                    String[] data = result.split(",");
                    publishProgress("Deleting Existing Barcodes");
                    db.execSQL("delete from Excel_Barcodes");
                    int i = 0;
                    for (i = 1; i < data.length; i++) {
                        db.execSQL("insert into Excel_Barcodes (BARCODE) values('" + data[i] + "')");
                        publishProgress(i + " Records Inserted.");
                    }
                    status = "success," + i;
                } else {
                    status = result;
                }
            } catch (Exception Ex) {
                status = "Local Processing Error: " + Ex.getMessage().toString();
            }
            return status;
        }
        //System.out.println(response.toString());

        protected void onProgressUpdate(String... value) {
            dialog_download.setMessage(value[0]);
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            dialog_download.cancel();
            if (result.contains("success")) {
                barcodeFinished(result);
            }
            else
            {
                Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
            }
        }
    }
    public void barcodeFinished(String result)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(ArticleData.this);
        builder.setTitle("Download Successful");
        builder.setCancelable(false);
        // Set up the input
        builder.setMessage(result.split(",")[1] + " Barcodes Inserted.");

        // Set up the buttons
        builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                EditText edt=(EditText)findViewById(R.id.txt_data_barcode);
                edt.setText("");
                edt.requestFocus();
                LinearLayout layout=(LinearLayout)findViewById(R.id.dataScrollView);
                layout.setVisibility(INVISIBLE);
            }
        });
        builder.show();
    }
    public void clearAll(View view) {
        LayoutTransition transition = new LayoutTransition();
        LinearLayout layout = (LinearLayout) findViewById(R.id.layout_art_data);
        layout.setLayoutTransition(transition);
        layout = (LinearLayout) findViewById(R.id.layout_stock);
        layout.setLayoutTransition(transition);
        layout = (LinearLayout) findViewById(R.id.layout_suppplier);
        layout.setLayoutTransition(transition);
        LinearLayout scrollView = (LinearLayout) findViewById(R.id.dataScrollView);
        scrollView.setVisibility(View.INVISIBLE);
        EditText edt = (EditText) findViewById(R.id.txt_data_barcode);
        edt.setText("");
        edt.requestFocus();
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void showBarcode(String data) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(VERTICAL);
        TextView txt = new TextView(this);
        txt.setTextSize(18);
        //float a=1.5f;
        txt.setLineSpacing(1.2f, 1.2f);
        txt.setTextColor(Color.BLACK);
        layout.setPadding(20, 20, 20, 20);
        txt.setPadding(20, 20, 20, 20);
        txt.setText(data.replace(",", "\n"));
        //layout.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_button));
        layout.addView(txt);
        scrollView.addView(layout);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Su Barcodes");
        builder.setView(scrollView);
        //builder.setMessage(data.replace(",","\n"));
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setIcon(R.drawable.barcode_icon);
        builder.show();
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
            if (OPERATION_NAME.equals("getArtDataDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("barcode");
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

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS);
                System.out.println(SOAP_ADDRESS);

                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            if (OPERATION_NAME.equals("getArtDataBarcode")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("gold_code");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("su");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                System.out.println(params[0] + " " + params[1] + " " + loc);

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
            //System.out.println(response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            if (OPERATION_NAME.equals("getArtDataBarcode")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    String data = result.replace("success,", "");
                    showBarcode(data);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("getArtDataDetl")) {
                //System.out.println(result);
                if (result.toUpperCase().contains("SUCCESS")) {
                    LinearLayout scrollView = (LinearLayout) findViewById(R.id.dataScrollView);
                    scrollView.setVisibility(View.VISIBLE);
                    closeKeyboard();
                    LayoutTransition transition = new LayoutTransition();
                    LinearLayout layout = (LinearLayout) findViewById(R.id.layout_art_data);
                    layout.setLayoutTransition(transition);
                    layout = (LinearLayout) findViewById(R.id.layout_stock);
                    layout.setLayoutTransition(transition);
                    layout = (LinearLayout) findViewById(R.id.layout_suppplier);
                    layout.setLayoutTransition(transition);
                    /*String[] data=result.split(",");
                    String prod_code=data[1];
                    String su=data[2];
                    String desc=data[3];
                    String stock_unit=data[4];
                    String section=data[5];
                    String rsp=data[6];
                    String unit_cost=data[7];
                    String conv=data[8];*/
                    try {
                        final JSONObject json = new JSONObject(result);
                        TextView txt = (TextView) findViewById(R.id.txt_data_prod);
                        txt.setText(json.getString("GOLD_CODE"));
                        txt = (TextView) findViewById(R.id.txt_data_su);
                        txt.setText(json.getString("SU"));
                        txt = (TextView) findViewById(R.id.txt_data_desc);
                        txt.setText(json.getString("GOLD_DESC"));
                        txt = (TextView) findViewById(R.id.txt_data_stock_unit);
                        txt.setText(json.getString("STOCK_UNIT"));
                        txt = (TextView) findViewById(R.id.txt_data_cat);
                        txt.setText(json.getString("SECTION"));
                        txt = (TextView) findViewById(R.id.txt_data_rsp);
                        txt.setText(json.getString("RSP"));
                        txt = (TextView) findViewById(R.id.txt_data_cost);
                        txt.setText(json.getString("UNIT_COST"));
                        txt = (TextView) findViewById(R.id.txt_data_conv);
                        txt.setText(json.getString("CONV"));
                        txt = (TextView) findViewById(R.id.txt_lst_supplier);
                        txt.setText(json.getString("LST_SUPP_CODE") + "\n" + json.getString("LST_SUPP_NAME"));
                        txt = (TextView) findViewById(R.id.txt_lst_supp_dt);
                        txt.setText(json.getString("LST_SUPP_DT"));
                        txt = (TextView) findViewById(R.id.txt_excel_exist);

                        Cursor cursor=db.rawQuery("select count(*) from Excel_Barcodes where BARCODE='"+json.getString("BARCODE")+"'",null);
                        cursor.moveToFirst();
                        int count=Integer.parseInt(cursor.getString(0));
                        if(count==0)
                        {
                            txt.setTextColor(Color.parseColor("#063844"));
                            txt.setText("No");
                        }
                        else
                        {
                            txt.setTextColor(Color.parseColor("#ED194A"));
                            txt.setText("Yes");
                        }

                        final String gold_code = json.getString("GOLD_CODE");
                        String stock_unit = json.getString("STOCK_UNIT").toLowerCase();
                        JSONObject json_stock_data = new JSONObject(json.getString("STOCK_DETL"));
                        if(stock_unit.contains("piece"))
                            stock_unit="Pcs";
                        if(stock_unit.contains("kilo"))
                            stock_unit="Kilos";
                        loadSuStock(json_stock_data, gold_code,stock_unit);
                        JSONObject json_supp_data = new JSONObject(json.getString("SUPPLIER"));
                        loadSupplier(json_supp_data);
                        System.out.println(json.getString("STOCK_DETL"));
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                    clearAll(null);
                }
            }
            dialog.cancel();
        }
    }

    public void loadSupplier(JSONObject json_supp_data) {
        try {
            JSONArray leaders = json_supp_data.getJSONArray("GRAND_ACTIVE_ORDERABLE_ASSORT@GOLDDB");
            LinearLayout layout_supp_data = findViewById(R.id.layout_suppplier);
            layout_supp_data.removeAllViews();

            LinearLayout layout_header = new LinearLayout(getApplicationContext());
            layout_header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout_header.setOrientation(HORIZONTAL);

            TextView txt_hd_lv = new TextView(getApplicationContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            params.setMargins(10, 0, 0, 0);
            txt_hd_lv.setLayoutParams(params);
            txt_hd_lv.setText("Lv");
            txt_hd_lv.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_lv.setTextColor(Color.parseColor("#11719e"));

            TextView txt_hd_supp_code = new TextView(getApplicationContext());
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            params.setMargins(10, 0, 0, 0);
            txt_hd_supp_code.setLayoutParams(params);
            txt_hd_supp_code.setText("Code");
            txt_hd_supp_code.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_supp_code.setTextColor(Color.parseColor("#11719e"));

            TextView txt_hd_supp = new TextView(getApplicationContext());
            txt_hd_supp.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 3));
            txt_hd_supp.setText("Supplier");
            txt_hd_supp.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_supp.setTextColor(Color.parseColor("#11719e"));

            TextView txt_hd_unit = new TextView(getApplicationContext());
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            params.setMargins(40, 0, 0, 0);
            txt_hd_unit.setLayoutParams(params);
            txt_hd_unit.setText("Unit");
            txt_hd_unit.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_unit.setTextColor(Color.parseColor("#11719e"));

            TextView txt_hd_main = new TextView(getApplicationContext());
            txt_hd_main.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            txt_hd_main.setText("Main");
            txt_hd_main.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_main.setTextColor(Color.parseColor("#11719e"));

            layout_header.addView(txt_hd_lv);
            layout_header.addView(txt_hd_supp_code);
            layout_header.addView(txt_hd_supp);
            layout_header.addView(txt_hd_unit);
            layout_header.addView(txt_hd_main);

            layout_supp_data.addView(layout_header);
            for (int i = 0; i <= leaders.length(); i++) {
                JSONObject jsonas = null;
                jsonas = leaders.getJSONObject(i);
                String p_lv = jsonas.getString("LV");
                String p_supp_code = jsonas.getString("SUPPLIER_CODE");
                String p_supplier = jsonas.getString("SUPPLIER_NAME");
                String p_unit = jsonas.getString("ORDER_UNIT");
                String p_main = jsonas.getString("MAIN_SUPP");

                //System.out.println(p_su + "\n" + p_prod_desc + "\n" + p_conv + "\n" + p_linked_su + "\n" + p_soh);

                final LinearLayout layout_data_container = new LinearLayout(getApplicationContext());
                layout_data_container.setPadding(10, 10, 10, 10);
                layout_data_container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout_data_container.setOrientation(VERTICAL);

                LinearLayout layout_data_row = new LinearLayout(getApplicationContext());
                layout_data_row.setPadding(10, 10, 10, 10);
                layout_data_row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout_data_row.setOrientation(HORIZONTAL);

                TextView txt_lv = new TextView(getApplicationContext());
                txt_lv.setText(p_lv);
                txt_lv.setTextColor(Color.parseColor("#ffffff"));
                TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                txt_lv.setLayoutParams(layoutParams);
                txt_lv.setPadding(20, 10, 0, 10);

                TextView txt_supp = new TextView(getApplicationContext());
                txt_supp.setText(p_supp_code);
                txt_supp.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                txt_supp.setLayoutParams(layoutParams);
                txt_supp.setPadding(10, 10, 5, 10);

                TextView txt_supplier = new TextView(getApplicationContext());
                txt_supplier.setText(p_supplier);
                txt_supplier.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = 200;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 3;
                txt_supplier.setLayoutParams(layoutParams);
                txt_supplier.setPadding(10, 10, 10, 10);

                TextView txt_unit = new TextView(getApplicationContext());
                txt_unit.setText(p_unit);
                txt_unit.setTextColor(Color.parseColor("#ffffff"));
                txt_unit.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                txt_unit.setLayoutParams(layoutParams);
                txt_unit.setPadding(10, 10, 10, 10);

                /*TextView txt_main = new TextView(getApplicationContext());
                txt_main.setText(p_main);
                txt_main.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                txt_main.setLayoutParams(layoutParams);
                txt_main.setPadding(10, 10, 10, 10);*/


                ImageView imageView = new ImageView(getApplicationContext());
                if (p_main.contains("Y"))
                    imageView.setImageResource(R.drawable.main_supp);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
                imageView.setPadding(10, 10, 10, 10);
                LinearLayout.LayoutParams image_params = (LinearLayout.LayoutParams) imageView.getLayoutParams();
                image_params.gravity = Gravity.CENTER;
                image_params.setMargins(0, 0, 15, 0);
                imageView.setLayoutParams(image_params);
                //layout_data_row.addView(imageView);
                layout_data_row.addView(txt_lv);
                layout_data_row.addView(txt_supp);
                layout_data_row.addView(txt_supplier);
                layout_data_row.addView(txt_unit);
                layout_data_row.addView(imageView);

                /*layout_data_container.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OPERATION_NAME = "getArtDataBarcode";
                        new MyTask().execute(gold_code, p_su);
                        //Toast.makeText(getApplicationContext(), p_su, Toast.LENGTH_LONG).show();
                    }
                });*/
                layout_data_row.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_card_supplier));
                // }
                layout_data_container.addView(layout_data_row);
                //layout_data_container.addView(txt);
                layout_supp_data.addView(layout_data_container);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private View contentView;
    private View loadingView;
    private int shortAnimationDuration;

    private void crossfade() {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        contentView.setAlpha(0f);
        contentView.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        contentView.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        final LinearLayout layout = (LinearLayout) findViewById(R.id.layout_stock);
        layout.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        layout.setVisibility(View.GONE);
                    }
                });
    }

    public static Integer pxFromDp(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public void loadSuStock(JSONObject json_stock_data, final String gold_code,final String stk_unit) {
        try {
            JSONArray leaders = json_stock_data.getJSONArray("GRAND_PRD_MASTER_FULL@GOLDDB");
            final LinearLayout layout_su_data = findViewById(R.id.layout_stock);
            layout_su_data.removeAllViews();

            LinearLayout layout_header = new LinearLayout(getApplicationContext());
            layout_header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout_header.setOrientation(HORIZONTAL);

            TextView txt_hd_su = new TextView(getApplicationContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            params.setMargins(10, 0, 0, 0);
            txt_hd_su.setLayoutParams(params);
            txt_hd_su.setText("Su");
            txt_hd_su.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_su.setTextColor(Color.parseColor("#ED194A"));

            TextView txt_hd_desc = new TextView(getApplicationContext());
            txt_hd_desc.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 3));
            txt_hd_desc.setText("Description");
            txt_hd_desc.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_desc.setTextColor(Color.parseColor("#ED194A"));

            TextView txt_hd_conv = new TextView(getApplicationContext());
            txt_hd_conv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            txt_hd_conv.setText("Conv");
            txt_hd_conv.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_conv.setTextColor(Color.parseColor("#ED194A"));

            TextView txt_hd_stock = new TextView(getApplicationContext());
            txt_hd_stock.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            txt_hd_stock.setText("Stock");
            txt_hd_stock.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_stock.setTextColor(Color.parseColor("#ED194A"));

            TextView txt_hd_link = new TextView(getApplicationContext());
            txt_hd_link.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            txt_hd_link.setText("Link");
            txt_hd_link.setTypeface(Typeface.DEFAULT_BOLD);
            txt_hd_link.setTextColor(Color.parseColor("#ED194A"));

            layout_header.addView(txt_hd_su);
            layout_header.addView(txt_hd_desc);
            layout_header.addView(txt_hd_conv);
            layout_header.addView(txt_hd_stock);
            layout_header.addView(txt_hd_link);

            //layout_su_data.addView(layout_header);
            for (int i = 0; i <= leaders.length(); i++) {
                JSONObject jsonas = null;
                jsonas = leaders.getJSONObject(i);
                final String p_su = jsonas.getString("SU");
                final String p_prod_desc = jsonas.getString("SU_DESC");
                String p_conv = jsonas.getString("SU_CONV");
                String p_linked_su = jsonas.getString("LINKED_SU");
                String p_soh = jsonas.getString("SU_SOH");
                String p_wh1_soh = jsonas.getString("WH1_STK");
                String p_wh2_soh = jsonas.getString("WH2_STK");
                String p_rsp = jsonas.getString("RSP");
                String p_sale = jsonas.getString("SALE_QTY");

                System.out.println(p_su + "\n" + p_prod_desc + "\n" + p_conv + "\n" + p_linked_su + "\n" + p_soh);

                LinearLayout layout_card_su_data = new LinearLayout(getApplicationContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(pxFromDp(getApplicationContext(), 10), pxFromDp(getApplicationContext(), 3), pxFromDp(getApplicationContext(), 10), 0);
                layout_card_su_data.setOrientation(HORIZONTAL);
                layout_card_su_data.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_art_data_row));
                layout_card_su_data.setLayoutParams(layoutParams);

                TextView txt_su = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.2f);
                layoutParams.setMargins(pxFromDp(getApplicationContext(), 8), 0, 0, 0);
                /*Typeface face = ResourcesCompat.getFont(ArticleData.this, R.font.didact_gothic);
                txt_su.setTypeface(face);*/
                txt_su.setGravity(Gravity.CENTER_HORIZONTAL + Gravity.CENTER_VERTICAL);
                txt_su.setTextSize(14);
                txt_su.setText(p_su);
                //txt_su.setTextColor(Color.parseColor("#ffffff"));
                txt_su.setTextColor(Color.parseColor("#ffffff"));
                txt_su.setLayoutParams(layoutParams);

                LinearLayout layout_description = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                layout_description.setOrientation(VERTICAL);
                layout_description.setLayoutParams(layoutParams);


                TextView txt_desc = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(pxFromDp(getApplicationContext(), 200), ViewGroup.LayoutParams.WRAP_CONTENT);
                txt_desc.setPadding(pxFromDp(getApplicationContext(), 10), pxFromDp(getApplicationContext(), 10), pxFromDp(getApplicationContext(), 10), pxFromDp(getApplicationContext(), 10));
                //txt_desc.setTypeface(face);
                txt_desc.setGravity(Gravity.CENTER_VERTICAL);
                txt_desc.setTextSize(16);
                txt_desc.setText(p_prod_desc);
                //txt_desc.setTextColor(Color.parseColor("#ffffff"));
                txt_desc.setTextColor(Color.parseColor("#ffffff"));
                txt_desc.setLayoutParams(layoutParams);

                //RSP
                LinearLayout layout_rsp = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layout_rsp.setOrientation(HORIZONTAL);
                layout_rsp.setLayoutParams(layoutParams);

                TextView txt_qar = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(pxFromDp(getApplicationContext(), 13), 0, 0, 0);
                //txt_qar.setTypeface(face);
                txt_qar.setGravity(Gravity.CENTER_VERTICAL);
                txt_qar.setTextSize(10);
                txt_qar.setText("QAR ");
                //txt_desc.setTextColor(Color.parseColor("#ffffff"));
                txt_qar.setTextColor(Color.parseColor("#DADADA"));
                txt_qar.setLayoutParams(layoutParams);

                TextView txt_rsp = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                //txt_rsp.setTypeface(face);
                txt_rsp.setGravity(Gravity.CENTER_VERTICAL);
                txt_rsp.setTextSize(14);
                txt_rsp.setText(p_rsp);
                //txt_desc.setTextColor(Color.parseColor("#ffffff"));
                txt_rsp.setTextColor(Color.parseColor("#ffffff"));
                txt_rsp.setLayoutParams(layoutParams);

                layout_rsp.addView(txt_qar);
                layout_rsp.addView(txt_rsp);
                //ENDING RSP

                //TODAY SALE
                LinearLayout layout_sale = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layout_sale.setOrientation(HORIZONTAL);
                layout_sale.setLayoutParams(layoutParams);

                TextView txt_lbl_sale = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(pxFromDp(getApplicationContext(), 13), 0, 0, 0);
                //txt_lbl_sale.setTypeface(face);
                txt_lbl_sale.setGravity(Gravity.CENTER_VERTICAL);
                txt_lbl_sale.setTextSize(10);
                txt_lbl_sale.setText("Today Sale  ");
                //txt_desc.setTextColor(Color.parseColor("#ffffff"));
                txt_lbl_sale.setTextColor(Color.parseColor("#DADADA"));
                txt_lbl_sale.setLayoutParams(layoutParams);

                TextView txt_sale = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                //txt_sale.setTypeface(face);
                txt_sale.setGravity(Gravity.CENTER_VERTICAL);
                txt_sale.setTextSize(14);
                txt_sale.setText(p_sale+" "+stk_unit);
                //txt_desc.setTextColor(Color.parseColor("#ffffff"));
                txt_sale.setTextColor(Color.parseColor("#ffffff"));
                txt_sale.setLayoutParams(layoutParams);

                layout_sale.addView(txt_lbl_sale);
                layout_sale.addView(txt_sale);
                //TODAY SALE

                layout_description.addView(txt_desc);
                layout_description.addView(layout_rsp);
                layout_description.addView(layout_sale);

                LinearLayout.LayoutParams divider_params = new LinearLayout.LayoutParams(3, ViewGroup.LayoutParams.MATCH_PARENT);
                LinearLayout layout_divider_1 = new LinearLayout(new ContextThemeWrapper(getApplicationContext(), R.style.Divider));
                layout_divider_1.setLayoutParams(divider_params);
                layout_divider_1.setBackgroundColor(Color.parseColor("#333333"));

                LinearLayout layout_stock_data = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0.7f);
                layoutParams.leftMargin = pxFromDp(getApplicationContext(), 5);
                layout_stock_data.setOrientation(VERTICAL);
                layout_stock_data.setLayoutParams(layoutParams);

                LinearLayout layout_stock_header = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                layout_stock_header.setOrientation(HORIZONTAL);
                layout_stock_header.setPadding(pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5));
                layout_stock_header.setLayoutParams(layoutParams);

                TextView txt_header_conv = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_header_conv.setTypeface(face);
                txt_header_conv.setGravity(Gravity.CENTER_HORIZONTAL);
                txt_header_conv.setText("Conv");
                txt_header_conv.setTextColor(Color.parseColor("#9F9F9F"));
                txt_header_conv.setLayoutParams(layoutParams);

                TextView txt_header_stock = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_header_stock.setTypeface(face);
                txt_header_stock.setGravity(Gravity.CENTER_HORIZONTAL);
                txt_header_stock.setText("Stock");
                txt_header_stock.setTextColor(Color.parseColor("#9F9F9F"));
                txt_header_stock.setLayoutParams(layoutParams);

                layout_stock_header.addView(txt_header_conv);
                layout_stock_header.addView(txt_header_stock);

                LinearLayout layout_stock_site = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                layout_stock_site.setOrientation(HORIZONTAL);
                layout_stock_site.setPadding(pxFromDp(getApplicationContext(), 3), pxFromDp(getApplicationContext(), 3), pxFromDp(getApplicationContext(), 3), pxFromDp(getApplicationContext(), 3));
                layout_stock_site.setLayoutParams(layoutParams);

                TextView txt_conv = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_conv.setTypeface(face);
                txt_conv.setGravity(Gravity.CENTER_HORIZONTAL);
                txt_conv.setText(p_conv);
                txt_conv.setTextColor(Color.parseColor("#ffffff"));
                //txt_conv.setTextColor(Color.parseColor("#ffffff"));
                txt_conv.setLayoutParams(layoutParams);

                TextView txt_stock = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_stock.setTypeface(face);
                txt_stock.setGravity(Gravity.CENTER_HORIZONTAL);
                txt_stock.setText(p_soh);
                txt_stock.setTextColor(Color.parseColor("#ffffff"));
                //txt_stock.setTextColor(Color.parseColor("#ffffff"));
                txt_stock.setLayoutParams(layoutParams);

                layout_stock_site.addView(txt_conv);
                layout_stock_site.addView(txt_stock);

                LinearLayout.LayoutParams divider_params_h = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 3);
                LinearLayout layout_divider_h = new LinearLayout(new ContextThemeWrapper(getApplicationContext(), R.style.Divider_Horizontal));
                layout_divider_h.setLayoutParams(divider_params_h);
                layout_divider_h.setBackgroundColor(Color.parseColor("#333333"));

                LinearLayout layout_wh1_stock = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                layout_wh1_stock.setOrientation(HORIZONTAL);
                layout_wh1_stock.setPadding(pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5));
                layout_wh1_stock.setLayoutParams(layoutParams);

                TextView txt_header_wh1 = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_header_wh1.setTypeface(face);
                txt_header_wh1.setText("WH1");
                txt_header_wh1.setTextColor(Color.parseColor("#9F9F9F"));
                txt_header_wh1.setLayoutParams(layoutParams);

                TextView txt_soh_wh1 = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_soh_wh1.setTypeface(face);
                txt_soh_wh1.setTextColor(Color.parseColor("#ffffff"));
                txt_soh_wh1.setTextColor(Color.parseColor("#ffffff"));
                txt_soh_wh1.setGravity(Gravity.CENTER_HORIZONTAL);
                if (p_linked_su.equals("-"))
                    txt_soh_wh1.setText(p_wh1_soh);
                else
                    txt_soh_wh1.setText("-");
                txt_soh_wh1.setLayoutParams(layoutParams);

                layout_wh1_stock.addView(txt_header_wh1);
                layout_wh1_stock.addView(txt_soh_wh1);

                LinearLayout layout_wh2_stock = new LinearLayout(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                layout_wh2_stock.setOrientation(HORIZONTAL);
                layout_wh2_stock.setPadding(pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5), pxFromDp(getApplicationContext(), 5));
                layout_wh2_stock.setLayoutParams(layoutParams);

                TextView txt_header_wh2 = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_header_wh2.setTypeface(face);
                txt_header_wh2.setText("WH2");
                txt_header_wh2.setTextColor(Color.parseColor("#9F9F9F"));
                txt_header_wh2.setLayoutParams(layoutParams);

                TextView txt_soh_wh2 = new TextView(getApplicationContext());
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f);
                //txt_soh_wh2.setTypeface(face);
                txt_soh_wh2.setGravity(Gravity.CENTER_HORIZONTAL);
                if (p_linked_su.equals("-"))
                    txt_soh_wh2.setText(p_wh2_soh);
                else
                    txt_soh_wh2.setText("-");
                txt_soh_wh2.setTextColor(Color.parseColor("#ffffff"));
                //txt_soh_wh2.setTextColor(Color.parseColor("#ffffff"));
                txt_soh_wh2.setLayoutParams(layoutParams);

                layout_wh2_stock.addView(txt_header_wh2);
                layout_wh2_stock.addView(txt_soh_wh2);

                layout_stock_data.addView(layout_stock_header);
                layout_stock_data.addView(layout_stock_site);
                layout_stock_data.addView(layout_divider_h);
                layout_stock_data.addView(layout_wh1_stock);
                layout_stock_data.addView(layout_wh2_stock);

                layout_card_su_data.addView(txt_su);
                layout_card_su_data.addView(layout_description);
                layout_card_su_data.addView(layout_divider_1);
                layout_card_su_data.addView(layout_stock_data);

                layout_su_data.addView(layout_card_su_data);

                layout_card_su_data.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OPERATION_NAME = "getArtDataBarcode";
                        System.out.println(p_prod_desc);
                        new MyTask().execute(gold_code, p_su);
                        //Toast.makeText(getApplicationContext(), p_su, Toast.LENGTH_LONG).show();
                    }
                });

                /*final LinearLayout layout_data_container = new LinearLayout(getApplicationContext());
                layout_data_container.setPadding(10, 10, 10, 10);
                layout_data_container.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout_data_container.setOrientation(VERTICAL);

                LinearLayout layout_data_row = new LinearLayout(getApplicationContext());
                layout_data_row.setPadding(10, 10, 10, 10);
                layout_data_row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout_data_row.setOrientation(VERTICAL);

                LinearLayout layout_data_row_su = new LinearLayout(getApplicationContext());
                layout_data_row_su.setPadding(10, 10, 10, 10);
                layout_data_row_su.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout_data_row_su.setOrientation(HORIZONTAL);

                TextView txt_su = new TextView(getApplicationContext());
                txt_su.setText(p_su);
                txt_su.setTextColor(Color.parseColor("#ffffff"));
                TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                txt_su.setLayoutParams(layoutParams);
                txt_su.setPadding(10, 10, 10, 10);

                TextView txt_desc = new TextView(getApplicationContext());
                txt_desc.setText(p_prod_desc);
                txt_desc.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = 200;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 3;
                txt_desc.setLayoutParams(layoutParams);
                txt_desc.setPadding(10, 10, 10, 10);

                TextView txt_conv = new TextView(getApplicationContext());
                txt_conv.setText(p_conv);
                txt_conv.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                txt_conv.setLayoutParams(layoutParams);
                txt_conv.setPadding(10, 10, 10, 10);

                TextView txt_linked_su = new TextView(getApplicationContext());
                txt_linked_su.setText(p_linked_su);
                txt_linked_su.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                txt_linked_su.setLayoutParams(layoutParams);
                txt_linked_su.setPadding(10, 10, 10, 10);

                TextView txt_soh = new TextView(getApplicationContext());
                txt_soh.setText(p_soh);
                txt_soh.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                txt_soh.setLayoutParams(layoutParams);
                //txt_soh.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_conv_sm));
                //txt_soh.setTextColor(Color.parseColor("#ED194A"));
                txt_soh.setPadding(10, 10, 10, 10);

                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setImageResource(R.drawable.barcode_icon);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(50, 50));
                imageView.setPadding(10, 10, 10, 10);
                //layout_data_row.addView(imageView);
                layout_data_row_su.addView(txt_su);
                layout_data_row_su.addView(txt_desc);
                layout_data_row_su.addView(txt_conv);
                layout_data_row_su.addView(txt_soh);
                layout_data_row_su.addView(txt_linked_su);

                layout_data_row.addView(layout_data_row_su);

                //WH STOCK

                LinearLayout layout_wh_stk = new LinearLayout(getApplicationContext());
                layout_wh_stk.setPadding(10, 0, 10, 10);
                layout_wh_stk.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout_wh_stk.setOrientation(HORIZONTAL);

                TextView txt_wh1 = new TextView(getApplicationContext());
                txt_wh1.setText("WH 1\n"+p_wh1_soh);
                txt_wh1.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                txt_wh1.setLayoutParams(layoutParams);
                txt_wh1.setGravity(Gravity.CENTER_HORIZONTAL);
                txt_wh1.setPadding(10, 10, 10, 10);

                TextView txt_wh2 = new TextView(getApplicationContext());
                txt_wh2.setText("WH 2\n"+p_wh2_soh);
                txt_wh2.setTextColor(Color.parseColor("#ffffff"));
                layoutParams = new TableLayout.LayoutParams();
                layoutParams.width = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.height = TableLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.weight = 1;
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                txt_wh2.setLayoutParams(layoutParams);
                txt_wh2.setGravity(Gravity.CENTER_HORIZONTAL);
                txt_wh2.setPadding(10, 10, 10, 10);

                layout_wh_stk.addView(txt_wh1);
                layout_wh_stk.addView(txt_wh2);*/

                //ENDS WH STOCK

/*
                layout_data_row.addView(layout_wh_stk);
                // }
                layout_data_container.addView(layout_data_row);
                layout_data_row_su.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_card_su));
                layout_wh_stk.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_card_wh));
                //layout_data_container.addView(txt);
                layout_su_data.addView(layout_data_container);
                //crossfade();*/
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}