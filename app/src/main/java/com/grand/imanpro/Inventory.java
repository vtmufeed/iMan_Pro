package com.grand.imanpro;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.Switch;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.FileReader;

public class Inventory extends AppCompatActivity {
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "", user = "", loc = "";
    SQLiteDatabase db = null;
    public Button btn_new;
    LinearLayout layout_sections;
    LinearLayout layout;
    ScrollView scrollView;
    CheckBox checkbox_all = null;
    String master_filename;
    String ip;
    ProgressDialog dialog;
    String invent_type;
    MediaPlayer player;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        checkbox_all = new CheckBox(this);
        Context context = getApplicationContext();
        layout = new LinearLayout(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(layoutParams);
        //layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(10, 10, 10, 10);
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
        scrollView.removeAllViews();
        scrollView.addView(layout);

        btn_new = (Button) findViewById(R.id.btn_online);
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                loc = cursor.getString(cursor.getColumnIndex("loc"));
                ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
            }
        }
        btn_new.setEnabled(true);
        user = getUser();
        Button bt_online=(Button)findViewById(R.id.btn_online);
        Button bt_offline=(Button)findViewById(R.id.btn_offline);
        switch(invent_type)
        {
            case "0":
            {
                bt_online.setVisibility(View.VISIBLE);
                bt_offline.setVisibility(View.VISIBLE);
                break;
            }
            case "1":
            {
                bt_online.setVisibility(View.VISIBLE);
                bt_offline.setVisibility(View.GONE);
                break;
            }
            case "2":
            {
                bt_online.setVisibility(View.GONE);
                bt_offline.setVisibility(View.VISIBLE);
                break;
            }
        }
        master_filename = getExternalFilesDir(null).toString() + "/MASTER.CSV";
        //Toast.makeText(this,user,LENGTH_SHORT).show();
    }
    public String getUser() {
        String user_id = "", user_name = "", user_type = "";
        SQLiteDatabase db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from current_user", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                user_id = cursor.getString(cursor.getColumnIndex("id"));
                invent_type = cursor.getString(cursor.getColumnIndex("invent_type"));
                System.out.println("Invent Type: "+invent_type);
            }
        }
        return user_id;
    }
    public void checkboxAllEvent(final String origin) {
        checkbox_all.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //Toast.makeText(getApplicationContext(),"Checked",LENGTH_SHORT).show();
                    if (layout != null) {
                        for (int x = 0; x < layout.getChildCount(); x++) {
                            View viewChild1 = layout.getChildAt(x);
                            Class classChild1 = viewChild1.getClass();
                            if (classChild1 == CheckBox.class) {
                                CheckBox cb = (CheckBox) viewChild1;
                                cb.setChecked(true);
                            }
                        }
                    }
                } else {
                    if (layout != null) {
                        if (origin.equals("OWN")) {
                            for (int x = 0; x < layout.getChildCount(); x++) {
                                View viewChild1 = layout.getChildAt(x);
                                Class classChild1 = viewChild1.getClass();
                                if (classChild1 == CheckBox.class) {
                                    CheckBox cb = (CheckBox) viewChild1;
                                    cb.setChecked(false);
                                }
                            }
                        }
                    }
                }
            }
        });
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
            if(OPERATION_NAME.equals("genOfflineMaster"))
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("genOfflineMaster")) {
                publishProgress("Generating Master... Please Wait.");
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("site");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("cat");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;

                envelope.setOutputSoapObject(request);

                HttpTransportSE httpTransport = new HttpTransportSE(SOAP_ADDRESS,60000);
                System.out.println(SOAP_ADDRESS);

                try {
                    httpTransport.call(SOAP_ACTION, envelope);
                    response = envelope.getResponse();
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            if (OPERATION_NAME.equals("getSections")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
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
            if (OPERATION_NAME.equals("getPendInvNo")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("mac");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(loc);
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
            if (OPERATION_NAME.equals("getNextInvNo")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);
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
            System.out.println(response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
            dialog.setMessage(value[0]);
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            if (OPERATION_NAME.equals("genOfflineMaster")) {
                if (result.split(",")[0].equals("success")) {
                    String datetime=result.split(",")[2];
                    new DownloadFileFromURL().execute("http://" + ip + "/iManWebService/master/MASTER_" + result.split(",")[1] + ".csv",datetime);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
            if (OPERATION_NAME.equals("getPendInvNo")) {
                if (result.equals("No pending inv")) {
                    OPERATION_NAME = "getNextInvNo";
                    new MyTask().execute();
                } else if (result.toUpperCase().contains("ERROR")) {
                    makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                } else if (result.toUpperCase().contains("SUCCESS")) {
                    String[] res = result.split(",");
                    Intent intent = new Intent(getApplicationContext(), InventoryNew.class);
                    intent.putExtra("mode", "pending");
                    intent.putExtra("inv_no", res[1]);
                    intent.putExtra("inv_name", res[2]);
                    intent.putExtra("inv_date", res[3]);
                    intent.putExtra("user", user);
                    intent.putExtra("offline_login", "N");
                    startActivity(intent);
                }
            }
            if (OPERATION_NAME.equals("getNextInvNo")) {
                Intent intent = new Intent(getApplicationContext(), InventoryNew.class);
                intent.putExtra("mode", "new");
                intent.putExtra("inv_no", result);
                intent.putExtra("user", user);
                intent.putExtra("offline_login", "N");
                startActivity(intent);
            }
            if (OPERATION_NAME.equals("getSections")) {
                //System.out.println(result);
                try {
                    JSONObject object = new JSONObject(result);
                    JSONArray array = object.getJSONArray("Table1");
                    JSONObject row;
                    String section_name, section_code;
                    CheckBox checkBox;
                    ColorStateList colorStateList = new ColorStateList(
                            new int[][]{
                                    new int[]{-android.R.attr.state_checked}, // unchecked
                                    new int[]{android.R.attr.state_checked}  // checked
                            },
                            new int[]{
                                    Color.parseColor("#000000"),
                                    Color.parseColor("#000000")
                            }
                    );
                    layout.removeAllViews();
                    checkbox_all.setText("ALL");
                    checkbox_all.setTextColor(Color.parseColor("#000000"));
                    checkboxAllEvent("OWN");

                    if (Build.VERSION.SDK_INT < 21) {
                        CompoundButtonCompat.setButtonTintList(checkbox_all, colorStateList);//Use android.support.v4.widget.CompoundButtonCompat when necessary else
                    } else {
                        checkbox_all.setButtonTintList(colorStateList);//setButtonTintList is accessible directly on API>19
                    }
                    layout.addView(checkbox_all);
                    for (int i = 0; i < array.length(); i++) {
                        row = array.getJSONObject(i);
                        section_name = row.getString("NAME");
                        section_code = row.getString("CODE");
                        final CheckBox checkbox = new CheckBox(getApplicationContext());
                        checkbox.setHighlightColor(Color.parseColor("#4287f5"));
                        checkbox.setText(section_code + " - " + section_name);
                        //checkbox.setChecked(true);
                        checkbox.setTextColor(Color.parseColor("#000000"));
                        if (Build.VERSION.SDK_INT < 21) {
                            CompoundButtonCompat.setButtonTintList(checkbox, colorStateList);//Use android.support.v4.widget.CompoundButtonCompat when necessary else
                        } else {
                            checkbox.setButtonTintList(colorStateList);//setButtonTintList is accessible directly on API>19
                        }
                        layout.addView(checkbox);
                        System.out.println(section_name);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Button btn = (Button) findViewById(R.id.btn_choose_sec);
                btn.setText("Tap here to choose sections");
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layout_donwload_mst);
                linearLayout.setVisibility(View.VISIBLE);
            }
            dialog.dismiss();
        }
    }
    public static Integer pxFromDp(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    public void offlineClicked(View view) {
        if (loc != null) {
            if (loc.length() > 0) {
                Cursor cursor = db.rawQuery("select count(*) from Inventory_Art_Master", null);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int count = Integer.parseInt(cursor.getString(0));
                    if (count != 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(Inventory.this);
                        String last_download_date="Not Available";
                        Integer last_download_count=0;
                        Cursor cursor1=db.rawQuery("select *from Inventory_Art_Master_Head",null);
                        if(cursor1.getCount()>0)
                        {
                            cursor1.moveToFirst();
                            last_download_date=cursor1.getString(0);
                            last_download_count=Integer.parseInt(cursor1.getString(1));
                        }
                        cursor1.close();
                        LinearLayout layout=new LinearLayout(this);
                        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        layout.setOrientation(LinearLayout.VERTICAL);
                        layout.setLayoutParams(params);

                        TextView txt_lbl_date=new TextView(this);
                        params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(pxFromDp(getApplicationContext(),30),pxFromDp(getApplicationContext(),15),0,0);
                        txt_lbl_date.setLayoutParams(params);
                        txt_lbl_date.setText("Last Downloaded at");

                        TextView txt_date=new TextView(this);
                        params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(pxFromDp(getApplicationContext(),40),0,0,0);
                        txt_date.setLayoutParams(params);
                        txt_date.setText(last_download_date);
                        txt_date.setTextColor(Color.BLACK);

                        TextView txt_lbl_cnt=new TextView(this);
                        params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(pxFromDp(getApplicationContext(),30),0,0,0);
                        txt_lbl_cnt.setLayoutParams(params);
                        txt_lbl_cnt.setText("Item Count");

                        TextView txt_count=new TextView(this);
                        params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(pxFromDp(getApplicationContext(),40),0,0,0);
                        txt_count.setLayoutParams(params);
                        txt_count.setText(String.format("%,d", last_download_count));
                        txt_count.setTextColor(Color.BLACK);

                        TextView txt_message=new TextView(this);
                        params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(pxFromDp(getApplicationContext(),25),pxFromDp(getApplicationContext(),10),pxFromDp(getApplicationContext(),25),0);
                        txt_message.setLayoutParams(params);
                        txt_message.setText("Do You Want To Download New Master?");
                        txt_message.setTextColor(Color.BLACK);
                        txt_message.setTextSize(17);

                        layout.addView(txt_lbl_date);
                        layout.addView(txt_date);
                        layout.addView(txt_lbl_cnt);
                        layout.addView(txt_count);
                        layout.addView(txt_message);

                        builder.setView(layout);
                        builder.setTitle("Download Master");
                        //builder.setMessage("Do You Want To Download New Master?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                OPERATION_NAME = "getSections";
                                new MyTask().execute();
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                gotoInventoryNew();
                            }
                        });
                        builder.show();
                    } else {
                        OPERATION_NAME = "getSections";
                        new MyTask().execute();
                    }
                } else {
                    OPERATION_NAME = "getSections";
                    new MyTask().execute();
                }
                cursor.close();
            } else
                Toast.makeText(this, "Location is Not set!\nPlease Contact System Administrator.", LENGTH_SHORT).show();
        } else
            Toast.makeText(this, "Location is Not set!\nPlease Contact System Administrator.", LENGTH_SHORT).show();
    }
    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }
    private static final int PERMISSION_REQUEST_CODE = 200;

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    downloadPressed(null);
                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
        new AlertDialog.Builder(Inventory.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    public void downloadPressed(View view) {
        if (checkPermission()) {
            {
                String category = "";
                if (layout != null) {
                    if (checkbox_all.isChecked())
                        category = "ALL";
                    else {
                        for (int x = 0; x < layout.getChildCount(); x++) {
                            View viewChild1 = layout.getChildAt(x);
                            Class classChild1 = viewChild1.getClass();
                            if (classChild1 == CheckBox.class) {
                                CheckBox cb = (CheckBox) viewChild1;
                                if (cb.isChecked())
                                    category += "'" + cb.getText().toString().split("-")[0].trim() + "',";
                            }
                        }
                        category = category.substring(0, category.length() - 1);
                    }
                }
                System.out.println(category);
                OPERATION_NAME = "genOfflineMaster";
                new MyTask().execute(category);
                /*new DownloadFileFromURL().execute("http://" + ip + "/iManWebService/master/MASTER.csv");*/
            }
        } else {
            requestPermission();
        }
    }

    private ProgressDialog pdia;

    class DownloadFileFromURL extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdia = new ProgressDialog(Inventory.this);
            pdia.setMax(100);
            pdia.setCancelable(false);
            pdia.setMessage("Downloading... Please Wait.");
            //pdia.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pdia.setIndeterminate(true);
            pdia.setProgress(0);
            pdia.show();
            //showDialog(progress_bar_type);
        }

        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                System.out.println(master_filename);
                OutputStream output = new FileOutputStream(master_filename);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("Downloading " + (int) ((total * 100) / lenghtOfFile) + "%");

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            int i = 0;
            try {
                publishProgress("Deleting Existing Master...");
                db.execSQL("DELETE FROM Inventory_Art_Master");
                FileReader file = new FileReader(master_filename);
                BufferedReader buffer = new BufferedReader(file);
                String line = "";
                String tableName = "Inventory_Art_Master";
                String columns = "BARCODE,GOLD_CODE,SU,STOCK_SU,PROD_DESC,RSP,CONV,UNIT_COST,STOCK_UNIT";
                String str1 = "INSERT INTO " + tableName + " (" + columns + ") values(";
                String str2 = ");";

                db.beginTransaction();
                buffer.readLine();
                while ((line = buffer.readLine()) != null) {
                    i++;
                    StringBuilder sb = new StringBuilder(str1);
                    String[] str = line.split(",");
                    sb.append("'" + str[0] + "',");
                    sb.append("'" + str[1] + "',");
                    sb.append("'" + str[2] + "',");
                    sb.append("'" + str[3] + "',");
                    sb.append("'" + str[4] + "',");
                    sb.append("'" + str[5] + "',");
                    sb.append("'" + str[6] + "',");
                    sb.append("'" + str[7] + "',");
                    sb.append("'" + str[8] + "'");
                    sb.append(str2);
                    db.execSQL(sb.toString());
                    publishProgress(i + " Records Inserted.");
                }
                db.execSQL("DELETE FROM Inventory_Art_Master_Head");
                db.execSQL("Insert into Inventory_Art_Master_Head (LST_DT,CNT) Values ('"+f_url[1]+"',"+i+")");
                db.setTransactionSuccessful();
                db.endTransaction();
            } catch (IOException e) {

            }
            return String.valueOf(i);
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
//            pdia.setProgress(Integer.parseInt(progress[0]));
            pdia.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(final String result) {
            // dismiss the dialog after the file was downloaded
            // dismissDialog(progress_bar_type);
            pdia.dismiss();
            /*Toast.makeText(getApplicationContext(), "Download Completed.", LENGTH_SHORT).show();
            Button btn = (Button) findViewById(R.id.btn_update);
            btn.setEnabled(true);
            btn.setAlpha(1);*/
            //new UpdateMaster().execute();
            pdia.dismiss();
            Toast.makeText(getApplicationContext(), "Update Completed.", LENGTH_SHORT).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(Inventory.this);
            builder.setTitle("Update Completed");
            builder.setMessage(result + " Records Inserted.");
            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    if (result != "0")
                        gotoInventoryNew();
                }
            });
            builder.setCancelable(false);
            builder.show();
            if (player == null)
                player = MediaPlayer.create(getApplicationContext(), R.raw.completed_tone);
            player.start();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void gotoInventoryNew() {
        String inv_date = "";
        String inv_name = "";

        Cursor cursor = db.rawQuery("SELECT INV_NAME,DATE FROM Inventory_Master", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            inv_name = cursor.getString(0);
            inv_date = cursor.getString(1);
        }
        Intent intent = new Intent(getApplicationContext(), InventoryNew.class);
        intent.putExtra("mode", "offline");
        intent.putExtra("inv_name", inv_name);
        intent.putExtra("inv_date", inv_date);
        intent.putExtra("user", user);
        intent.putExtra("offline_login", "N");
        startActivity(intent);
    }

    /*public void updateClicked(View view) {
        new UpdateMaster().execute();
    }*/

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void newInventoryPressed(View view) {
        if (loc != null) {
            if (loc.length() > 0) {
                Button btn = (Button) findViewById(R.id.btn_online);
                btn.setEnabled(false);
                OPERATION_NAME = "getPendInvNo";
                new MyTask().execute(new Ops().getMacAddress());
            } else
                Toast.makeText(this, "Location is Not set!\nPlease Contact System Administrator.", LENGTH_SHORT).show();
        } else
            Toast.makeText(this, "Location is Not set!\nPlease Contact System Administrator.", LENGTH_SHORT).show();
    }

    public void showSections(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Sections To Download");
        if (scrollView.getParent() != null)
            ((ViewGroup) scrollView.getParent()).removeAllViews();
        builder.setView(scrollView);
        builder.setCancelable(false);
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Button btn = (Button) findViewById(R.id.btn_choose_sec);
                String sections = "";
                if (layout != null) {
                    for (int x = 0; x < layout.getChildCount(); x++) {
                        View viewChild1 = layout.getChildAt(x);
                        Class classChild1 = viewChild1.getClass();
                        if (classChild1 == CheckBox.class) {
                            CheckBox cb = (CheckBox) viewChild1;
                            if (cb.isChecked())
                                sections += cb.getText().toString() + ",";
                        }
                    }
                }
                Button btn_download = (Button) (findViewById(R.id.btn_download));
                if (sections.length() > 0) {
                    btn_download.setEnabled(true);
                    btn_download.setAlpha(1);
                    sections = sections.substring(0, sections.length() - 1);
                    if (sections.contains("ALL")) {
                        btn.setText("ALL");
                    } else {
                        String btnText = "";
                        int length = sections.length();
                        if (length > 30)
                            length = 29;
                        if (sections.length() - sections.replace(",", "").length() > 1)
                            btnText = sections.substring(0, length);
                        else
                            btnText = sections.substring(0, length);
                        if (btnText.contains(","))
                            btnText += "...";
                        btn.setText(btnText);
                    }
                } else {
                    btn.setText("Tap here to choose sections");
                    btn_download.setEnabled(false);
                    btn_download.setAlpha(0.5f);
                }
                dialog.cancel();
            }
        });
        builder.show();
    }
}
