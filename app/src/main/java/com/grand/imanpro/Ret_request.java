package com.grand.imanpro;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextPaint;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.widget.Toast.LENGTH_SHORT;

public class Ret_request extends AppCompatActivity {
    SQLiteDatabase db = null;
    public String user = "", loc = "", mail_id = "";
    public int ok_to_save = 0;
    Double unit_cost = 0.0;
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public String p_stock_unit, inv_date, terminal;
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    ProgressDialog dialog;
    TextView txt_exist;

    FloatingActionButton fab_plus, fab_report, fab_mail, fab_post;
    TextView fab_label_report, fab_label_mail, fab_label_post;
    Animation FabOpen, FabClose, FabClockWise, FabAnitClockWise;
    boolean isOpen = false;
    LinearLayout layout_main;

    Button btn_clear, btn_save, btn_plus, btn_minus;
    EditText txt_phy_qty, txt_barcode;
    String global_po_no;
    Double global_rcvd_qty;
    String global_price_unit;
    String supplier_data;
    EditText edt_doc_no;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ret_request);

        layout_main = (LinearLayout) findViewById(R.id.layout_main);
        edt_doc_no=(EditText)findViewById(R.id.txt_doc_no);
        //blurView = (BlurView)findViewById(R.id.blurlayout);
        //blurBg();
        //Floating Buttons and It's Animations
        fab_plus = (FloatingActionButton) findViewById(R.id.btn_fab_plus);
        fab_report = (FloatingActionButton) findViewById(R.id.btn_fab_report);
        fab_mail = (FloatingActionButton) findViewById(R.id.btn_fab_mail);
        fab_post = (FloatingActionButton) findViewById(R.id.btn_fab_post);

        btn_clear = (Button) findViewById(R.id.btn_clear);
        btn_save = (Button) findViewById(R.id.btn_save);
        btn_plus = (Button) findViewById(R.id.btn_plus);
        btn_minus = (Button) findViewById(R.id.btn_minus);
        txt_phy_qty = (EditText) findViewById(R.id.txt_scan_phy_pack);
        txt_barcode = (EditText) findViewById(R.id.txt_scan_barcode);

        fab_label_report = (TextView) findViewById(R.id.label_fab_report);
        fab_label_mail = (TextView) findViewById(R.id.label_fab_mail);
        fab_label_post = (TextView) findViewById(R.id.label_fab_post);

        FabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        FabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        FabClockWise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_clockwise);
        FabAnitClockWise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_anticlockwise);

        fab_plus.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (isOpen) {
                                                closeFab();
                                            } else {
                                                openFab();
                                            }
                                        }
                                    }
        );
        //FAB Ends Here
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        txt_exist = (TextView) findViewById(R.id.txt_exist_qty);
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

        final EditText edittext = (EditText) findViewById(R.id.txt_scan_barcode);
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (!edittext.getText().toString().isEmpty()) {
                        OPERATION_NAME = "retReqGetProdDetl";
                        String barcode = edittext.getText().toString();
                        new MyTask().execute(barcode, loc);
                    }
                    return true;
                }
                return false;
            }
        });
        Intent intent = getIntent();


        LinearLayout btn_upload = (LinearLayout) findViewById(R.id.btn_upload);
        LinearLayout btn_mail = (LinearLayout) findViewById(R.id.btn_mail_inv);
        LinearLayout btn_valid = (LinearLayout) findViewById(R.id.btn_valid_inv);

        fab_mail.setEnabled(true);

        user = getUser();
        EditText edt = (EditText) findViewById(R.id.txt_scan_barcode);
        edt.requestFocus();
        OPERATION_NAME = "retReqGetDocNo";
        new MyTask().execute();
    }

    public void chooseSupplier(View view) throws JSONException {
        try {
            Context context = getApplicationContext();
            LinearLayout layout = new LinearLayout(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.setLayoutParams(layoutParams);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(5, 5, 5, 5);

            JSONObject object = new JSONObject(supplier_data);
            JSONArray supplier = object.getJSONArray("Table1");
            ScrollView scrollView = new ScrollView(this);
            LinearLayout.LayoutParams divider_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 3);
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose Supplier");
            builder.setView(layout);
            builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            final AlertDialog ad = builder.create();
            for (int i = 0; i < supplier.length(); i++) {
                JSONObject row = supplier.getJSONObject(i);
                final String supplier_code = row.getString("SUPPLIER_CODE");
                final String cc = row.getString("CC_NO");
                final String supplier_name = row.getString("CC_DESC");

                final LinearLayout layout_card = new LinearLayout(this);
                layout_card.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams layout_card_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layout_card.setLayoutParams(layout_card_params);
                layout_card.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_supp_selection));
                layout_card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText edt = (EditText) findViewById(R.id.txt_supplier);
                        edt.setText(supplier_code + " - " + cc + " - " + supplier_name);
                        ad.dismiss();
                    }
                });

                /*SUPPLIER CODE*/
                TextView txt_lbl_supp_code = new TextView(this);
                txt_lbl_supp_code.setText("Supplier Code");
                txt_lbl_supp_code.setTextColor(Color.parseColor("#DCDCDC"));
                txt_lbl_supp_code.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                TextView txt_supp_code = new TextView(this);
                txt_supp_code.setText(supplier_code);
                txt_supp_code.setTextColor(Color.parseColor("#ffffff"));
                txt_supp_code.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                txt_supp_code.setGravity(Gravity.RIGHT);

                LinearLayout layout_supp_code = new LinearLayout(this);
                layout_supp_code.setPadding(pxFromDp(this, 5), pxFromDp(this, 5), pxFromDp(this, 5), pxFromDp(this, 5));
                LinearLayout.LayoutParams layout_supp_code_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                //layout_supp_code_params.setMargins(0,pxFromDp(this,10),0,0);
                layout_supp_code.setLayoutParams(layout_supp_code_params);
                layout_supp_code.setOrientation(LinearLayout.HORIZONTAL);

                layout_supp_code.addView(txt_lbl_supp_code);
                layout_supp_code.addView(txt_supp_code);
                /*SUPPLIER CODE ENDS HERE*/
                /*SUPPLIER CC*/
                TextView txt_lbl_supp_cc = new TextView(this);
                txt_lbl_supp_cc.setText("CC");
                txt_lbl_supp_cc.setTextColor(Color.parseColor("#DCDCDC"));
                txt_lbl_supp_cc.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));

                TextView txt_supp_cc = new TextView(this);
                txt_supp_cc.setText(cc);
                txt_supp_cc.setTextColor(Color.parseColor("#ffffff"));
                txt_supp_cc.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                txt_supp_cc.setGravity(Gravity.RIGHT);

                LinearLayout layout_supp_cc = new LinearLayout(this);
                LinearLayout.LayoutParams layout_supp_cc_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layout_supp_cc.setPadding(pxFromDp(this, 5), pxFromDp(this, 5), pxFromDp(this, 5), pxFromDp(this, 5));
                layout_supp_cc.setLayoutParams(layout_supp_cc_params);
                layout_supp_cc.setOrientation(LinearLayout.HORIZONTAL);

                layout_supp_cc.addView(txt_lbl_supp_cc);
                layout_supp_cc.addView(txt_supp_cc);
                /*SUPPLIER CC ENDS HERE*/

                /*SUPPLIER NAME*/
                TextView txt_lbl_supp_name = new TextView(this);
                txt_lbl_supp_name.setText("Name");
                txt_lbl_supp_name.setTextColor(Color.parseColor("#DCDCDC"));
                txt_lbl_supp_name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 2));

                TextView txt_supp_name = new TextView(this);
                txt_supp_name.setText(supplier_name);
                txt_supp_name.setTextColor(Color.parseColor("#ffffff"));
                txt_supp_name.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 3));
                txt_supp_name.setGravity(Gravity.RIGHT);

                LinearLayout layout_supp_name = new LinearLayout(this);
                layout_supp_name.setPadding(pxFromDp(this, 5), pxFromDp(this, 5), pxFromDp(this, 5), pxFromDp(this, 5));
                LinearLayout.LayoutParams layout_supp_name_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layout_supp_name.setLayoutParams(layout_supp_name_params);
                layout_supp_name.setOrientation(LinearLayout.HORIZONTAL);

                layout_supp_name.addView(txt_lbl_supp_name);
                layout_supp_name.addView(txt_supp_name);
                /*SUPPLIER NAME ENDS HERE*/

                LinearLayout layout_divider = new LinearLayout(new ContextThemeWrapper(getApplicationContext(), R.style.Divider_Horizontal2));
                layout_divider.setLayoutParams(divider_params);

                //layout_card.addView(layout_divider);
                layout_card.addView(layout_supp_code);
                layout_card.addView(layout_supp_cc);
                layout_card.addView(layout_supp_name);

                layout.addView(layout_card);
            }
            scrollView.addView(layout);

            ad.setView(scrollView);
            ad.show();
            // Set up the buttons
        /*builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });*/
            //final AlertDialog aad = builder.show();

            //builder.show();
        } catch (Exception ex) {
            Toast.makeText(this, "No Data Found", LENGTH_SHORT).show();
        }
    }

    public static Integer pxFromDp(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public void openFab() {
        fab_report.startAnimation(FabOpen);
        fab_plus.startAnimation(FabClockWise);

        fab_label_report.startAnimation(FabOpen);

        fab_report.setClickable(true);

        btn_clear.setEnabled(false);
        btn_save.setEnabled(false);
        btn_plus.setEnabled(false);
        btn_minus.setEnabled(false);
        txt_phy_qty.setEnabled(false);
        txt_barcode.setEnabled(false);

        layout_main.setAlpha(0.0f);
        layout_main.setVisibility(View.GONE);

        isOpen = true;
    }

    public void closeFab() {
        fab_report.startAnimation(FabClose);
        fab_plus.startAnimation(FabAnitClockWise);

        fab_label_report.startAnimation(FabClose);

        fab_report.setClickable(false);

        btn_clear.setEnabled(true);
        btn_save.setEnabled(true);
        btn_plus.setEnabled(true);
        btn_minus.setEnabled(true);
        txt_phy_qty.setEnabled(true);
        txt_barcode.setEnabled(true);

        layout_main.setAlpha(1f);
        layout_main.setEnabled(true);
        layout_main.setVisibility(View.VISIBLE);
        isOpen = false;
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

    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            dialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("retReqInsertion")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("site");
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
                pi.setName("supp_code");
                pi.setType(String.class);
                pi.setValue(params[5]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("cc");
                pi.setType(String.class);
                pi.setValue(params[6]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("cc_desc");
                pi.setType(String.class);
                pi.setValue(params[7]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("qty");
                pi.setType(String.class);
                pi.setValue(params[8]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(params[9]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("doc_no");
                pi.setType(String.class);
                pi.setValue(params[10]);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
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
            if (OPERATION_NAME.equals("retReqGetDocNo")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
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
            if (OPERATION_NAME.equals("retReqGetProdDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("barcode");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("site_id");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
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
            System.out.println(result);
            if (OPERATION_NAME.equals("retReqGetProdDetl")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    try {
                        final JSONObject json = new JSONObject(result);
                        EditText edt = (EditText) findViewById(R.id.txt_prodcode);
                        edt.setText(json.getString("gold_code"));
                        edt = (EditText) findViewById(R.id.txt_su);
                        edt.setText(json.getString("su"));
                        edt = (EditText) findViewById(R.id.txt_desc);
                        edt.setText(json.getString("su_desc"));
                        supplier_data = json.getString("supplier");
                    } catch (JSONException e) {
                        Toast.makeText(getApplicationContext(), e.getMessage().toString(), LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("retReqInsertion")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    clearAll(null);
                    Toast.makeText(getApplicationContext(), "Saved!", LENGTH_SHORT).show();
                } else {
                    clearAll(null);
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            EditText edt=(EditText)findViewById(R.id.txt_scan_barcode);
            if (OPERATION_NAME.equals("retReqGetDocNo")) {
                if (result.toUpperCase().contains("Error")) {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                    edt.setEnabled(false);
                    edt_doc_no.setText("");
                } else {
                    edt_doc_no.setText(result);
                    edt.setEnabled(true);
                }
            }
            dialog.dismiss();
            closeKeyboard();
        }
    }

    public void reportPressed(View view) {
        Intent intent = new Intent(this, RetReqReport.class);
        intent.putExtra("doc_no", edt_doc_no.getText().toString());
        closeFab();
        EditText edt = (EditText) findViewById(R.id.txt_scan_barcode);
        edt.requestFocus();
        startActivity(intent);
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void addPressed(View view) {
        setStock("+");
    }

    public void minusPressed(View view) {
        setStock("-");
    }

    public void setStock(String sign) {
        String format = "#0.000";
        if (global_price_unit != null) {
            if (global_price_unit.toUpperCase().contains("KILO"))
                format = "#0.000";
            else
                format = "#0";
        }
        NumberFormat formatter = new DecimalFormat(format);
        Double stock = 0.0;
        EditText edt = (EditText) findViewById(R.id.txt_scan_phy_pack);
        String phy = edt.getText().toString();
        if (phy.trim().length() <= 0)
            stock = 0.0;
        else
            stock = Double.parseDouble(phy);
        switch (sign) {
            case "+": {
                stock++;
                break;
            }
            case "-": {
                stock--;
                break;
            }
        }
        edt.setText(formatter.format(stock));
    }

    public void validatePressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Validate Reception");
        builder.setMessage("Are you sure to Validate this Reception?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                OPERATION_NAME = "validateWhRec";
                new MyTask().execute(global_po_no);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public void clearAll(View view) {
        EditText edt = (EditText) findViewById(R.id.txt_prodcode);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_su);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_desc);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_supplier);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_phy_pack);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_barcode);
        edt.setText("");
        edt.requestFocus();
        ok_to_save = 0;
    }
    public void savePressed(View view) {
        EditText edt = (EditText) findViewById(R.id.txt_scan_barcode);
        String barcode = edt.getText().toString();
        TextView txt = (EditText) findViewById(R.id.txt_prodcode);
        String gold_code = txt.getText().toString();
        txt = (EditText) findViewById(R.id.txt_su);
        String su = txt.getText().toString();
        txt = (EditText) findViewById(R.id.txt_desc);
        String desc = txt.getText().toString();
        txt = (EditText) findViewById(R.id.txt_supplier);
        txt = (EditText) findViewById(R.id.txt_supplier);
        String supp = txt.getText().toString();
        edt = (EditText) findViewById(R.id.txt_scan_phy_pack);
        String qty = edt.getText().toString();
        String doc_no = edt_doc_no.getText().toString();

        if (doc_no!=null && barcode != null && gold_code != null && su != null && desc != null && supp != null && qty != null) {
            if (barcode.length() > 0 && gold_code.length() > 0 && su.length() > 0 && desc.length() > 0 && supp.length() > 0 && qty.length() > 0) {
                OPERATION_NAME = "retReqInsertion";
                new MyTask().execute(loc, barcode, gold_code, su, desc, supp.split("-")[0].trim(), supp.split("-")[1].trim(), supp.split("-")[2].trim(), qty, user,doc_no);
            } else {
                Toast.makeText(this, "Please make sure that all required fields are not empty", LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please make sure that all required fields are not empty", LENGTH_SHORT).show();
        }
    }

    public void mailPressed(View view) {
        Context context = getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(layoutParams);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 10, 10, 10);

        final EditText mailBox = new EditText(context);
        mailBox.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
        mailBox.setHint("Mail ID");
        mailBox.setText("");
        mailBox.setTextColor(Color.parseColor("#000000"));

// Add a TextView here for the "Title" label, as noted in the comments
        final RadioGroup radioGroup = new RadioGroup(this);
        final RadioButton button_other = new RadioButton(this);
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
                final String mail_id = cursor.getString(cursor.getColumnIndex("MAIL_ID"));
                final RadioButton radioButton = new RadioButton(this);
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
                    OPERATION_NAME = "sendWhRcvMail";
                    new MyTask().execute(global_po_no, mail_id);
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