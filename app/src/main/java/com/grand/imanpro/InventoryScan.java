package com.grand.imanpro;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.widget.Toast.LENGTH_SHORT;

public class InventoryScan extends AppCompatActivity {
    SQLiteDatabase db = null;
    public String user = "", loc = "", mail_id = "";
    public int ok_to_save = 0;
    Double unit_cost = 0.0;
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public String p_stock_unit, inv_date, terminal, shelf;
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    ProgressDialog dialog;
    String mode, offline_slno, scan_mode,offline_login="";
    TextView txt_exist;
    Calendar c = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    FloatingActionButton fab_plus, fab_report, fab_delete, fab_post, fab_mail, fab_upload;
    TextView fab_label_post, fab_label_delete, fab_label_report, fab_label_mail, fab_label_upload;
    Animation FabOpen, FabClose, FabClockWise, FabAnitClockWise;
    boolean isOpen = false;
    LinearLayout layout_main,layout_ho_stock;

    MediaPlayer player;
    MediaPlayer player_saved;
    Button btn_clear, btn_save, btn_plus, btn_minus;
    EditText txt_phy_qty, txt_barcode;
    String global_head_date = "", global_head_name = "", global_head_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_scan);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        player_saved= MediaPlayer.create(getApplicationContext(), R.raw.saved);

        layout_main = (LinearLayout) findViewById(R.id.layout_main);
        layout_ho_stock=(LinearLayout) findViewById(R.id.layout_ho_stock);

        //blurView = (BlurView)findViewById(R.id.blurlayout);
        //blurBg();
        //Floating Buttons and It's Animations
        fab_plus = (FloatingActionButton) findViewById(R.id.btn_fab_plus);
        fab_report = (FloatingActionButton) findViewById(R.id.btn_fab_report);
        fab_post = (FloatingActionButton) findViewById(R.id.btn_fab_post);
        fab_delete = (FloatingActionButton) findViewById(R.id.btn_fab_delete);
        fab_mail = (FloatingActionButton) findViewById(R.id.btn_fab_mail);
        fab_upload = (FloatingActionButton) findViewById(R.id.btn_fab_upload);

        btn_clear = (Button) findViewById(R.id.btn_clear);
        btn_save = (Button) findViewById(R.id.btn_save);
        btn_plus = (Button) findViewById(R.id.btn_plus);
        btn_minus = (Button) findViewById(R.id.btn_minus);
        txt_phy_qty = (EditText) findViewById(R.id.txt_scan_phy_pack);
        txt_barcode = (EditText) findViewById(R.id.txt_scan_barcode);

        fab_label_post = (TextView) findViewById(R.id.label_fab_post);
        fab_label_delete = (TextView) findViewById(R.id.label_fab_delete);
        fab_label_report = (TextView) findViewById(R.id.label_fab_report);
        fab_label_mail = (TextView) findViewById(R.id.label_fab_mail);
        fab_label_upload = (TextView) findViewById(R.id.label_fab_upload);

        FabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        FabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        FabClockWise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_clockwise);
        FabAnitClockWise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_anticlockwise);

        fab_plus.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {

                }else{
                    EditText edt=(EditText)findViewById(R.id.txt_scan_phy_pack);
                    edt.requestFocus();
                }

            }
        });
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
        /*edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (!edittext.getText().toString().isEmpty()) {
                        if (mode.equals("offline")) {
                            if (scan_mode.equals("SO")) {
                                new saveScanOnly().execute(edittext.getText().toString());
                            } else {
                                OPERATION_NAME = "offGetInvProdDetl";
                                String barcode = edittext.getText().toString();
                                EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                                new MyTask().execute(barcode, edt.getText().toString());
                            }
                        } else {
                            OPERATION_NAME = "olGetInvProdDetl";
                            String barcode = edittext.getText().toString();
                            EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                            new MyTask().execute(barcode, edt.getText().toString());
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),"Please scan a barcode",LENGTH_SHORT).show();
                        edittext.requestFocus();
                    }
                    return true;
                }
                return false;
            }
        });*/

        edittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                {
                    int flag = 0;
                    if (!edittext.getText().toString().isEmpty()) {
                        if (actionId == EditorInfo.IME_ACTION_NEXT) {

                            if (mode.equals("offline")) {
                                if (scan_mode.equals("SO")) {
                                    new saveScanOnly().execute(edittext.getText().toString());
                                } else {
                                    OPERATION_NAME = "offGetInvProdDetl";
                                    String barcode = edittext.getText().toString();
                                    EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                                    new MyTask().execute(barcode, edt.getText().toString());
                                }
                            } else {
                                OPERATION_NAME = "olGetInvProdDetl";
                                String barcode = edittext.getText().toString();
                                EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                                new MyTask().execute(barcode, edt.getText().toString());
                            }

                            EditText Ed_qty = (EditText) findViewById(R.id.txt_scan_phy_pack);
                            Ed_qty.requestFocus();
                            Ed_qty.setCursorVisible(true);
                            /*InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);*/

                        } else {
                            if (mode.equals("offline")) {
                                if (scan_mode.equals("SO")) {
                                    new saveScanOnly().execute(edittext.getText().toString());
                                } else {
                                    OPERATION_NAME = "offGetInvProdDetl";
                                    String barcode = edittext.getText().toString();
                                    EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                                    new MyTask().execute(barcode, edt.getText().toString());
                                }
                            } else {
                                OPERATION_NAME = "olGetInvProdDetl";
                                String barcode = edittext.getText().toString();
                                EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                                new MyTask().execute(barcode, edt.getText().toString());
                            }

                            EditText Ed_qty = (EditText) findViewById(R.id.txt_scan_phy_pack);
                            Ed_qty.requestFocus();
                            //Ed_qty.setCursorVisible(true);
                            /*InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);*/
                        }
                    }
                    handled = true;
                }
                return handled;
            }
        });
        EditText qty=(EditText) findViewById(R.id.txt_scan_phy_pack);
        qty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                boolean handled = false;
                {
                    savePressed(null);
                    handled = true;
                }
                return handled;
            }
        });
        Intent intent = getIntent();
        String inv_no = intent.getStringExtra("inv_no");
        String inv_name = intent.getStringExtra("inv_name");
        mode = intent.getStringExtra("mode");
        scan_mode = intent.getStringExtra("scan_mode");
        terminal = intent.getStringExtra("terminal");
        shelf = intent.getStringExtra("shelf");

        EditText edt_inv_id=(EditText)findViewById(R.id.txt_scan_inv_no);
        String mode_label;
        if (mode.equals("offline")) {
            offline_login= intent.getStringExtra("offline_login");
            edt_inv_id.setVisibility(View.GONE);
            mode_label = "Offline " + scan_mode;
            /*btn_upload.setVisibility(View.VISIBLE);
            btn_mail.setVisibility(View.GONE);
            btn_valid.setVisibility(View.GONE);*/
            fab_upload.setEnabled(true);
            fab_mail.setEnabled(false);
            fab_post.setEnabled(false);
            txt_exist.setVisibility(View.VISIBLE);
            cursor = db.rawQuery("select 'X' from current_user where type='A' and offline=0", null);
            if (cursor.getCount() > 0) {
                fab_delete.setEnabled(true);
            } else {
                fab_delete.setEnabled(false);
            }
            layout_ho_stock.setVisibility(View.GONE);
        } else {
            edt_inv_id.setVisibility(View.VISIBLE);
            mode_label = "Online";
            /*btn_upload.setVisibility(View.GONE);
            btn_mail.setVisibility(View.VISIBLE);
            btn_valid.setVisibility(View.VISIBLE);*/
            fab_upload.setEnabled(false);
            fab_mail.setEnabled(true);
            fab_post.setEnabled(true);
            txt_exist.setVisibility(View.GONE);
            layout_ho_stock.setVisibility(View.VISIBLE);
        }
        TextView textView = (TextView) findViewById(R.id.txt_mode);
        textView.setText(mode_label);
        user = getUser();
        inv_date = intent.getStringExtra("inv_date");
        //Toast.makeText(this, inv_date, LENGTH_SHORT).show();
        EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
        edt.setText(inv_no);
        edt = (EditText) findViewById(R.id.txt_scan_inv_name);
        edt.setText(inv_name);
        edt = (EditText) findViewById(R.id.txt_scan_barcode);
        edt.requestFocus();
    }

    private class saveScanOnly extends AsyncTask<String, String, String> {
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
            Cursor cursor = db.rawQuery("SELECT *FROM Inventory_Art_Master WHERE BARCODE='" + params[0] + "'", null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String phy = "1";
                String gold_code = cursor.getString(cursor.getColumnIndex("GOLD_CODE"));
                String su = cursor.getString(cursor.getColumnIndex("SU"));
                String stock_su = cursor.getString(cursor.getColumnIndex("STOCK_SU"));
                String prod_desc = cursor.getString(cursor.getColumnIndex("PROD_DESC"));
                String rsp = cursor.getString(cursor.getColumnIndex("RSP"));
                String conv = cursor.getString(cursor.getColumnIndex("CONV"));
                String unit_cost = cursor.getString(cursor.getColumnIndex("UNIT_COST"));
                String stock_unit = cursor.getString(cursor.getColumnIndex("STOCK_UNIT"));
                db.execSQL("INSERT INTO Inventory_Physical_Stock (BARCODE,GOLD_CODE,STOCK_SU,PROD_DESC,UNIT_COST,PHY_STOCK,CONV,RSP,DATE,TERMINAL,SHELF) VALUES('" + params[0] + "','" + gold_code + "','" + stock_su + "','" + prod_desc + "','" + unit_cost + "','" + phy + "','" + conv + "','" + rsp + "','" + sdf.format((c.getTime())) + "','" + terminal + "','" + shelf + "')");
                if (player_saved == null)
                    player_saved = MediaPlayer.create(getApplicationContext(), R.raw.saved);
                player_saved.start();
                return "Saved!";
            } else {
                System.out.println("Barcode Not Found: " + params[0]);
                if (player == null)
                    player = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
                player.start();
                return "Barcode Not Found";
            }
        }

        protected void onProgressUpdate(String... value) {

        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            txt_barcode.setText("");
            txt_barcode.requestFocus();
            dialog.dismiss();
        }
    }

    public void openFab() {
        fab_post.startAnimation(FabOpen);
        fab_report.startAnimation(FabOpen);
        fab_delete.startAnimation(FabOpen);
        fab_mail.startAnimation(FabOpen);
        fab_upload.startAnimation(FabOpen);
        fab_plus.startAnimation(FabClockWise);

        fab_label_post.startAnimation(FabOpen);
        fab_label_delete.startAnimation(FabOpen);
        fab_label_report.startAnimation(FabOpen);
        fab_label_mail.startAnimation(FabOpen);
        fab_label_upload.startAnimation(FabOpen);

        fab_post.setClickable(true);
        fab_report.setClickable(true);
        fab_delete.setClickable(true);
        fab_mail.setClickable(true);
        fab_upload.setClickable(true);

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
        fab_post.startAnimation(FabClose);
        fab_report.startAnimation(FabClose);
        fab_delete.startAnimation(FabClose);
        fab_mail.startAnimation(FabClose);
        fab_upload.startAnimation(FabClose);
        fab_plus.startAnimation(FabAnitClockWise);

        fab_label_post.startAnimation(FabClose);
        fab_label_delete.startAnimation(FabClose);
        fab_label_report.startAnimation(FabClose);
        fab_label_mail.startAnimation(FabClose);
        fab_label_upload.startAnimation(FabClose);

        fab_post.setClickable(false);
        fab_report.setClickable(false);
        fab_delete.setClickable(false);
        fab_mail.setClickable(false);
        fab_upload.setClickable(false);

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
        EditText edt=(EditText)findViewById(R.id.txt_scan_barcode);
        edt.requestFocus();
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

    private class TaskgetOfflineInvHead extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

            PropertyInfo pi = new PropertyInfo();
            pi.setName("invh_id");
            pi.setType(String.class);
            pi.setValue(params[0]);
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("loc");
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

            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
            if (result.contains("success")) {
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(result);
                    global_head_date = jsonObject.getString("date");
                    global_head_name = jsonObject.getString("name");
                    global_head_id = jsonObject.getString("id");
                    Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Inventory_Physical_Stock", null);
                    cursor.moveToFirst();
                    Integer count = Integer.parseInt(cursor.getString(0));
                    showUploadDialog(count);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Error: Please contact system administrator\n" + e.getMessage().toString(), LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                global_head_date = "";
                global_head_name = "";
                global_head_id = "";
            }
        }
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
            if(OPERATION_NAME.equals("uploadOfflineData"))
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("deleteInvBarcode")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("inv_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("gold_code");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("su");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("barcode");
                pi.setType(String.class);
                pi.setValue(params[3]);
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
            if (OPERATION_NAME.equals("validInventory")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("inv_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
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
            if (OPERATION_NAME.equals("delInventory")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("inv_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
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
            if (OPERATION_NAME.equals("sendInvMail")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("inv_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mail_id");
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
            if (OPERATION_NAME.equals("offInsertInvProdDetl")) {
                System.out.println(params[4]);
                /*Cursor cursor = db.rawQuery("SELECT PHY_STOCK FROM Inventory_Physical_Stock WHERE BARCODE='" + params[0] + "'", null);
                if (cursor.getCount() == 0) {*/
                db.execSQL("INSERT INTO Inventory_Physical_Stock (BARCODE,GOLD_CODE,STOCK_SU,PROD_DESC,UNIT_COST,PHY_STOCK,CONV,RSP,DATE,TERMINAL,SHELF) VALUES('" + params[0] + "','" + params[1] + "','" + params[2] + "','" + params[3] + "','" + params[4] + "','" + params[5] + "','" + params[6] + "','" + params[7] + "','" + params[8] + "','" + params[9] + "','" + params[10] + "')");
                /*} else
                    db.execSQL("UPDATE Inventory_Physical_Stock SET PHY_STOCK=" + params[5] + " WHERE BARCODE='" + params[0] + "'");*/
                response = "SUCCESS";
            }
            if (OPERATION_NAME.equals("olInsertInvProdDetl")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("inv_no");
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
                pi.setName("stock_val");
                pi.setType(String.class);
                pi.setValue(params[5]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("sale");
                pi.setType(String.class);
                pi.setValue(params[6]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("phy_stock");
                pi.setType(String.class);
                pi.setValue(params[7]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("sys_stock");
                pi.setType(String.class);
                pi.setValue(params[8]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("conv");
                pi.setType(String.class);
                pi.setValue(params[9]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("rsp");
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
            if (OPERATION_NAME.equals("offGetInvProdDetl")) {
                String result = "";
                Cursor cursor = db.rawQuery("SELECT *FROM Inventory_Art_Master WHERE BARCODE='" + params[0] + "'", null);
                if (cursor.getCount() > 0) {
                    String phy = "0";
                    cursor.moveToFirst();
                    String gold_code = cursor.getString(cursor.getColumnIndex("GOLD_CODE"));
                    String su = cursor.getString(cursor.getColumnIndex("SU"));
                    String stock_su = cursor.getString(cursor.getColumnIndex("STOCK_SU"));
                    String prod_desc = cursor.getString(cursor.getColumnIndex("PROD_DESC"));
                    String rsp = cursor.getString(cursor.getColumnIndex("RSP"));
                    String conv = cursor.getString(cursor.getColumnIndex("CONV"));
                    String unit_cost = cursor.getString(cursor.getColumnIndex("UNIT_COST"));
                    String stock_unit = cursor.getString(cursor.getColumnIndex("STOCK_UNIT"));
                    cursor = db.rawQuery("SELECT ifnull(sum(ifnull(PHY_STOCK,0)),0) FROM Inventory_Physical_Stock WHERE BARCODE='" + params[0] + "'", null);
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        phy = cursor.getString(0);
                    }
                    //response = "{\"GOLD_CODE\":\"" + gold_code + "\",\"SU\":\"" + su + "\",\"STOCK_SU\":\"" + stock_su + "\",\"PROD_DESC\":\"" + prod_desc + "\",\"RSP\":\"" + rsp + "\",\"CONV\":\"" + conv + "\",\"UNIT_COST\":\"" + unit_cost + "\",\"STOCK_UNIT\":\"" + stock_unit + "\",\"PHY\":\"" + phy + "\"}";
                    response = "{\"GOLD_CODE\":\"" + gold_code + "\",\"SU\":\"" + su + "\",\"STOCK_SU\":\"" + stock_su + "\",\"PROD_DESC\":\"" + prod_desc + "\",\"RSP\":\"" + rsp + "\",\"CONV\":\"" + conv + "\",\"UNIT_COST\":\"" + unit_cost + "\",\"STOCK_UNIT\":\"" + stock_unit + "\",\"PHY\":\"" + phy + "\"}";
                } else {
                    response = "error";
                }
            }
            if (OPERATION_NAME.equals("olGetInvProdDetl")) {
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

                pi = new PropertyInfo();
                pi.setName("inv_no");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("inv_date");
                pi.setType(String.class);
                pi.setValue(inv_date);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;

                envelope.setOutputSoapObject(request);

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS, 300000);

                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            if (OPERATION_NAME.equals("uploadOfflineData")) {
                String inv_name = "", inv_date = "";
                Cursor cursor_master = db.rawQuery("SELECT *FROM Inventory_Master", null);
                if (cursor_master.getCount() > 0) {
                    cursor_master.moveToFirst();
                    inv_name = cursor_master.getString(cursor_master.getColumnIndex("INV_NAME"));
                    inv_date = cursor_master.getString(cursor_master.getColumnIndex("DATE"));
                    /*db.execSQL("delete from Inventory_Physical_Stock");
                    for(int i=0;i<=25;i++)
                        db.execSQL("INSERT INTO Inventory_Physical_Stock VALUES('TESTTESTTEST-"+i+"','123456','123','EMAMI FAIR AND HANDSOME FAIRNE CREAM FOR MEN 10ML',"+i+","+i+","+i+","+i+",'"+sdf.format((c.getTime()))+"')");*/
                    Cursor cursor_data = db.rawQuery("SELECT *FROM Inventory_Physical_Stock", null);
                    Integer rowcount = cursor_data.getCount();
                    if (rowcount > 0) {
                        cursor_data.moveToFirst();
                        JSONArray resultSet = new JSONArray();
                        for (int i = 0; i < cursor_data.getCount(); i++) {
                            int totalColumn = cursor_data.getColumnCount();
                            JSONObject rowObject = new JSONObject();

                            for (int j = 0; j < totalColumn; j++) {
                                if (cursor_data.getColumnName(j) != null) {
                                    try {
                                        if (cursor_data.getString(j) != null) {
                                            //Log.d("TAG_NAME", cursor.getString(j) );
                                            rowObject.put(cursor_data.getColumnName(j), cursor_data.getString(j));
                                        } else {
                                            rowObject.put(cursor_data.getColumnName(j), "");
                                        }
                                    } catch (Exception e) {
                                        response = "Error Processing Data: Please Contact System Administrator\n" + e.getMessage();
                                    }
                                }
                            }
                            resultSet.put(rowObject);
                            cursor_data.moveToNext();
                        }
                        System.out.println(resultSet);
                        if (response == null)
                            response = "ok";
                        if (!response.toString().toUpperCase().contains("ERROR")) {
                            SOAP_ACTION = "http://tempuri.org/" + "insertOfflineData";
                            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, "insertOfflineData");

                            PropertyInfo pi = new PropertyInfo();
                            pi.setName("inv_name");
                            pi.setType(String.class);
                            pi.setValue(inv_name);
                            request.addProperty(pi);

                            pi = new PropertyInfo();
                            pi.setName("inv_date");
                            pi.setType(String.class);
                            pi.setValue(inv_date);
                            request.addProperty(pi);

                            pi = new PropertyInfo();
                            pi.setName("loc");
                            pi.setType(String.class);
                            pi.setValue(loc);
                            request.addProperty(pi);

                            pi = new PropertyInfo();
                            pi.setName("user_id");
                            pi.setType(String.class);
                            pi.setValue(user);
                            request.addProperty(pi);

                            pi = new PropertyInfo();
                            pi.setName("invh_id");
                            pi.setType(String.class);
                            pi.setValue(global_head_id);
                            request.addProperty(pi);

                            pi = new PropertyInfo();
                            pi.setName("rowcount");
                            pi.setType(Integer.class);
                            pi.setValue(rowcount);
                            request.addProperty(pi);

                            pi = new PropertyInfo();
                            pi.setName("inv_data");
                            pi.setType(String.class);
                            pi.setValue(resultSet.toString().replace("},{}]", "}]"));
                            request.addProperty(pi);

                            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                            envelope.dotNet = true;

                            envelope.setOutputSoapObject(request);

                            HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS, 300000);
                            try {
                                httpTransport.call(SOAP_ACTION, envelope);
                                response = envelope.getResponse();
                            } catch (Exception exception) {
                                response = exception.toString();
                            }
                            if (response.toString().split(",")[0].equals("success")) {
                                File file = new File(getExternalFilesDir(null).toString(), response.toString().split(",")[1] + ".csv");
                                try {
                                    file.createNewFile();
                                    CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                                    Cursor curCSV = db.rawQuery("SELECT * FROM Inventory_Physical_Stock", null);
                                    csvWrite.writeNext(curCSV.getColumnNames());
                                    while (curCSV.moveToNext()) {
                                        //Which column you want to exprort
                                        String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2),
                                                curCSV.getString(3), curCSV.getString(4), curCSV.getString(5),
                                                curCSV.getString(6), curCSV.getString(7), curCSV.getString(8),
                                                curCSV.getString(9), curCSV.getString(10), curCSV.getString(11)};
                                        csvWrite.writeNext(arrStr);
                                    }
                                    csvWrite.close();
                                    curCSV.close();
                                } catch (Exception sqlEx) {
                                    response = "Error Generating csv file";
                                }
                            }
                            System.out.println("server response: " + response);
                        }
                        cursor_data.close();
                    } else {
                        response = "Error: No Data Found";
                    }
                } else
                    response = "Error: No Data Found";
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
            if (OPERATION_NAME.equals("uploadOfflineData")) {
                if (result.split(",")[0].equals("success")) {
                    db.execSQL("delete from Inventory_Master");
                    db.execSQL("delete from Inventory_Physical_stock");
                    db.execSQL("DELETE FROM sqlite_sequence WHERE name='Inventory_Physical_Stock'");
                    offlineSuccessAlert(result);
                    //Toast.makeText(getApplicationContext(), "Transferred Successfully! " + result.split(",")[1], LENGTH_SHORT).show();
                } else
                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (OPERATION_NAME.equals("insertOfflineData")) {
                Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            }
            if (OPERATION_NAME.equals("deleteInvBarcode")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    Toast.makeText(getApplicationContext(), "Deleted!", LENGTH_SHORT).show();
                    clearAll(null);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("validInventory")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    Toast.makeText(getApplicationContext(), "Validated!", LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), Inventory.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("delInventory")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    Toast.makeText(getApplicationContext(), "Deleted!", LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), Inventory.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("sendInvMail")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
                    Cursor cursor = db.rawQuery("select * from RecentMailIds where MAIL_ID='" + mail_id + "'", null);
                    if (cursor.getCount() <= 0) {
                        db.execSQL("INSERT INTO RecentMailIds (MAIL_ID)VALUES('" + mail_id.trim() + "')");
                    }
                    Toast.makeText(getApplicationContext(), "Mail Sent!", LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
            }
            if (OPERATION_NAME.equals("olInsertInvProdDetl")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    Toast.makeText(getApplicationContext(), "Saved!", LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
                clearAll(null);
            }
            if (OPERATION_NAME.equals("offInsertInvProdDetl")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    Toast.makeText(getApplicationContext(), "Saved!", LENGTH_SHORT).show();
                    if (player_saved == null)
                        player_saved = MediaPlayer.create(getApplicationContext(), R.raw.saved);
                    player_saved.start();
                    txt_exist.setVisibility(View.GONE);
                    EditText edt = (EditText) findViewById(R.id.txt_scan_barcode);
                    edt.setText("");
                    edt.requestFocus();
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
                clearAll(null);
            }
            if (OPERATION_NAME.equals("offGetInvProdDetl")) {
                if (result.equals("error")) {
                    if (player == null)
                        player = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
                    player.start();
                    Toast.makeText(getApplicationContext(), "Product Not Found", LENGTH_SHORT).show();
                    clearAll(null);
                } else {
                    try {
                        JSONObject object = new JSONObject(result);
                        EditText edt = (EditText) findViewById(R.id.txt_scan_prodcode);
                        edt.setText(object.getString("GOLD_CODE"));
                        edt = (EditText) findViewById(R.id.txt_scan_su);
                        edt.setText(object.getString("STOCK_SU"));
                        edt = (EditText) findViewById(R.id.txt_scan_desc);
                        edt.setText(object.getString("PROD_DESC"));
                        edt = (EditText) findViewById(R.id.txt_scan_rsp);
                        edt.setText(object.getString("RSP"));
                        edt = (EditText) findViewById(R.id.txt_scan_conv);
                        edt.setText(object.getString("CONV"));
                        edt = (EditText) findViewById(R.id.txt_scan_stock);
                        edt.setText("0");
                        edt = (EditText) findViewById(R.id.txt_scan_sale);
                        edt.setText("0");
                        txt_exist.setVisibility(View.VISIBLE);
                        txt_exist.setText("Existing Qty Entered: " + object.getString("PHY"));
                        unit_cost = 0.0;
                        if (object.getString("UNIT_COST").length() > 0)
                            unit_cost = Double.parseDouble(object.getString("UNIT_COST"));

                        p_stock_unit = object.getString("STOCK_UNIT");

                        edt = (EditText) findViewById(R.id.txt_scan_phy_pack);

                        //edt.requestFocus();
                        if (p_stock_unit.toUpperCase().contains("KILO")) {
                            edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            //edt.setText("0.000");
                        } else {
                            edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                            //edt.setText("0");
                        }
                        edt.setText("");
                        edt.requestFocus();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    EditText edt_qty=(EditText) findViewById(R.id.txt_scan_phy_pack);
                    edt_qty.requestFocus();
                }
                //closeKeyboard();
            }
            if (OPERATION_NAME.equals("olGetInvProdDetl")) {
                if (result.contains("Error: ")) {
                    if (player == null)
                        player = MediaPlayer.create(getApplicationContext(), R.raw.error_sound);
                    player.start();
                    ok_to_save = 0;
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                    clearAll(null);
                } else {
                    //closeKeyboard();
                    String[] array = result.split(",");
                    String p_gold_code = array[0];
                    String p_su = array[1];
                    String p_desc = array[2];
                    String p_rsp = array[3];
                    String p_conv = array[4];
                    String p_sale_qty = array[5];
                    String p_stock_qty = array[6];
                    String p_unit_cost = array[7];
                    p_stock_unit = array[8];
                    String p_phy_entered = array[9];
                    if (p_phy_entered.equals("0"))
                        p_phy_entered = "";
                    System.out.println("phy entered: '" + p_phy_entered + "'");
                    unit_cost = Double.parseDouble(p_unit_cost);

                    EditText edt = (EditText) findViewById(R.id.txt_scan_prodcode);
                    edt.setText(p_gold_code);
                    edt = (EditText) findViewById(R.id.txt_scan_su);
                    edt.setText(p_su);
                    edt = (EditText) findViewById(R.id.txt_scan_desc);
                    edt.setText(p_desc);
                    edt = (EditText) findViewById(R.id.txt_scan_rsp);
                    edt.setText(p_rsp);
                    edt = (EditText) findViewById(R.id.txt_scan_conv);
                    edt.setText(p_conv);
                    edt = (EditText) findViewById(R.id.txt_scan_sale);
                    edt.setText(p_sale_qty);
                    edt = (EditText) findViewById(R.id.txt_scan_stock);
                    edt.setText(p_stock_qty);

                    edt = (EditText) findViewById(R.id.txt_scan_phy_pack);
                    if (p_stock_unit.toUpperCase().contains("KILO")) {
                        edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                        //edt.setText("0.000");
                    } else {
                        edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        //edt.setText("0");
                    }
                    //edt.setText("1");
                    if (p_phy_entered.trim().length() > 0)
                        edt.setText(p_phy_entered);
                    edt.requestFocus();
                    //edt.setText("1");
                }
            }
            dialog.cancel();
        }
    }

    /*Handler handler = new Handler();
    Runnable runnable;
    int delay = 1*1000; //Delay for 15 seconds.  One second = 1000 milliseconds.*/


   /* @Override
    protected void onResume() {
        //start handler as activity become visible

        handler.postDelayed( runnable = new Runnable() {
            public void run() {
                //do something
                System.out.println(getCurrentFocus());
                handler.postDelayed(runnable, delay);
            }
        }, delay);

        super.onResume();
    }*/

// If onPause() is not included the threads will double up when you
// reload the activity

    /*@Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }*/

    /*@Override
    protected void onPause() {

        // hide the keyboard in order to avoid getTextBeforeCursor on inactive InputConnection
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        EditText edt=(EditText)findViewById(R.id.txt_scan_phy_pack);
        inputMethodManager.hideSoftInputFromWindow(edt.getWindowToken(), 0);

        super.onPause();
    }*/
    public void showUploadDialog(Integer count) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload to Server");
        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 0, 10, 0);
        linearLayout.setLayoutParams(params);
        linearLayout.setPadding(10, 10, 10, 10);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setTextSize(15);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setText("Inventory ID");
        textView.setTextColor(Color.parseColor("#5c5c5c"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(25, 25, 25, 0);
        linearLayout.addView(textView);

        textView = new TextView(this);
        textView.setTextSize(20);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setText(global_head_id);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(25, 0, 25, 5);
        linearLayout.addView(textView);

        textView = new TextView(this);
        textView.setTextSize(15);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setText("Inventory Date");
        textView.setTextColor(Color.parseColor("#5c5c5c"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(25, 0, 25, 0);
        linearLayout.addView(textView);

        textView = new TextView(this);
        textView.setTextSize(20);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setText(global_head_date);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(25, 0, 25, 5);
        linearLayout.addView(textView);

        textView = new TextView(this);
        textView.setTextSize(15);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setText("Name");
        textView.setTextColor(Color.parseColor("#5c5c5c"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(25, 10, 25, 0);
        linearLayout.addView(textView);

        textView = new TextView(this);
        textView.setTextSize(20);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setText(global_head_name);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(25, 0, 25, 25);
        linearLayout.addView(textView);

        textView = new TextView(this);
        textView.setTextSize(15);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setText(count + " Data Records are Found.\nAre You Sure to Upload Now?");
        linearLayout.addView(textView);

        builder.setView(linearLayout);
        //builder.setMessage(result.split(",")[2]+" Records Transferred Successfully!\n"+"Upload ID: "+result.split(",")[1]);
        builder.setPositiveButton("UPLOAD", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog1, int id) {
                OPERATION_NAME = "uploadOfflineData";
                new MyTask().execute();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog1, int id) {
                dialog1.cancel();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    public void offlineSuccessAlert(String result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Transfer Successfull");
        LinearLayout linearLayout = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 0, 10, 0);
        linearLayout.setLayoutParams(params);
        linearLayout.setPadding(10, 10, 10, 10);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView textView = new TextView(this);
        textView.setTextSize(15);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setText(result.split(",")[2] + " Records Transferred Successfully!");
        linearLayout.addView(textView);

        textView = new TextView(this);
        textView.setTextSize(25);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setText("Upload ID: " + result.split(",")[1]);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setPadding(25, 25, 25, 25);
        linearLayout.addView(textView);
        builder.setView(linearLayout);
        //builder.setMessage(result.split(",")[2]+" Records Transferred Successfully!\n"+"Upload ID: "+result.split(",")[1]);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog2, int id) {
                if(offline_login.equals("Y"))
                {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent(getApplicationContext(), Inventory.class);
                    startActivity(intent);
                }
            }
        });
        builder.setCancelable(false);
        builder.show();
        MediaPlayer player_completed = MediaPlayer.create(getApplicationContext(), R.raw.completed_tone);
        player_completed.start();
    }

    public void deleteBarcodePressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Barcode");
        builder.setMessage("Are You Sure to Delete?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog2, int id) {
                EditText edt = (EditText) findViewById(R.id.txt_scan_barcode);
                String barcode = edt.getText().toString();
                if (mode.equals("offline")) {
                    txt_exist.setVisibility(View.GONE);
                    db.execSQL("DELETE FROM Inventory_Physical_Stock WHERE BARCODE='" + barcode + "'");
                    Toast.makeText(getApplicationContext(), "Deleted!", LENGTH_SHORT).show();
                    clearAll(null);
                } else {
                    edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                    String inv_no = edt.getText().toString();
                    edt = (EditText) findViewById(R.id.txt_scan_prodcode);
                    String gold_code = edt.getText().toString();
                    edt = (EditText) findViewById(R.id.txt_scan_su);
                    String su = edt.getText().toString();
                    if (barcode.length() > 0) {
                        OPERATION_NAME = "deleteInvBarcode";
                        System.out.println("Delete Online Barcode: " + inv_no + " " + gold_code + " " + barcode);
                        new MyTask().execute(inv_no, gold_code, su, barcode);
                    }
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog2, int id) {
                dialog2.cancel();
            }
        });
        builder.show();
    }

    public void reportPressed(View view) {
        Intent intent = new Intent(this, InventoryReport.class);
        EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
        intent.putExtra("inv_no", edt.getText().toString());
        edt = (EditText) findViewById(R.id.txt_scan_inv_name);
        intent.putExtra("inv_name", edt.getText().toString());
        intent.putExtra("mode", mode);
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
        if (p_stock_unit != null) {
            if (p_stock_unit.toUpperCase().contains("KILO"))
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
        //if(stock>=0)
        edt.setText(formatter.format(stock));
    }

    public void validatePressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Validate Inventory");
        builder.setMessage("Are You Sure to Validate?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog2, int id) {
                EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                OPERATION_NAME = "validInventory";
                new MyTask().execute(edt.getText().toString(), user);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog2, int id) {
                dialog2.cancel();
            }
        });
        builder.show();
    }

    public void deletePressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Inventory");
        builder.setMessage("Are You Sure to Delete?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog2, int id) {
                if (mode.equals("offline")) {
                    db.execSQL("DELETE FROM Inventory_Physical_Stock");
                    db.execSQL("DELETE FROM Inventory_Master");
                    db.execSQL("DELETE FROM sqlite_sequence WHERE name='Inventory_Physical_Stock'");
                    Toast.makeText(getApplicationContext(), "Deleted!", LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), Inventory.class);
                    startActivity(intent);
                } else {
                    EditText edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                    OPERATION_NAME = "delInventory";
                    new MyTask().execute(edt.getText().toString(), user);
                }
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog2, int id) {
                dialog2.cancel();
            }
        });
        builder.show();
    }

    public void uploadPressed(View view) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Inventory_Physical_Stock", null);
        cursor.moveToFirst();
        Integer count = Integer.parseInt(cursor.getString(0));
        if (count <= 0)
            Toast.makeText(this, "No Data Found!", LENGTH_SHORT).show();
        else {
            String upload_mode="QR Code";
            cursor.close();
            cursor = db.rawQuery("SELECT upload_mode FROM server_ip", null);
            if(cursor.getCount()>0)
            {
                cursor.moveToFirst();
                if(cursor.getString(cursor.getColumnIndex("upload_mode"))!=null)
                {
                    if(cursor.getString(cursor.getColumnIndex("upload_mode")).length()>0)
                    {
                        upload_mode=cursor.getString(cursor.getColumnIndex("upload_mode"));
                    }
                }
            }
            if(upload_mode.length()<=0)
                upload_mode="QR Code";
            if(upload_mode.equals("QR Code"))
                openCamera();
            else if(upload_mode.equals("Manual"))
            {
                Context context = getApplicationContext();
                final EditText txt_barcode = new EditText(context);
                txt_barcode.setHint("Inventory ID");
                txt_barcode.setTextColor(Color.parseColor("#000000"));
                txt_barcode.setInputType(InputType.TYPE_CLASS_NUMBER);
                txt_barcode.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 3));
//        txt_barcode.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.form_input_field_pick));

                LinearLayout layout = new LinearLayout(this);
                layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layout.setOrientation(LinearLayout.VERTICAL);

                LinearLayout layout_barcode = new LinearLayout(context);
                layout_barcode.setOrientation(LinearLayout.HORIZONTAL);
                layout_barcode.addView(txt_barcode);

                ImageButton d_btn_clear = new ImageButton(this);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(pxFromDp(getApplicationContext(), 35), ViewGroup.LayoutParams.MATCH_PARENT, 1);
                layoutParams.gravity = Gravity.CENTER;
                layoutParams.leftMargin = 5;
                layoutParams.rightMargin = 5;
                d_btn_clear.setImageResource(R.drawable.pick_icon_clear);
                d_btn_clear.setBackground(null);
                d_btn_clear.setLayoutParams(layoutParams);
                d_btn_clear.setScaleType(ImageView.ScaleType.FIT_CENTER);

                layout_barcode.addView(d_btn_clear);

                layout.addView(layout_barcode);

                LinearLayout layout_btns = new LinearLayout(context);
                layout_btns.setOrientation(LinearLayout.HORIZONTAL);

                Button d_btn_close = new Button(this);
                d_btn_close.setText("Close");
                d_btn_close.setTextColor(Color.parseColor("#ED194A"));
                d_btn_close.setBackground(null);

                Button d_btn_search = new Button(this);
                d_btn_search.setText("Upload");
                d_btn_search.setTextColor(Color.parseColor("#ED194A"));
                d_btn_search.setBackground(null);

                layout_btns.addView(d_btn_close);
                layout_btns.addView(d_btn_search);
                layout.addView(layout_btns);

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Enter Inventory ID");
                builder.setView(layout);

                final AlertDialog ad = builder.show();

                d_btn_clear.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        txt_barcode.setText("");
                        txt_barcode.requestFocus();
                    }
                });
                d_btn_close.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ad.dismiss();
                        if (txt_barcode.getText().toString().length() == 0) {
                            txt_barcode.setText("");
                        }
                    }
                });
                d_btn_search.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (txt_barcode.getText() != null) {
                            String barcode = txt_barcode.getText().toString();
                            if (barcode.length() > 0) {
                                ad.dismiss();
                                txt_barcode.setText("");
                                OPERATION_NAME = "getOfflineInvHead";
                                new TaskgetOfflineInvHead().execute(barcode, loc);
                            } else
                                Toast.makeText(getApplicationContext(), "Please Enter a Barcode", LENGTH_SHORT).show();
                        } else
                            Toast.makeText(getApplicationContext(), "Please Enter a Barcode", LENGTH_SHORT).show();
                    }
                });
            }
            /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Upload to Server");
            builder.setMessage(count + " Data Records are Found. Are You Sure to Upload Now?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    OPERATION_NAME = "uploadOfflineData";
                    new MyTask().execute();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.show();*/
        }
        cursor.close();
    }
    public static Integer pxFromDp(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //retrieve scan result
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        String codeContent;
        if (scanningResult != null) {
            //we have a result
            codeContent = scanningResult.getContents();
            if (codeContent != null) {
                if (codeContent.length() > 0) {
                    OPERATION_NAME = "getOfflineInvHead";
                    new TaskgetOfflineInvHead().execute(codeContent, loc);
                }
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
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

    public void openCamera() {
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
                Toast.makeText(this, "Please give Camera permission in Settings", LENGTH_SHORT).show();
            }
        } else {
            requestPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    openCamera();
                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog2, int which) {
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
        new AlertDialog.Builder(InventoryScan.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private static final int PERMISSION_REQUEST_CODE = 200;

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    public void clearAll(View view) {
        System.out.println("called clear all");
        TextView txt = findViewById(R.id.txt_exist_qty);
        txt.setText("");
        EditText edt = (EditText) findViewById(R.id.txt_scan_su);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_desc);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_rsp);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_conv);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_sale);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_stock);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_phy_pack);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_prodcode);
        edt.setText("");
        edt = (EditText) findViewById(R.id.txt_scan_barcode);
        edt.setText("");
        edt.requestFocus();
        ok_to_save = 0;
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (isOpen) {
            closeFab();
        }
        else {
            if (doubleBackToExitPressedOnce) {
                if (mode.equals("offline")) {
                    super.onBackPressed();
                    return;
                } else {
                    Intent intent = new Intent(this, Inventory.class);
                    startActivity(intent);
                }

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
    }

    public void savePressed(View view) {
        try {
            EditText edt = (EditText) findViewById(R.id.txt_scan_phy_pack);
            EditText edt_phy_stock = (EditText) findViewById(R.id.txt_scan_phy_pack);
            String phy = edt.getText().toString();

            edt = (EditText) findViewById(R.id.txt_scan_barcode);
            String barcode = edt.getText().toString();
            if (phy.trim().length() <= 0 || phy.trim().replace(".","").length() >6) {
                if (phy.trim().length() <= 0)
                    Toast.makeText(this, "Physical Stock Can't be Zero", LENGTH_SHORT).show();
                if (phy.trim().replace(".","").length() >6)
                    Toast.makeText(this, "Invalid Physical Stock", LENGTH_SHORT).show();
                ok_to_save = 0;
            } else {
                Double stock = Double.parseDouble(phy);
                if (stock == 0) {
                    Toast.makeText(this, "Physical Stock Can't be Zero", LENGTH_SHORT).show();
                    ok_to_save = 0;
                } else {
                    if (barcode == null) {
                        ok_to_save = 0;
                        Toast.makeText(this, "Please Scan a Barcode", LENGTH_SHORT).show();
                    } else {
                        if (barcode.length() <= 0) {
                            ok_to_save = 0;
                            Toast.makeText(this, "Please Scan a Barcode", LENGTH_SHORT).show();
                        } else {
                            ok_to_save = 1;
                            edt = (EditText) findViewById(R.id.txt_scan_inv_no);
                            String inv_no = edt.getText().toString();

                            edt = (EditText) findViewById(R.id.txt_scan_prodcode);
                            String gold_code = edt.getText().toString();

                            edt = (EditText) findViewById(R.id.txt_scan_su);
                            String su = edt.getText().toString();

                            edt = (EditText) findViewById(R.id.txt_scan_desc);
                            String desc = edt.getText().toString();

                            edt = (EditText) findViewById(R.id.txt_scan_sale);
                            String sale = edt.getText().toString();

                            edt = (EditText) findViewById(R.id.txt_scan_stock);
                            String sys_stock = edt.getText().toString();

                            edt = (EditText) findViewById(R.id.txt_scan_conv);
                            String conv = edt.getText().toString();

                            edt = (EditText) findViewById(R.id.txt_scan_rsp);
                            String rsp = edt.getText().toString();

                            if (gold_code != null && gold_code.length() > 0) {
                                if (mode.equals("offline")) {
                                    System.out.println(phy);
                                    OPERATION_NAME = "offInsertInvProdDetl";
                                    new MyTask().execute(barcode, gold_code, su, desc, unit_cost.toString(), phy, conv, rsp, sdf.format((c.getTime())), terminal, shelf);
                                } else {
                                    OPERATION_NAME = "olInsertInvProdDetl";
                                    new MyTask().execute(inv_no, barcode, gold_code, su, desc, unit_cost.toString(), sale, phy, sys_stock, conv, rsp);
                                }
                            } else {
                                ok_to_save = 0;
                                Toast.makeText(this, "Please Scan a Barcode", LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
            edt_phy_stock.requestFocus();
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
            public void onClick(DialogInterface dialog2, int which) {
                mail_id = mailBox.getText().toString();
                if (mail_id.trim().length() > 0) {
                    EditText inv_no = (EditText) findViewById(R.id.txt_scan_inv_no);
                    OPERATION_NAME = "sendInvMail";
                    new MyTask().execute(inv_no.getText().toString(), mail_id);
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter a valid mail ID", LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                dialog2.cancel();
            }
        });
        builder.show();
    }
}