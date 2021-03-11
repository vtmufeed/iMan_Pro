package com.grand.imanpro;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.google.zxing.WriterException;
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

import static android.widget.LinearLayout.HORIZONTAL;
import static android.widget.LinearLayout.INVISIBLE;
import static android.widget.LinearLayout.OnClickListener;
import static android.widget.LinearLayout.OnKeyListener;
import static android.widget.LinearLayout.TEXT_ALIGNMENT_CENTER;
import static android.widget.LinearLayout.VERTICAL;
import static android.widget.Toast.LENGTH_SHORT;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Writer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

public class BarcodeGen extends AppCompatActivity {
    SQLiteDatabase db = null;
    public String user = "", loc = "";
    public int ok_to_save = 0;
    Double stock_val = 0.0;
    //public String SOAP_ACTION = "";
    //public String OPERATION_NAME = "";
    public String p_stock_unit, inv_date;
    //public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    //public String SOAP_ADDRESS = "";
    ProgressDialog dialog;
    ProgressDialog dialog_download;
    String code_type = "";
    private ImageView imageViewResult;
    TextView txtv_ret_bar;
    TextView txtv_ret_price;
    String vodafone_item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_gen);
        imageViewResult = findViewById(R.id.imageViewResult);
        dialog_download = new ProgressDialog(this);
        dialog_download.setCancelable(false);
        dialog_download.setInverseBackgroundForced(false);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        txtv_ret_bar = (TextView) findViewById(R.id.txt_ret_bar);
        txtv_ret_price = (TextView) findViewById(R.id.txt_ret_price);

       /* txtv_ret_price.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                imageViewResult.setImageDrawable(null);
                TextView txtv=(TextView)findViewById(R.id.txt_ret_price);
                txtv.setText("");
            }
        });*/

        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                loc = cursor.getString(cursor.getColumnIndex("loc"));
                vodafone_item = cursor.getString(cursor.getColumnIndex("vodafone_server"));
                //SOAP_ADDRESS = vodafone_server + "/vfBarcodeGenerator/Service.asmx";
            }
        }
        /**/
        final EditText edittext = (EditText) findViewById(R.id.txt_price);
        edittext.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                imageViewResult.setImageDrawable(null);
                TextView txtv = (TextView) findViewById(R.id.txt_ret_bar);
                txtv.setText("");
                txtv = (TextView) findViewById(R.id.txt_ret_price);
                txtv.setText("");
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (!edittext.getText().toString().isEmpty()) {
                        if (vodafone_item == null || vodafone_item.length() <= 0 || loc == null || loc.length() <= 0) {
                            Toast.makeText(getApplicationContext(), "Item Code or Site is Not Found", LENGTH_SHORT).show();
                        } else {
                            //OPERATION_NAME = "generateBarcode";
                            String price = edittext.getText().toString();
                            new MyTask().execute(price);
                        }
                        //Toast.makeText(getApplicationContext(),"sdadsad",LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
        clearAll(null);
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
            /*ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);*/
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            String barcode = "";
            String str_price = "";
            // if (OPERATION_NAME.equals("generateBarcode")) {
            Double price = Double.parseDouble(params[0]);
            String doubleAsString = String.format("%.2f", price);
            String p_price=doubleAsString;
            int indexOfDecimal = doubleAsString.indexOf(".");
            String item_code = vodafone_item;
            String prefix = "270";
            String first_part = doubleAsString.substring(0, indexOfDecimal);
            String second_part = doubleAsString.substring(indexOfDecimal).replace(".", "");
            if (first_part.length() == 0)
                first_part = "000";
            if (first_part.length() == 1)
                first_part = "00" + first_part;
            if (first_part.length() == 2)
                first_part = "0" + first_part;
            if (first_part.length() <= 3) {
                String gen_barcode = prefix + item_code + first_part + second_part;
                char[] digits = gen_barcode.toCharArray();
                //gen_barcode = "";
                int multiplier = 3;
                int sum = 0;
                for (int i = 0; i < digits.length; i++) {
                    char ch = digits[i];
                    Integer digit = Integer.parseInt(String.valueOf(ch));
                    int act_multiplier = 0;
                    if (multiplier == 3)
                        act_multiplier = 1;
                    if (multiplier == 1)
                        act_multiplier = 3;
                    sum += digit * act_multiplier;
                    multiplier = act_multiplier;
                }
                int check_digit;
                if (sum % 10 == 0)
                    check_digit = 0;
                else {
                    int temp = sum + 10;
                    Double temp_1 = Double.parseDouble(String.valueOf(temp)) / 10;
                    doubleAsString = String.valueOf(temp_1);
                    indexOfDecimal = doubleAsString.indexOf(".");
                    int temp_2 = Integer.parseInt(doubleAsString.substring(0, indexOfDecimal));
                    int temp_3 = temp_2 * 10;
                    check_digit = temp_3 - sum;
                }
                barcode = gen_barcode + check_digit;
            }
                /*SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("p_price");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("site");
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
                }*/
            // }
            //System.out.println(response.toString());
            return "success," + barcode + "," + p_price;
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            try {
                //  if (OPERATION_NAME.equals("generateBarcode")) {
                if (result.toUpperCase().contains("SUCCESS")) {
                    System.out.println("Result: " + result);
                    String data = result.split(",")[1];
                    Toast.makeText(getApplicationContext(), data, LENGTH_SHORT).show();
                    Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
                    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
                    Writer codeWriter;
                    codeWriter = new Code128Writer();
                    BitMatrix byteMatrix = null;

                    byteMatrix = codeWriter.encode(data, BarcodeFormat.CODE_128, 600, 200, hintMap);

                    int width = byteMatrix.getWidth();
                    int height = byteMatrix.getHeight();
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    for (int i = 0; i < width; i++) {
                        for (int j = 0; j < height; j++) {
                            bitmap.setPixel(i, j, byteMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                        }
                    }
                    //  Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
                    imageViewResult.setImageBitmap(bitmap);
                    txtv_ret_price.setText(result.split(",")[2] + " QR");
                    txtv_ret_bar.setText(data);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
                //  }
            } catch (WriterException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getMessage().toString(), Toast.LENGTH_LONG).show();
                txtv_ret_bar.setText("Error");
                txtv_ret_price.setText("Error");
            }
            dialog.cancel();
        }
    }

    public void clearAll(View view) {
        EditText edt = (EditText) findViewById(R.id.txt_price);
        edt.setText("");
        imageViewResult.setImageDrawable(null);
        TextView txtv = (TextView) findViewById(R.id.txt_ret_bar);
        txtv.setText("");
        txtv = (TextView) findViewById(R.id.txt_ret_price);
        txtv.setText("");
        edt.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static Integer pxFromDp(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}