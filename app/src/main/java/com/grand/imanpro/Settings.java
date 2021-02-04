package com.grand.imanpro;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.util.ArrayList;

import static android.database.sqlite.SQLiteDatabase.findEditTable;
import static android.database.sqlite.SQLiteDatabase.openOrCreateDatabase;
import static android.widget.Toast.LENGTH_SHORT;

public class Settings extends AppCompatActivity {
    SQLiteDatabase db = null;
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public String loc="",user="";
    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                String terminal_id = cursor.getString(cursor.getColumnIndex("terminal_id"));
                String upload_mode = cursor.getString(cursor.getColumnIndex("upload_mode"));
                String vodafone_server = cursor.getString(cursor.getColumnIndex("vodafone_server"));
                Spinner spinner=(Spinner)findViewById(R.id.spinner_upload_modes);
                if(upload_mode.length()<=0||upload_mode.equals("QR Code"))
                    spinner.setSelection(0);
                else
                    spinner.setSelection(1);
                EditText edt=(EditText)findViewById(R.id.txt_terminal_id);
                edt.setText(terminal_id);
                edt=(EditText)findViewById(R.id.txt_vodafone_server);
                edt.setText(vodafone_server);
                String ip=ips[0];
                edt=(EditText)findViewById(R.id.txt_serverip);
                edt.setText(cursor.getString(cursor.getColumnIndex("ip")));
                /*edt=(EditText)findViewById(R.id.txt_loc);
                edt.setText(loc);*/
                loc=cursor.getString(cursor.getColumnIndex("loc"));
                if(loc==null)
                    loc="";
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
            }
        }
        cursor = db.rawQuery("select id from current_user", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            user=cursor.getString(cursor.getColumnIndex("id"));
        }
        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);
        OPERATION_NAME="getSites";
        new MyTask().execute();
    }
    public void savePressed(View view){
        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        db.execSQL("delete from server_ip");
        EditText edt=(EditText)findViewById(R.id.txt_serverip);
        String ip=edt.getText().toString();
        edt=(EditText)findViewById(R.id.txt_vodafone_server);
        String vodafone_server=edt.getText().toString();
        edt=(EditText)findViewById(R.id.txt_terminal_id);
        String terminal_id=edt.getText().toString();
        //edt=(EditText)findViewById(R.id.txt_loc);
        Spinner spinner=(Spinner)findViewById(R.id.spinner_settings_loc);
        String loc_name=spinner.getSelectedItem().toString();
        spinner=(Spinner)findViewById(R.id.spinner_upload_modes);
        String upload_mode=spinner.getSelectedItem().toString();
        if(loc_name.equals("  --Choose Site--")||ip.length()<=0||terminal_id.length()<=0) {
            Toast.makeText(this, "Please Enter All Fields!", Toast.LENGTH_SHORT).show();
        }
        else {
            String loc = loc_name.split("-")[0].trim();
            db.execSQL("insert into server_ip (ip,loc,terminal_id,upload_mode,vodafone_server) values('" + ip + "','" + loc + "','"+terminal_id+"','"+upload_mode+"','"+vodafone_server+"')");
            Toast toast = Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }
    private class MyTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }
        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

            PropertyInfo pi = new PropertyInfo();
            pi.setName("site_code");
            pi.setType(String.class);
            pi.setValue("");
            request.addProperty(pi);

            pi = new PropertyInfo();
            pi.setName("user_id");
            pi.setType(String.class);
            pi.setValue(user);
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
            System.out.println(result);
            if(result.toUpperCase().contains("ERROR"))
            {
                Toast.makeText(getApplicationContext(),result,LENGTH_SHORT).show();
            }
            else
            {
                System.out.println(result);
                String selected_loc="";
                JSONObject json = null;
                try {
                    ArrayList<String> arrayList = new ArrayList<>();
                    arrayList.add("  --Choose Site--");
                    json = new JSONObject(result);
                    //LOAD LOCATIONS
                    JSONArray leaders = json.getJSONArray("GOLD_LOC_LNK");
                    Integer j = 0;
                    for (int i = 0; i <= leaders.length() - 1; i++) {
                        j++;
                        JSONObject jsonas = leaders.getJSONObject(i);
                        String site_name = jsonas.getString("LOC_CODE")+" - "+jsonas.getString("LOC_NAME");
                        if(loc.equals(jsonas.getString("LOC_CODE")))
                            selected_loc=site_name;
                        System.out.println(loc+" - "+jsonas.getString("LOC_CODE"));
                        arrayList.add(site_name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_item, arrayList);
                    adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
                    Spinner spinner = (Spinner) findViewById(R.id.spinner_settings_loc);
                    spinner.setAdapter(adapter);
                    System.out.println("Selected Loc: "+selected_loc);
                    if(selected_loc.length()>0)
                        spinner.setSelection(adapter.getPosition(selected_loc));
                    else
                        spinner.setSelection(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            dialog.dismiss();
        }
    }
}
