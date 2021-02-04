package com.grand.imanpro;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.w3c.dom.Text;

import java.util.Calendar;

import static android.database.sqlite.SQLiteDatabase.findEditTable;
import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;
import static android.widget.Toast.LENGTH_SHORT;

public class InventoryNew extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {
    SQLiteDatabase db = null;
    public String user = "", loc = "", mode = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    public String offline_login="N";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_new);

        LinearLayout layout_terminal=(LinearLayout)findViewById(R.id.layout_terminal);
        LinearLayout layout_shelf=(LinearLayout)findViewById(R.id.layout_shelf);
        LinearLayout layout_scan_mode=(LinearLayout)findViewById(R.id.layout_scan_mode);
        LinearLayout layout_inv_id=(LinearLayout)findViewById(R.id.layout_inv_id);
        Intent intent = getIntent();
        //user = intent.getStringExtra("user");
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                loc = cursor.getString(cursor.getColumnIndex("loc"));
                String terminal_id=cursor.getString(cursor.getColumnIndex("terminal_id"));
                EditText edt_new_site=(EditText) findViewById(R.id.txt_new_site);
                edt_new_site.setText(loc);
                EditText edt=(EditText) findViewById(R.id.txt_new_terminal);
                edt.setText(terminal_id);
                String ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
            }
        }
        cursor = db.rawQuery("select id from current_user", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            user=cursor.getString(cursor.getColumnIndex("id"));
        }
        EditText edit;
        edit = (EditText) findViewById(R.id.txt_new_inv_no);
        edit.setText(intent.getStringExtra("inv_no"));
        offline_login=intent.getStringExtra("offline_login");
        TextView textView = (TextView) findViewById(R.id.lbl_header);
        mode = intent.getStringExtra("mode");
        if (mode.equals("pending")) {
            edit = (EditText) findViewById(R.id.txt_new_inv_name);
            edit.setEnabled(false);
            edit.setText(intent.getStringExtra("inv_name"));
            edit = (EditText) findViewById(R.id.txt_new_date);
            edit.setEnabled(false);
            edit.setText(intent.getStringExtra("inv_date"));
            textView.setText("Pending Inventory");
            layout_terminal.setVisibility(View.GONE);
            layout_shelf.setVisibility(View.GONE);
            layout_scan_mode.setVisibility(View.GONE);
            layout_inv_id.setVisibility(View.VISIBLE);
            closeKeyboard();
        } else if (mode.equals("new")) {
            edit = (EditText) findViewById(R.id.txt_new_inv_name);
            edit.requestFocus();
            edit.setEnabled(true);
            edit = (EditText) findViewById(R.id.txt_new_date);
            edit.setEnabled(true);
            textView.setText("New Inventory");
            layout_terminal.setVisibility(View.GONE);
            layout_shelf.setVisibility(View.GONE);
            layout_scan_mode.setVisibility(View.GONE);
            layout_inv_id.setVisibility(View.VISIBLE);
        } else if (mode.equals("offline")) {
            textView.setText("Offline Inventory");
            if (intent.getStringExtra("inv_name").length() > 0) {
                edit = (EditText) findViewById(R.id.txt_new_inv_name);
                edit.setEnabled(false);
                edit.setText(intent.getStringExtra("inv_name"));
                edit = (EditText) findViewById(R.id.txt_new_date);
                edit.setEnabled(false);
                edit.setText(intent.getStringExtra("inv_date"));
                edit = (EditText) findViewById(R.id.txt_new_terminal);
                edit.requestFocus();
                Button bt = (Button) findViewById(R.id.btn_next);
                bt.requestFocus();
                layout_terminal.setVisibility(View.VISIBLE);
                layout_shelf.setVisibility(View.VISIBLE);
                layout_scan_mode.setVisibility(View.VISIBLE);
                closeKeyboard();
            } else {
                edit = (EditText) findViewById(R.id.txt_new_inv_name);
                edit.setEnabled(true);
                edit.setText(intent.getStringExtra("inv_name"));
                edit.requestFocus();
                edit = (EditText) findViewById(R.id.txt_new_date);
                edit.setEnabled(true);
                edit.setText(intent.getStringExtra("inv_date"));
            }
            layout_inv_id.setVisibility(View.GONE);
        }
    }
    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }
    public void showDateDialog(View view) {
        showDatePicker();
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, this,
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        EditText edt = (EditText) findViewById(R.id.txt_new_date);
        String mon = "";
        switch (month) {
            case 0: {
                mon = "Jan";
                break;
            }
            case 1: {
                mon = "Feb";
                break;
            }
            case 2: {
                mon = "Mar";
                break;
            }
            case 3: {
                mon = "Apr";
                break;
            }
            case 4: {
                mon = "May";
                break;
            }
            case 5: {
                mon = "Jun";
                break;
            }
            case 6: {
                mon = "Jul";
                break;
            }
            case 7: {
                mon = "Aug";
                break;
            }
            case 8: {
                mon = "Sep";
                break;
            }
            case 9: {
                mon = "Oct";
                break;
            }
            case 10: {
                mon = "Nov";
                break;
            }
            case 11: {
                mon = "Dec";
                break;
            }
        }
        edt.setText(dayOfMonth + "-" + mon + "-" + year);
    }

    public void newNextPressed(View view) {
        System.out.println(mode);
        if (mode.equals("new")) {
            EditText edt = (EditText) findViewById(R.id.txt_new_inv_name);
            String inv_name = edt.getText().toString();
            edt = (EditText) findViewById(R.id.txt_new_date);
            String inv_date = edt.getText().toString();
            if (inv_name.equals(""))
                Toast.makeText(this, "Please Enter Inventory Name", LENGTH_SHORT).show();
            else {
                edt = (EditText) findViewById(R.id.txt_new_inv_no);
                String inv_no = edt.getText().toString();

                edt = (EditText) findViewById(R.id.txt_new_date);
                OPERATION_NAME = "insertInvHeader";
                new MyTask().execute(inv_no, inv_name, edt.getText().toString());
            }
        }
        if (mode.equals("pending")) {
            EditText edt = (EditText) findViewById(R.id.txt_new_inv_no);
            String inv_no = edt.getText().toString();
            edt = (EditText) findViewById(R.id.txt_new_inv_name);
            String inv_name = edt.getText().toString();
            edt = (EditText) findViewById(R.id.txt_new_date);
            String inv_date = edt.getText().toString();

            Intent intent = new Intent(getApplicationContext(), InventoryScan.class);
            intent.putExtra("inv_no", inv_no);
            intent.putExtra("inv_name", inv_name);
            intent.putExtra("user", getUser());
            intent.putExtra("inv_date", inv_date);
            intent.putExtra("mode", "pending");
            //Toast.makeText(getApplicationContext(), inv_date, LENGTH_SHORT).show();
            startActivity(intent);
        }
        if (mode.equals("offline")) {
            String inv_no = "";
            EditText edt_inv_name = (EditText) findViewById(R.id.txt_new_inv_name);
            String inv_name = edt_inv_name.getText().toString();
            EditText edt_date = (EditText) findViewById(R.id.txt_new_date);
            String inv_date = edt_date.getText().toString();
            EditText edt_terminal = (EditText) findViewById(R.id.txt_new_terminal);
            EditText edt_shelf = (EditText) findViewById(R.id.txt_new_shelf);
            String terminal = edt_terminal.getText().toString();
            String shelf = edt_shelf.getText().toString();
            String scan_mode="";
            RadioButton radio=(RadioButton)findViewById(R.id.rd_scan_only);
            if(radio.isChecked()==true)
                scan_mode="SO";
            else
                scan_mode="SQ";
            if(inv_date.length()<=0||inv_name.length()<=0||terminal.length()<=0||shelf.length()<=0)
                Toast.makeText(this,"Please Enter Inventory Name, Date, Terminal ID and Shelf ID",LENGTH_SHORT).show();
            else {
                Intent intent = new Intent(getApplicationContext(), InventoryScan.class);
                intent.putExtra("inv_no", inv_no);
                intent.putExtra("inv_name", inv_name);
                intent.putExtra("user", getUser());
                intent.putExtra("inv_date", inv_date);
                intent.putExtra("terminal", terminal);
                intent.putExtra("shelf", shelf);
                intent.putExtra("mode", "offline");
                intent.putExtra("scan_mode", scan_mode);
                intent.putExtra("offline_login", offline_login);
                db.execSQL("INSERT INTO Inventory_Master VALUES('" + inv_name + "','" + inv_date + "')");
                edt_inv_name.setEnabled(false);
                edt_date.setEnabled(false);
                startActivity(intent);
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
            if (OPERATION_NAME.equals("insertInvHeader")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("loc");
                pi.setType(String.class);
                pi.setValue(loc);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("inv_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("inv_name");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("inv_date");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(user);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("mac_address");
                pi.setType(String.class);
                pi.setValue(new Ops().getMacAddress());
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
            System.out.println(response.toString());
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            if (OPERATION_NAME.equals("insertInvHeader")) {
                if (result.contains("success")) {
                    EditText edt = (EditText) findViewById(R.id.txt_new_inv_no);
                    String inv_no = edt.getText().toString();
                    edt = (EditText) findViewById(R.id.txt_new_inv_name);
                    String inv_name = edt.getText().toString();
                    edt = (EditText) findViewById(R.id.txt_new_date);
                    String inv_date = edt.getText().toString();

                    Intent intent = new Intent(getApplicationContext(), InventoryScan.class);
                    intent.putExtra("inv_no", inv_no);
                    intent.putExtra("inv_name", inv_name);
                    intent.putExtra("user", getUser());
                    intent.putExtra("mode", "new");
                    intent.putExtra("inv_date", inv_date);
                    //Toast.makeText(getApplicationContext(), inv_date, LENGTH_SHORT).show();
                    startActivity(intent);
                } else
                    Toast.makeText(getApplicationContext(), "Server Error: " + result, LENGTH_SHORT).show();
            }
            ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        final Intent intent_login = new Intent(this, LoginActivity.class);
        final Intent intent = new Intent(this, Inventory.class);

            if (mode.equals("offline")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Confirm Exit");

                // Set up the input
                final EditText input = new EditText(this);
                final EditText input1 = new EditText(this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                input1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setMessage("Exit Offline Mode?");
                // Set up the buttons
                builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!isNetworkAvailable())
                        {
                            startActivity(intent_login);
                        }
                        else if(offline_login.equals("Y"))
                        {
                            startActivity(intent_login);
                        }
                        else {
                            startActivity(intent);
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
            } else {
                startActivity(intent);
            }
    }
    public void newCancelPressed(View view) {
        Intent intent = new Intent(this, Inventory.class);
        startActivity(intent);
    }
    public String getUser() {
        String user = "";
        SQLiteDatabase db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from current_user", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                user = cursor.getString(cursor.getColumnIndex("id"));
            }
        }
        return user;
    }
}
