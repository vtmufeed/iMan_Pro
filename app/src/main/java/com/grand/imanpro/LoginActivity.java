package com.grand.imanpro;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.renderscript.ScriptGroup;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.text.SimpleDateFormat;
import java.util.Date;

import static android.widget.Toast.*;

public class LoginActivity extends AppCompatActivity {
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    SQLiteDatabase db = null;

    public interface DrawableClickListener {

        public static enum DrawablePosition {TOP, BOTTOM, LEFT, RIGHT}

        ;

        public void onClick(DrawablePosition target);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        System.out.println(new Ops().getMacAddress());

        EditText edt_user = (EditText) findViewById(R.id.txt_user);
        final EditText edt_pass = (EditText) findViewById(R.id.txt_pass);
        String mac_address = new Ops().getMacAddress().toUpperCase();
        if (mac_address.equals("0:10:20:8c:32:2a".toUpperCase()) || mac_address.equals("2c:fd:ab:ae:27:e2".toUpperCase())) {
            edt_user.setText("MUFEED");
            edt_pass.setText("muf123");
        } else {
            edt_user.setText("");
            edt_pass.setText("");
        }

        /*edt_pass.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (edt_pass.getRight() - edt_pass.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        System.out.println(edt_pass.getInputType());
                        if(edt_pass.getInputType()==129)
                            edt_pass.setInputType(97);
                        else
                            edt_pass.setInputType(129);
                        return true;
                    }
                }
                return false;
            }
        });*/
        edt_user.setEnabled(true);
        edt_pass.setEnabled(true);
        edt_user = (EditText) findViewById(R.id.txt_user);
        edt_user.requestFocus();
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS server_ip(ip varchar (20),loc varchar(10),loc_name varchar(100),terminal_id varchar(10),upload_mode varchar(50),vodafone_server varchar(70))");
        db.execSQL("CREATE TABLE IF NOT EXISTS current_user(id varchar (30),name varchar (60),type varcahr2(1),invent_type varcahr2(1), offline integer)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Inventory(slno NUMERIC, barcode varchar2(200), gold_code VARCHAR(10),SU VARCHAR(10),P_DESC VARCHAR(50),SYS_QTY VARCHAR(10),PHY_QTY VARCHAR(10),VAR_QTY VARCHAR(10),VAR_VAL VARCHAR(100))");
        db.execSQL("CREATE TABLE IF NOT EXISTS Transfer(slno NUMERIC, TRD_BARCODE varchar(20),TRD_ART_CODE varchar(6),TRD_STOCK_SU varchar2(2),TRD_DESCRIPTION varchar(50),TRD_BARCODE_CONV varchar(3),TRD_QTY varchar2(20),TRD_UNIT_COST varchar2(10),TRD_AMOUNT varchar(20))");
        db.execSQL("CREATE TABLE IF NOT EXISTS RecentMailIds(slno INTEGER PRIMARY KEY AUTOINCREMENT, MAIL_ID varchar(100))");
        db.execSQL("CREATE TABLE IF NOT EXISTS Inventory_Art_Master(BARCODE VARCHAR2(25),GOLD_CODE VARCAHR2(10),SU VARCHAR2(3),STOCK_SU VARCHAR2(3),PROD_DESC VARCHAR(50),RSP VARCHAR2(10),CONV VARCAHR2(10),UNIT_COST VARCAHR2(10),STOCK_UNIT VARCHAR2(20))");
        db.execSQL("CREATE TABLE IF NOT EXISTS Inventory_Master(INV_NAME VARCHAR2(50),DATE VARCHAR2(50))");
        db.execSQL("CREATE TABLE IF NOT EXISTS Inventory_Physical_Stock(BARCODE VARCHAR2(25) NOT NULL,GOLD_CODE VARCHAR2(10) NOT NULL,STOCK_SU VARCHAR2(3) NOT NULL,PROD_DESC VARCAHR2(50) NOT NULL,UNIT_COST VARCHAR2(10) NOT NULL,PHY_STOCK REAL NOT NULL,CONV VARCHAR2(10) NOT NULL,RSP VARCHAR2(10),DATE VARCHAR2(100),TERMINAL VARCHAR2(10),SHELF VARCHAR2(30),SLNO INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Wh_Order_Detl(ORDER_NO VARCHAR2(50),ORDER_DATE VARCHAR2(20),BARCODE VARCHAR2(20),GOLD_CODE VARCHAR2(20),LV VARCHAR2(3),ORD_UNIT VARCHAR2(10),PROD_DESC VARCHAR2(50),ORDER_QTY INTEGER,PREP_QTY INTEGER,PICK_QTY INTEGER,STOCK_UNIT VARCHAR2(20),CONV INTEGER,ORDER_PACK INTEGER,PO_LINE INTEGER,SOH INTEGER,QTY_IN_ORDER INTEGER,SLNO INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Wh_Order_Bar_Detl(GOLD_CODE VARCHAR2(6),SU VARCHAR2(10),BARCODE VARCHAR2(20))");
        db.execSQL("CREATE TABLE IF NOT EXISTS Excel_Barcodes(BARCODE VARCHAR2(20))");
        db.execSQL("CREATE TABLE IF NOT EXISTS Inventory_Art_Master_Head(LST_DT VARCAHR2(50),CNT INTEGER,SITE INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS Wh_Reception_Report(WRD_SLNO INTEGER PRIMARY KEY AUTOINCREMENT, WRD_BARCODE VARCHAR2(20),WRD_GOLD_CODE VARCHAR2(10),WRD_SU VARCHAR2(4),WRD_PRICE_UNIT VARCHAR2(20),WRD_DESCRIPTION VARCHAR2(50),WRD_ORDER_QTY VARCAHR2(10),WRD_SHIPPED_QTY VARCAHR2(10),WRD_RECEIVED_QTY VARCAHR2(10) )");
        db.execSQL("CREATE TABLE IF NOT EXISTS Return_Request(SLNO INTEGER PRIMARY KEY AUTOINCREMENT,BARCODE VARCHAR2(20),ART_CODE VARCHAR2(10),ART_SU VARCHAR2(3),ART_DESC VARCHAR2(50),SUPP_CODE VARCHAR2(15),CC VARCHAR2(15),CC_DESC VARCHAR2(100),QTY VARCHAR2(25))");
        edt_user.setEnabled(true);
        edt_pass.setEnabled(true);
        if (!isNetworkAvailable()) {
            makeText(this, "Network is Not Available", LENGTH_SHORT).show();
            //OPERATION_NAME = "loadUserID";
            //new MyTask().execute();
        }
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(0xFF063844, PorterDuff.Mode.SRC_ATOP);
        /*OPERATION_NAME = "loadUserID";
        new MyTask().execute();*/
        closeKeyboard();
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void settingsPressed(View view) {
        Context context = getApplicationContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Add a TextView here for the "Title" label, as noted in the comments
        final EditText titleBox = new EditText(context);
        titleBox.setHint("Enter Url");
        titleBox.setText("");
        titleBox.setTextColor(Color.parseColor("#000000"));
        titleBox.setMaxLines(1);
        titleBox.setInputType(InputType.TYPE_CLASS_TEXT);
        titleBox.setHintTextColor(Color.parseColor("#a6a6a6"));
        layout.addView(titleBox); // Notice this is an add method
        final EditText edt_terminal = new EditText(context);
        edt_terminal.setHint("Terminal ID");
        edt_terminal.setHintTextColor(Color.parseColor("#a6a6a6"));
        edt_terminal.setText("");
        edt_terminal.setTextColor(Color.parseColor("#000000"));
        edt_terminal.setMaxLines(1);
        edt_terminal.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(edt_terminal);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        int flag = 0;
        if (cursor.getCount() > 0) {
            flag = 1;
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                String ip = ips[0];
                String terminal_id =cursor.getString(cursor.getColumnIndex("terminal_id"));
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
                titleBox.setText(cursor.getString(cursor.getColumnIndex("ip")));
                edt_terminal.setText(terminal_id);
            }
        }
        cursor.close();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Server IP Address");

        // Set up the input
        final EditText input = new EditText(this);
        final EditText input1 = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = titleBox.getText().toString();
                String terminal_id = edt_terminal.getText().toString();
                if(m_Text.length()>0 && terminal_id.length()>0) {
                    //Toast.makeText(getApplicationContext(),m_Text,LENGTH_SHORT).show();
                    Cursor cursor = db.rawQuery("select * from server_ip", null);
                    int flag = 0;
                    if (cursor.getCount() > 0)
                        db.execSQL("update server_ip set ip='" + m_Text + "',terminal_id=" + terminal_id + "");
                    else
                        db.execSQL("insert into server_ip(ip,terminal_id,upload_mode) values('" + m_Text + "'," + terminal_id + ",'QR Code')");
                    cursor.close();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Please Enter All Fields",LENGTH_SHORT).show();
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

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public void loginPressed(View view) {
        EditText edt = (EditText) findViewById(R.id.txt_user);
        String p_user = edt.getText().toString().toUpperCase();
        if (p_user.equals("VODAFONE")) {
            Intent intent = new Intent(getApplicationContext(), BarcodeGen.class);
            startActivity(intent);
        }
        else if (p_user.equals("OFFLINE")) {
            Cursor cursor = db.rawQuery("select * from server_ip", null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                String terminal_id = cursor.getString(cursor.getColumnIndex("terminal_id"));
                if(terminal_id.length()>0) {
                    gotoInventoryNew(p_user);
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Terminal ID Not Found",LENGTH_SHORT).show();
                    cursor.close();
                }
            }
            else
            {
                Toast.makeText(getApplicationContext(),"Terminal ID Not Found",LENGTH_SHORT).show();
                cursor.close();
            }
        } else {
            edt = (EditText) findViewById(R.id.txt_pass);
            String p_pass = edt.getText().toString();
            if (p_user.equals("") || p_user.isEmpty() || p_pass.equals("") || p_pass.isEmpty())
                makeText(this, "Please Enter User Name and Password", LENGTH_SHORT).show();
            else {
                Cursor cursor = db.rawQuery("select * from server_ip", null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                        String terminal_id = cursor.getString(cursor.getColumnIndex("terminal_id"));
                        if(terminal_id.length()>0) {
                            String ip = ips[0];
                            cursor.close();
                            SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
                            OPERATION_NAME = "appLogin";
                            cursor.close();
                            new MyTask().execute(p_user, p_pass);
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(),"Terminal ID Not Found",LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Toast toast = makeText(getApplicationContext(), "IP Not Found !!!  Please Contact Your System Administrator !!", LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                }
                cursor.close();
            }
        }
    }

    public void gotoInventoryNew(String user) {
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            String loc = cursor.getString(cursor.getColumnIndex("loc"));
            if (loc != null) {
                String inv_date = "";
                String inv_name = "";

                cursor = db.rawQuery("SELECT INV_NAME,DATE FROM Inventory_Master", null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    inv_name = cursor.getString(0);
                    inv_date = cursor.getString(1);
                }
                db.execSQL("update current_user set offline=1");
                Intent intent = new Intent(getApplicationContext(), InventoryNew.class);
                intent.putExtra("mode", "offline");
                intent.putExtra("inv_name", inv_name);
                intent.putExtra("inv_date", inv_date);
                intent.putExtra("user", "OFFLINE");
                intent.putExtra("offline_login", "Y");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Location is not Set! Please contact system administrator", LENGTH_SHORT).show();
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
            super.onPreExecute();
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("loadUserID")) {
                String user_id = "";
                Cursor cursor = db.rawQuery("select * from current_user", null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    user_id = cursor.getString(cursor.getColumnIndex("id"));
                }
                response = user_id;
            }
            if (OPERATION_NAME.equals("appLogin")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
                String user = params[0];
                String pass = params[1];

                PropertyInfo pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(user);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("pass");
                pi.setType(String.class);
                pi.setValue(pass);
                request.addProperty(pi);

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
                Date now = new Date();
                String strDate = sdf.format(now);

                pi = new PropertyInfo();
                pi.setName("datetime");
                pi.setType(String.class);
                pi.setValue(strDate);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
                        SoapEnvelope.VER11);
                envelope.dotNet = true;

                envelope.setOutputSoapObject(request);

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS,15000);
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
            System.out.println(result);
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
            if (OPERATION_NAME.equals("loadUserID")) {
                EditText edt_user = (EditText) findViewById(R.id.txt_user);
                edt_user.setText(result);
                EditText edt_pass = (EditText) findViewById(R.id.txt_pass);
                if (result == null)
                    edt_user.requestFocus();
                else if (result.length() == 0)
                    edt_user.requestFocus();
                else
                    edt_pass.requestFocus();
            }
            if (OPERATION_NAME.equals("appLogin")) {
                if (result.contains("successlogin")) {
                    EditText edt = (EditText) findViewById(R.id.txt_pass);
                    String pass = edt.getText().toString();
                    edt = (EditText) findViewById(R.id.txt_user);
                    db.execSQL("delete from current_user");
                    db.execSQL("insert into current_user values ('" + edt.getText().toString() + "','" + result.split(",")[1] + "','" + result.split(",")[2] + "','" + result.split(",")[3] + "',0)");
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("user", edt.getText().toString());
                    startActivity(intent);
                    edt = (EditText) findViewById(R.id.txt_pass);
                    // edt.setText("");
                } else if (result.contains("authfailed"))
                    makeText(getApplicationContext(), "Authentication Failed.!", LENGTH_SHORT).show();
                else
                    makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
    }
}
