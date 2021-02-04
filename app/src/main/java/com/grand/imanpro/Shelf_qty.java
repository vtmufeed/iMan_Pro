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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
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
import android.widget.TextView;
import android.widget.Toast;

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

public class Shelf_qty extends AppCompatActivity {
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
    String mode, offline_slno;
    TextView txt_exist;
    Calendar c = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf_qty);

        layout_main = (LinearLayout) findViewById(R.id.layout_main);

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
                        OPERATION_NAME = "getShfQtyProdDetl";
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
    }

    public void openFab() {
        fab_report.startAnimation(FabOpen);
        fab_mail.startAnimation(FabOpen);
        fab_post.startAnimation(FabOpen);
        fab_plus.startAnimation(FabClockWise);

        fab_label_report.startAnimation(FabOpen);
        fab_label_mail.startAnimation(FabOpen);
        fab_label_post.startAnimation(FabOpen);

        fab_report.setClickable(true);
        fab_mail.setClickable(true);
        fab_post.setClickable(true);

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
        fab_mail.startAnimation(FabClose);
        fab_post.startAnimation(FabClose);
        fab_plus.startAnimation(FabAnitClockWise);

        fab_label_report.startAnimation(FabClose);
        fab_label_mail.startAnimation(FabClose);
        fab_label_post.startAnimation(FabClose);

        fab_report.setClickable(false);
        fab_mail.setClickable(false);
        fab_post.setClickable(false);

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
            if (OPERATION_NAME.equals("getShfQtyProdDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("barcode");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("loc_code");
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
            if (OPERATION_NAME.equals("setShfQty")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc_code");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("prod_code");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("prod_desc");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("su");
                pi.setType(String.class);
                pi.setValue(params[3]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("conv");
                pi.setType(String.class);
                pi.setValue(params[4]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("rsp");
                pi.setType(String.class);
                pi.setValue(params[5]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("shelf_qty");
                pi.setType(String.class);
                pi.setValue(params[6]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("facing");
                pi.setType(String.class);
                pi.setValue(params[7]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("pres_stock");
                pi.setType(String.class);
                pi.setValue(params[8]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(user);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("barcode");
                pi.setType(String.class);
                pi.setValue(params[9]);
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
            if (OPERATION_NAME.equals("setShfQty")) {
                if (result.contains("success")) {
                    Toast.makeText(getApplicationContext(), "Saved Successfully", LENGTH_SHORT).show();
                    clearAll(null);
                } else
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            }
            if (OPERATION_NAME.equals("getShfQtyProdDetl")) {
                if (result.contains("Product Not Found")) {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                } else if (result.contains("Server Error")) {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                } else {
                    String[] data = result.split(",");
                    EditText edt = findViewById(R.id.txt_prodcode);
                    edt.setText(data[0]);
                    edt = findViewById(R.id.txt_su);
                    edt.setText(data[1]);
                    edt = findViewById(R.id.txt_desc);
                    edt.setText(data[2]);
                    edt = findViewById(R.id.txt_rsp);
                    edt.setText(data[3]);
                    edt = findViewById(R.id.txt_conv);
                    edt.setText(data[4]);
                    edt = findViewById(R.id.txt_shelf_capacity);
                    if (data[5].equals("0"))
                        edt.setText("");
                    else
                        edt.setText(data[5]);
                    edt = findViewById(R.id.txt_facing);
                    if (data[6].equals("0"))
                        edt.setText("");
                    else
                        edt.setText(data[6]);
                    edt = findViewById(R.id.txt_pres_stock);
                    if (data[7].equals("0"))
                        edt.setText("");
                    else
                        edt.setText(data[7]);
                }
                closeKeyboard();
            }
            dialog.cancel();
        }
    }

    public void reportPressed(View view) {
        Intent intent = new Intent(this, Wh_rcv_report.class);
        EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
        intent.putExtra("po_no", global_po_no);
        closeFab();
        edt = (EditText) findViewById(R.id.txt_scan_barcode);
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
        edt = (EditText) findViewById(R.id.txt_conv);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_rsp);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_shelf_capacity);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_facing);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_pres_stock);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_barcode);
        edt.setText("");
        edt.requestFocus();
        ok_to_save = 0;
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;

        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click Back again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    public void savePressed(View view) {
        try {
            EditText edt = (EditText) findViewById(R.id.txt_scan_barcode);
            String barcode = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_prodcode);
            String prod_code = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_su);
            String su = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_desc);
            String prod_desc = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_rsp);
            String rsp = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_conv);
            String conv = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_shelf_capacity);
            String shelf_qty = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_facing);
            String facing = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_pres_stock);
            String pres_stock = edt.getText().toString();

            if (barcode == null||shelf_qty == null||facing == null) {
                ok_to_save = 0;
                Toast.makeText(this, "Please Enter Required Fields", LENGTH_SHORT).show();
            } else {
                if (barcode.length() <= 0) {
                    ok_to_save = 0;
                    Toast.makeText(this, "Please Scan a Barcode", LENGTH_SHORT).show();
                } else if (shelf_qty.length() <= 0 || facing.length() <= 0) {
                    ok_to_save = 0;
                    Toast.makeText(this, "Please Enter Required Fields", LENGTH_SHORT).show();
                } else {
                    OPERATION_NAME = "setShfQty";
                    new MyTask().execute(loc, prod_code, prod_desc, su, conv, rsp, shelf_qty, facing, pres_stock, barcode);
                }
            }
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), LENGTH_SHORT).show();
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