package com.grand.imanpro;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import org.w3c.dom.Text;

import static android.widget.Toast.LENGTH_SHORT;

public class Wh_pick_items extends AppCompatActivity {
    SQLiteDatabase db = null;
    public String user = "", loc = "", order_no = "";
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "";
    String date_picker_source = "";
    ProgressDialog dialog;
    int offset = 0, rowcount = 0, current_page = 1;
    Double line_per_page = 0.0;
    TextView tv_current_page;
    ImageButton current_save_img;
    String global_gold_code, global_lv, global_pick_qty, global_po_line, search_barcode = "";
    ImageButton btn_search;
    EditText edt_firstRow;
    Integer total_count = 0, entered_count = 0;
    LinearLayout btn_count;
    String sort_method = "slno";
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wh_pick_items);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btn_count = (LinearLayout) findViewById(R.id.btn_count);
        btn_search = (ImageButton) findViewById(R.id.btn_search);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        //Toast.makeText(this, String.valueOf(Math.round(height / (int) (metrics.density * 160f))), LENGTH_SHORT).show();
        line_per_page = 8.0;

        if (Math.round(height / (int) (metrics.density * 160f)) == 4)
            line_per_page = 5.0;
        if (Math.round(height / (int) (metrics.density * 160f)) == 5)
            line_per_page = 6.0;

        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        dialog.setInverseBackgroundForced(false);

        tv_current_page = (TextView) findViewById(R.id.txt_current_page);
        user = getUser();
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

        //Paging
        cursor = db.rawQuery("SELECT count(*) FROM Wh_Order_Detl", null);
        cursor.moveToFirst();
        rowcount = Integer.parseInt(cursor.getString(0));
        cursor.close();
        TextView textView = (TextView) findViewById(R.id.txt_total_pages);
        String total_pages = String.valueOf((int) Math.ceil(rowcount / line_per_page));
        textView.setText(total_pages);


        //order no reception
        Intent intent = getIntent();
        order_no = intent.getStringExtra("order_no");
        String wh = loc;
        String site = intent.getStringExtra("order_site");
        String date = intent.getStringExtra("deliv_date");
        TextView txt_order_no = (TextView) findViewById(R.id.txt_order_no);
        txt_order_no.setText(order_no);
        TextView txt_wh = (TextView) findViewById(R.id.txt_wh);
        txt_wh.setText(wh);
        TextView txt_site = (TextView) findViewById(R.id.txt_site);
        txt_site.setText(site);
        TextView txt_date = (TextView) findViewById(R.id.txt_deliv_date);
        txt_date.setText(date);
        updateCount();

        OPERATION_NAME = "getOrderItems";
        new MyTask().execute("all", "");
    }

    public void updateCount() {
        Cursor c1 = db.rawQuery("select count(*) from Wh_Order_Detl", null);
        c1.moveToFirst();
        total_count = Integer.parseInt(c1.getString(0));

        c1 = db.rawQuery("select count(*) from Wh_Order_Detl where PICK_QTY !='null' and PICK_QTY!=0", null);
        c1.moveToFirst();
        entered_count = Integer.parseInt(c1.getString(0));

        if (entered_count < total_count)
            btn_count.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_rounded_red_hf));
        else
            btn_count.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_rounded_hf));

        TextView textView = (TextView) findViewById(R.id.txt_entered_count);
        textView.setText(String.valueOf(entered_count));
        //textView.setText("999");
        textView = (TextView) findViewById(R.id.txt_total_count);
        textView.setText(String.valueOf(total_count));
        //textView.setText("999");
    }

    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (OPERATION_NAME.equals("updatePickQty")) {
                current_save_img.setImageResource(R.drawable.loader);
            }
            if (OPERATION_NAME.equals("validateOrder")) {
                dialog.show();
            }
            progressBar.setVisibility(View.VISIBLE);
            //dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("getOrderItems")) {
                loadCards(params[0], params[1]);
                response = "success";
            }
            if (OPERATION_NAME.equals("updatePickQty")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("order_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("gold_code");
                pi.setType(String.class);
                pi.setValue(params[1]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("lv");
                pi.setType(String.class);
                pi.setValue(params[2]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("slno");
                pi.setType(String.class);
                pi.setValue(params[4]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("pick_qty");
                pi.setType(String.class);
                if (params[3] == null)
                    pi.setValue("''");
                else
                    pi.setValue(params[3]);
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
            }
            if (OPERATION_NAME.equals("validateOrder")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("order_no");
                pi.setType(String.class);
                pi.setValue(params[0]);
                request.addProperty(pi);

                pi = new PropertyInfo();
                pi.setName("user");
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
                    //System.out.println(response);
                } catch (Exception exception) {
                    response = exception.toString();
                }
            }
            //return response.toString();
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            if (OPERATION_NAME.equals("getOrderItems")) {
                //loadCards("all", "");
                //closeKeyboard();
                closeKeyboard();
                edt_firstRow.requestFocus();
                closeKeyboard();
                //edt_firstRow.clearFocus();
            }
            if (OPERATION_NAME.equals("validateOrder")) {
                if (result.contains("success")) {
                    Toast.makeText(getApplicationContext(), "Posted!", LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), Wh_pick.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
            if (OPERATION_NAME.equals("updatePickQty")) {
                if (result.contains("success")) {
                    if (global_pick_qty == null)
                        db.execSQL("update Wh_Order_Detl set PICK_QTY=null where ORDER_NO='" + order_no + "' and GOLD_CODE='" + global_gold_code + "' and lv='" + global_lv + "' and PO_LINE='" + global_po_line + "'");
                    else
                        db.execSQL("update Wh_Order_Detl set PICK_QTY=" + global_pick_qty + " where ORDER_NO='" + order_no + "' and GOLD_CODE='" + global_gold_code + "' and lv='" + global_lv + "' and PO_LINE='" + global_po_line + "'");
                    updateCount();
                    current_save_img.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_saved));
                    current_save_img.setImageResource(R.drawable.pick_icon_save);
                    closeKeyboard();
                } else
                    Toast.makeText(getApplicationContext(), result, LENGTH_SHORT).show();
            }
            closeKeyboard();
            progressBar.setVisibility(View.INVISIBLE);
            //dialog.dismiss();
        }
    }

    public void sortItems(View view) {
        if (0 == 0) {
            //Toast.makeText(this,sort_method,LENGTH_SHORT).show();
            LinearLayout layout_sort = (LinearLayout) findViewById(R.id.layout_sort);
            final int resourceId = Wh_pick_items.this.getResources().getIdentifier("button_bottom_sort_asc", "drawable", getApplicationContext().getPackageName());
            LayerDrawable layerDrawable = (LayerDrawable) getResources().getDrawable(resourceId);
            GradientDrawable gradientDrawable = (GradientDrawable) layerDrawable
                    .findDrawableByLayerId(R.id.gradient_sort);
            switch (sort_method) {
                case "slno": {
                    sort_method = "asc";
                    gradientDrawable.setColors(new int[]{
                            Color.parseColor("#F6698A"),
                            Color.parseColor("#DF083A")
                    });
                    layout_sort.setBackground(layerDrawable);
                    break;
                }
                case "asc": {
                    sort_method = "desc";
                    gradientDrawable.setColors(new int[]{
                            Color.parseColor("#DF083A"),
                            Color.parseColor("#F6698A")
                    });
                    layout_sort.setBackground(layerDrawable);
                    break;
                }
                case "desc": {
                    sort_method = "slno";
                    layout_sort.setBackground(null);
                    break;
                }
                default: {
                    sort_method = "slno";
                    layout_sort.setBackground(null);
                    break;
                }
            }
            offset = 0;
            current_page = 1;
            tv_current_page.setText("1");
            //loadCards("all", "");
            OPERATION_NAME = "getOrderItems";
            new MyTask().execute("all", "");
        }
    }

    public void loadCards(String flag, String search_barcode) {
        final String p_flag = flag;
        final String p_search_barcode = search_barcode;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                // Stuff that updates the UI

                Cursor cursor;
                String query = "";
                if (p_flag.equals("all")) {
                    switch (sort_method) {
                        case "slno": {
                            query = "select *from Wh_Order_Detl ORDER BY SLNO LIMIT " + line_per_page + " OFFSET " + offset;
                            break;
                        }
                        case "asc": {
                            query = "select *from Wh_Order_Detl ORDER BY PICK_QTY  LIMIT " + line_per_page + " OFFSET " + offset;
                            break;
                        }
                        case "desc": {
                            query = "select *from Wh_Order_Detl ORDER BY PICK_QTY DESC LIMIT " + line_per_page + " OFFSET " + offset;
                            break;
                        }
                        default: {
                            query = "select *from Wh_Order_Detl ORDER BY SLNO LIMIT " + line_per_page + " OFFSET " + offset;
                        }
                    }
                } else {
                    switch (sort_method) {
                        case "slno": {
                            query = "select *from Wh_Order_Detl where (GOLD_CODE||LV) IN (select GOLD_CODE||SU from Wh_Order_Bar_Detl where BARCODE='" + p_search_barcode + "')  ORDER BY SLNO LIMIT " + line_per_page + " OFFSET " + offset;
                            break;
                        }
                        case "asc": {
                            query = "select *from Wh_Order_Detl where (GOLD_CODE||LV) IN (select GOLD_CODE||SU from Wh_Order_Bar_Detl where BARCODE='" + p_search_barcode + "')  ORDER BY PICK_QTY LIMIT " + line_per_page + " OFFSET " + offset;
                            break;
                        }
                        case "desc": {
                            query = "select *from Wh_Order_Detl where (GOLD_CODE||LV) IN (select GOLD_CODE||SU from Wh_Order_Bar_Detl where BARCODE='" + p_search_barcode + "')  ORDER BY PICK_QTY DESC LIMIT " + line_per_page + " OFFSET " + offset;
                            break;
                        }
                        default: {
                            query = "select *from Wh_Order_Detl where (GOLD_CODE||LV) IN (select GOLD_CODE||SU from Wh_Order_Bar_Detl where BARCODE='" + p_search_barcode + "')  ORDER BY SLNO LIMIT " + line_per_page + " OFFSET " + offset;
                        }
                    }
                }
                cursor = db.rawQuery(query, null);
                LinearLayout layout_mn = (LinearLayout) findViewById(R.id.layout_main);
                layout_mn.removeAllViews();
                System.out.println("in loadCards");
                LinearLayout.LayoutParams divider_params = new LinearLayout.LayoutParams(3, ViewGroup.LayoutParams.MATCH_PARENT);
                //String t
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {
                    final String slno = cursor.getString(cursor.getColumnIndex("SLNO"));
                    final String barcode = cursor.getString(cursor.getColumnIndex("BARCODE"));
                    final String gold_code = cursor.getString(cursor.getColumnIndex("GOLD_CODE"));
                    final String lv = cursor.getString(cursor.getColumnIndex("LV"));
                    final String ord_unit = cursor.getString(cursor.getColumnIndex("ORD_UNIT"));
                    final String prod_desc = cursor.getString(cursor.getColumnIndex("PROD_DESC"));
                    final String order_qty = cursor.getString(cursor.getColumnIndex("ORDER_QTY"));
                    final String prep_qty = cursor.getString(cursor.getColumnIndex("PREP_QTY"));
                    final String pick_qty = cursor.getString(cursor.getColumnIndex("PICK_QTY"));
                    final String stock_unit = cursor.getString(cursor.getColumnIndex("STOCK_UNIT"));
                    final String conv = cursor.getString(cursor.getColumnIndex("CONV"));
                    final String order_conv = cursor.getString(cursor.getColumnIndex("ORDER_PACK"));
                    final String qty_in_order = cursor.getString(cursor.getColumnIndex("QTY_IN_ORDER"));
                    final String po_line = cursor.getString(cursor.getColumnIndex("PO_LINE"));
                    String soh = cursor.getString(cursor.getColumnIndex("SOH"));
                    final Double d_soh = Double.parseDouble(soh);
                    final Double d_qty_in_order = Double.parseDouble(qty_in_order);

                    final Double d_prep_qty = Double.parseDouble(prep_qty);

                    System.out.println(barcode);

                    LinearLayout layout_card = new LinearLayout(getApplicationContext());
                    layout_card.setOrientation(LinearLayout.HORIZONTAL);
                    layout_card.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.menu_card_pick));
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(10, 0, 10, -10);
                    layout_card.setLayoutParams(layoutParams);
                    layout_card.setPadding(20, 20, 20, 20);

                    TextView txt_slno = new TextView(getApplicationContext());
                    txt_slno.setText(slno);
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    txt_slno.setTextColor(Color.parseColor("#ffffff"));
                    txt_slno.setGravity(Gravity.CENTER_HORIZONTAL + Gravity.CENTER_VERTICAL);
                    txt_slno.setLayoutParams(layoutParams);

                    layout_card.addView(txt_slno);

                    LinearLayout layout_divider_4 = new LinearLayout(new ContextThemeWrapper(getApplicationContext(), R.style.Divider));
                    layout_divider_4.setLayoutParams(divider_params);

                    layout_card.addView(layout_divider_4);

                    // Product Info Starts Here
                    LinearLayout layout_product = new LinearLayout(getApplicationContext());
                    layout_product.setOrientation(LinearLayout.VERTICAL);
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout_product.setLayoutParams(layoutParams);

                    LinearLayout layout_art_data = new LinearLayout(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout_art_data.setLayoutParams(layoutParams);
                    layout_art_data.setOrientation(LinearLayout.HORIZONTAL);

                    TextView txt_barcode = new TextView(getApplicationContext());
                    txt_barcode.setText(barcode + " |");
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 0, 5, 0);
                    txt_barcode.setTextColor(Color.parseColor("#ffffff"));
                    txt_barcode.setTextSize(10);
                    txt_barcode.setLayoutParams(layoutParams);

                    layout_art_data.addView(txt_barcode);

                    TextView txt_conv = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    txt_conv.setLayoutParams(layoutParams);
                    txt_conv.setTextColor(Color.parseColor("#ffffff"));
                    txt_conv.setTextSize(10);
                    txt_conv.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_conv_sm_trans));
                    txt_conv.setText("  x" + conv + "  ");

                    layout_art_data.addView(txt_conv);

                    TextView txt_article = new TextView(getApplicationContext());
                    txt_article.setText("| " + gold_code + " | " + lv + " | " + ord_unit);
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 0, 10, 0);
                    txt_article.setTextColor(Color.parseColor("#ffffff"));
                    txt_article.setTextSize(10);
                    txt_article.setLayoutParams(layoutParams);
                    layout_art_data.addView(txt_article);

                    final ImageButton arrow_down_img = new ImageButton(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(pxFromDp(getApplicationContext(), 15), pxFromDp(getApplicationContext(), 15));
                    layoutParams.gravity = Gravity.TOP;
                    layoutParams.leftMargin = 0;
                    layoutParams.rightMargin = 0;
                    layoutParams.topMargin = 0;
                    arrow_down_img.setBackground(null);
                    arrow_down_img.setImageResource(R.drawable.pick_arrow_down);
                    arrow_down_img.setLayoutParams(layoutParams);
                    arrow_down_img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    arrow_down_img.setPadding(0, 0, 0, 0);

                    arrow_down_img.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showBarcodes(gold_code, lv);
                        }
                    });

                    layout_art_data.addView(arrow_down_img);

                    TextView txt_prod_desc = new TextView(getApplicationContext());
                    txt_prod_desc.setText(prod_desc);
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 5, 10, 5);
                    txt_prod_desc.setTextColor(Color.parseColor("#ffffff"));
                    txt_prod_desc.setTextSize(13);
                    txt_prod_desc.setMaxWidth(pxFromDp(Wh_pick_items.this, 170));
                    txt_prod_desc.setMinWidth(pxFromDp(Wh_pick_items.this, 170));
                    txt_prod_desc.setLayoutParams(layoutParams);

                    //Order Conversion and SOH
                    LinearLayout layout_sku_count = new LinearLayout(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layout_sku_count.setLayoutParams(layoutParams);
                    layout_sku_count.setOrientation(LinearLayout.HORIZONTAL);

                    TextView txt_lbl_order_conv = new TextView(getApplicationContext());
                    txt_lbl_order_conv.setText("No. SKU/PU ");
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 0, 5, 0);
                    txt_lbl_order_conv.setTextColor(Color.parseColor("#ffffff"));
                    txt_lbl_order_conv.setTextSize(10);
                    txt_lbl_order_conv.setLayoutParams(layoutParams);

                    TextView txt_order_conv = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    txt_order_conv.setLayoutParams(layoutParams);
                    txt_order_conv.setTextColor(Color.parseColor("#ffffff"));
                    txt_order_conv.setTextSize(10);
                    txt_order_conv.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_conv_sm_trans));
                    txt_order_conv.setText("  " + order_conv + "  ");

                    TextView txt_lbl_soh = new TextView(getApplicationContext());
                    txt_lbl_soh.setText("SOH ");
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 0, 5, 0);
                    txt_lbl_soh.setTextColor(Color.parseColor("#ffffff"));
                    txt_lbl_soh.setTextSize(10);
                    txt_lbl_soh.setLayoutParams(layoutParams);

                    TextView txt_soh = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    txt_soh.setLayoutParams(layoutParams);
                    txt_soh.setTextColor(Color.parseColor("#ffffff"));
                    txt_soh.setTextSize(10);
                    txt_soh.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_conv_sm_trans));
                    txt_soh.setText("  " + soh + "  ");

                    //if (ord_unit.equals("Pie")) {
               /* if(stock_unit.toLowerCase().contains("kilo"))
                    txt_sku_qty.setText(" " + Double.parseDouble(prep_qty) + " ");
                else
                    txt_sku_qty.setText(" " + Integer.parseInt(conv) * Integer.parseInt(prep_qty) + " ");
            /*} else if (ord_unit.equals("PCK")) {
                if(stock_unit.toLowerCase().contains("kilo"))
                    txt_sku_qty.setText(" " + Integer.parseInt(conv) * Double.parseDouble(prep_qty) + " ");
                else
                    txt_sku_qty.setText(" " + Integer.parseInt(conv) * Integer.parseInt(prep_qty) + " ");
            }
            else {
                if(stock_unit.toLowerCase().contains("kilo"))
                    txt_sku_qty.setText(" " + Double.parseDouble(prep_qty) + " ");
                else
                    txt_sku_qty.setText(" " + Integer.parseInt(prep_qty) + " ");
            }*/

                    layout_sku_count.addView(txt_lbl_order_conv);
                    layout_sku_count.addView(txt_order_conv);
                    layout_sku_count.addView(txt_lbl_soh);
                    layout_sku_count.addView(txt_soh);

                    //SOH
            /*LinearLayout layout_soh = new LinearLayout(getApplicationContext());
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin=6;
            layout_soh.setLayoutParams(layoutParams);
            layout_soh.setOrientation(LinearLayout.HORIZONTAL);

            TextView txt_lbl_soh = new TextView(getApplicationContext());
            txt_lbl_soh.setText("SOH ");
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(5, 0, 5, 0);
            txt_lbl_soh.setTextColor(Color.parseColor("#ffffff"));
            txt_lbl_soh.setTextSize(10);
            txt_lbl_soh.setLayoutParams(layoutParams);

            TextView txt_soh = new TextView(getApplicationContext());
            layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            txt_soh.setLayoutParams(layoutParams);
            txt_soh.setTextColor(Color.parseColor("#ffffff"));
            txt_soh.setTextSize(10);
            txt_soh.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_conv_sm_trans));
            txt_soh.setText("  " + soh + "  ");

            layout_soh.addView(txt_lbl_soh);
            layout_soh.addView(txt_soh);*/

                    //SOH ENDS HERE

                    //Order Conversion and SOH
                    LinearLayout layout_qty_in_ord = new LinearLayout(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.topMargin = pxFromDp(getApplicationContext(), 5);
                    layout_qty_in_ord.setLayoutParams(layoutParams);
                    layout_qty_in_ord.setOrientation(LinearLayout.HORIZONTAL);

                    TextView txt_lbl_qty_in_ord = new TextView(getApplicationContext());
                    txt_lbl_qty_in_ord.setText("Qty in Other Orders ");
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 0, 5, 0);
                    txt_lbl_qty_in_ord.setTextColor(Color.parseColor("#ffffff"));
                    txt_lbl_qty_in_ord.setTextSize(10);
                    txt_lbl_qty_in_ord.setLayoutParams(layoutParams);

                    TextView txt_qty_in_ord = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    txt_qty_in_ord.setLayoutParams(layoutParams);
                    txt_qty_in_ord.setTextColor(Color.parseColor("#ffffff"));
                    txt_qty_in_ord.setTextSize(10);
                    txt_qty_in_ord.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_conv_sm_trans));
                    txt_qty_in_ord.setText("  " + qty_in_order + "  ");
                    layout_qty_in_ord.addView(txt_lbl_qty_in_ord);
                    layout_qty_in_ord.addView(txt_qty_in_ord);

                    layout_product.addView(layout_art_data);
                    layout_product.addView(txt_prod_desc);
                    layout_product.addView(layout_sku_count);
                    layout_product.addView(layout_qty_in_ord);
                    //layout_product.addView(layout_soh);

                    layout_product.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            showBarcodes(gold_code, lv);
                        }
                    });

                    layout_card.addView(layout_product);

                    // Product Info Ends Here

                    LinearLayout layout_divider_1 = new LinearLayout(new ContextThemeWrapper(getApplicationContext(), R.style.Divider));
                    layout_divider_1.setLayoutParams(divider_params);

                    layout_card.addView(layout_divider_1);

                    //VALUES LAYOUT STARTS HERE
                    LinearLayout layout_values = new LinearLayout(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    layout_values.setMinimumWidth(pxFromDp(getApplicationContext(), 40));
                    layout_values.setOrientation(LinearLayout.VERTICAL);
                    layout_values.setLayoutParams(layoutParams);

                    TextView txt_order_lbl = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    txt_order_lbl.setLayoutParams(layoutParams);
                    txt_order_lbl.setGravity(Gravity.BOTTOM + Gravity.CENTER_HORIZONTAL);
                    txt_order_lbl.setText("Ordered");
                    txt_order_lbl.setTextColor(Color.parseColor("#ffffff"));
                    txt_order_lbl.setTextSize(8);

                    TextView txt_order_qty = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    txt_order_qty.setLayoutParams(layoutParams);
                    txt_order_qty.setGravity(Gravity.CENTER_VERTICAL + Gravity.CENTER_HORIZONTAL);
                    txt_order_qty.setText(order_qty);
                    txt_order_qty.setTextColor(Color.parseColor("#ffffff"));
                    txt_order_qty.setTextSize(13);

                    LinearLayout layout_divider_5 = new LinearLayout(new ContextThemeWrapper(getApplicationContext(), R.style.Divider));
                    LinearLayout.LayoutParams divider_params_hz = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pxFromDp(getApplicationContext(), 1));
                    layout_divider_5.setLayoutParams(divider_params_hz);

                    TextView txt_prep_lbl = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    txt_prep_lbl.setLayoutParams(layoutParams);
                    txt_prep_lbl.setGravity(Gravity.CENTER_VERTICAL + Gravity.CENTER_HORIZONTAL);
                    txt_prep_lbl.setText("Prepared");
                    txt_prep_lbl.setTextColor(Color.parseColor("#ffffff"));
                    txt_prep_lbl.setTextSize(8);

                    TextView txt_prep_qty = new TextView(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, pxFromDp(getApplicationContext(), 10), 1);
                    layoutParams.gravity = Gravity.CENTER_HORIZONTAL+Gravity.CENTER_VERTICAL;
                    txt_prep_qty.setGravity(Gravity.CENTER_VERTICAL);
                    txt_prep_qty.setLayoutParams(layoutParams);
                    txt_prep_qty.setText(" " + prep_qty + " ");
                    txt_prep_qty.setTextColor(Color.parseColor("#000000"));
                    txt_prep_qty.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.card_conv_sm));
                    txt_prep_qty.setTextSize(12);
                    //txt_prep_qty.getGravity(Gravity.CENTER_VERTICAL);

                    layout_values.addView(txt_order_lbl);
                    layout_values.addView(txt_order_qty);
                    layout_values.addView(layout_divider_5);
                    layout_values.addView(txt_prep_lbl);
                    layout_values.addView(txt_prep_qty);

                    layout_card.addView(layout_values);

                    //VALUES LAYOUT ENDS HERE


                    LinearLayout layout_divider_3 = new LinearLayout(new ContextThemeWrapper(getApplicationContext(), R.style.Divider));
                    layout_divider_3.setLayoutParams(divider_params);

                    layout_card.addView(layout_divider_3);

                    //User input Layout
                    LinearLayout layout_user_input = new LinearLayout(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    layoutParams.gravity = Gravity.CENTER_VERTICAL;
                    layout_user_input.setLayoutParams(layoutParams);
                    layout_user_input.setOrientation(LinearLayout.VERTICAL);


                    final EditText edt_pick_qty = new EditText(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(pxFromDp(getApplicationContext(), 53), pxFromDp(getApplicationContext(), 35), 1);
                    layoutParams.setMargins(0, 0, 0, 0);
                    layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                    edt_pick_qty.setMaxWidth(pxFromDp(getApplicationContext(), 47));
                    edt_pick_qty.setMinWidth(pxFromDp(getApplicationContext(), 47));
                    edt_pick_qty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    edt_pick_qty.setGravity(Gravity.CENTER_VERTICAL);
                    edt_pick_qty.setPadding(pxFromDp(getApplicationContext(), -1000), 0, pxFromDp(getApplicationContext(), -1000), 0);
                    if (stock_unit.toLowerCase().contains("kilo"))
                        edt_pick_qty.setInputType(InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    else
                        edt_pick_qty.setInputType(InputType.TYPE_CLASS_NUMBER);
                    edt_pick_qty.setTextSize(12);
                    edt_pick_qty.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.form_input_field_pick));
                    edt_pick_qty.setLayoutParams(layoutParams);
                    edt_pick_qty.setMaxWidth(pxFromDp(Wh_pick_items.this, 53));
                    edt_pick_qty.setMinWidth(pxFromDp(Wh_pick_items.this, 53));
                    //edt_pick_qty.setMaxHeight(pxFromDp(Wh_pick_items.this,35));
                    edt_pick_qty.setHeight(pxFromDp(Wh_pick_items.this, 35));
                    if (i == 0)
                        edt_firstRow = edt_pick_qty;
                    edt_pick_qty.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    edt_pick_qty.setTextColor(Color.parseColor("#000000"));
                    edt_pick_qty.clearFocus();

                    final ImageButton save_img = new ImageButton(getApplicationContext());
                    layoutParams = new LinearLayout.LayoutParams(pxFromDp(getApplicationContext(), 40), pxFromDp(getApplicationContext(), 23));
                    layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                    layoutParams.leftMargin = 0;
                    layoutParams.rightMargin = 0;
                    layoutParams.topMargin = 5;
                    save_img.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_normal));
                    save_img.setImageResource(R.drawable.pick_icon_validate_black);
                    save_img.setLayoutParams(layoutParams);
                    save_img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    save_img.setPadding(0, 0, 0, 0);

                    if (pick_qty != null) {
                        if (pick_qty.length() > 0 && !pick_qty.equals("null")) {
                            edt_pick_qty.setText(pick_qty);
                            save_img.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_saved));
                            save_img.setImageResource(R.drawable.pick_icon_save);
                        }
                    }

                    edt_pick_qty.setOnKeyListener(new View.OnKeyListener() {
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            // If the event is a key-down event on the "enter" button
                            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                                closeKeyboard();
                                return true;
                            }
                            return false;
                        }
                    });

                    edt_pick_qty.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            //Toast.makeText(getApplicationContext(),"changed "+edt_pick_qty.getText().toString(),LENGTH_SHORT).show();
                            save_img.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_normal));
                            save_img.setImageResource(R.drawable.pick_icon_validate_black);
                        }

                        @Override
                        public void afterTextChanged(Editable s) {

                        }
                    });

                    //save_img.setImageResource(R.drawable.validate_sm1);
                    save_img.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int flag = 0;
                            current_save_img = save_img;
                            Double d_pick_qty = 0.0;
                            if (edt_pick_qty.getText().toString().length() > 0) {
                                d_pick_qty = Double.parseDouble(edt_pick_qty.getText().toString());
                                if (edt_pick_qty.getText().toString().length() == 0) {
                                    flag = 1;
                                    Toast.makeText(getApplicationContext(), "Please Enter Picked Qty", Toast.LENGTH_SHORT).show();
                                } else {
                                    if (d_pick_qty == 0) {
                                        flag = 1;
                                        Toast.makeText(getApplicationContext(), "Picked Qty Can't be Zero", Toast.LENGTH_SHORT).show();
                                    } else if (d_pick_qty > d_prep_qty) {
                                        if (d_pick_qty > d_soh - d_qty_in_order) {
                                            flag = 1;
                                            Toast.makeText(getApplicationContext(), "Stock Not Available", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            } else {
                                edt_pick_qty.setText("");
                            }
                            if (flag == 0) {
                                closeKeyboard();
                                global_gold_code = gold_code;
                                global_lv = lv;
                                global_po_line = po_line;
                                OPERATION_NAME = "updatePickQty";
                                if (d_pick_qty != 0) {
                                    global_pick_qty = edt_pick_qty.getText().toString();
                                    new MyTask().execute(order_no, gold_code, lv, d_pick_qty.toString(), po_line);
                                } else {
                                    global_pick_qty = null;
                                    new MyTask().execute(order_no, gold_code, lv, null, po_line);
                                }
                            }
                        }
                    });

                    layout_user_input.addView(edt_pick_qty);
                    layout_user_input.addView(save_img);

                    layout_card.addView(layout_user_input);

                    //layout_card.addView(save_img);

                    layout_mn.addView(layout_card);

                    cursor.moveToNext();
                }
                cursor.close();
            }
        });
        closeKeyboard();
    }

    public void showBarcodes(String gold_code, String lv) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Barcodes");
        Cursor cursor1 = db.rawQuery("SELECT BARCODE FROM Wh_Order_Bar_Detl WHERE GOLD_CODE=" + gold_code + " AND SU=" + lv + "", null);
        String barcodes = "";
        cursor1.moveToFirst();
        if (cursor1.getCount() > 0) {
            for (int j = 0; j < cursor1.getCount(); j++) {
                barcodes += cursor1.getString(0) + "\n";
                cursor1.moveToNext();
            }
        }
        cursor1.close();
        barcodes = barcodes.substring(0, barcodes.length() - 1);
        builder.setMessage(barcodes);
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        //Toast.makeText(getApplicationContext(),barcodes,Toast.LENGTH_LONG).show();
        builder.show();
    }

    public void searchPressed(View view) {
        Context context = getApplicationContext();
        final EditText txt_barcode = new EditText(context);
        txt_barcode.setHint("Scan Barcode");
        txt_barcode.setTextColor(Color.parseColor("#000000"));
        txt_barcode.setInputType(InputType.TYPE_CLASS_NUMBER);
        txt_barcode.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 3));
        if (search_barcode.length() > 0)
            txt_barcode.setText(search_barcode);
        txt_barcode.setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
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

        Button d_btn_clear_search = new Button(this);
        d_btn_clear_search.setText("Clear Search");
        d_btn_clear_search.setTextColor(Color.parseColor("#ED194A"));
        d_btn_clear_search.setBackground(null);

        Button d_btn_search = new Button(this);
        d_btn_search.setText("Search");
        d_btn_search.setTextColor(Color.parseColor("#ED194A"));
        d_btn_search.setBackground(null);

        layout_btns.addView(d_btn_close);
        layout_btns.addView(d_btn_clear_search);
        layout_btns.addView(d_btn_search);
        layout.addView(layout_btns);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Barcode");
        builder.setView(layout);

        final AlertDialog ad = builder.show();

        txt_barcode.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (txt_barcode.getText().toString().length() > 0) {
                        ad.dismiss();
                        btn_search.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_rounded_red));
                        btn_search.setImageResource(R.drawable.pick_icon_search_clear);
                        search_barcode = txt_barcode.getText().toString();
                        offset = 0;
                        current_page = 1;
                        tv_current_page.setText("1");
                        //loadCards("barcode", txt_barcode.getText().toString());
                        OPERATION_NAME = "getOrderItems";
                        new MyTask().execute("barcode", txt_barcode.getText().toString());
                        //Toast.makeText(getApplicationContext(),"sdadsad",LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
        d_btn_clear_search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ad.dismiss();
                clearSearch();
            }
        });
        d_btn_clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search_barcode = "";
                txt_barcode.setText("");
                txt_barcode.requestFocus();
            }
        });
        d_btn_close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ad.dismiss();
                if (txt_barcode.getText().toString().length() == 0) {
                    clearSearch();
                }
            }
        });
        d_btn_search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (txt_barcode.getText() != null) {
                    String barcode = txt_barcode.getText().toString();
                    if (barcode.length() > 0) {
                        ad.dismiss();
                        btn_search.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_rounded_red));
                        btn_search.setImageResource(R.drawable.pick_icon_search_clear);
                        search_barcode = barcode;
                        offset = 0;
                        current_page = 1;
                        tv_current_page.setText("1");
                        //loadCards("barcode", barcode);
                        OPERATION_NAME = "getOrderItems";
                        new MyTask().execute("barcode", barcode);
                    } else
                        Toast.makeText(getApplicationContext(), "Please Enter a Barcode", LENGTH_SHORT).show();
                } else
                    Toast.makeText(getApplicationContext(), "Please Enter a Barcode", LENGTH_SHORT).show();
            }
        });
    }

    public void clearSearch() {
        search_barcode = "";
        btn_search.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_rounded));
        btn_search.setImageResource(R.drawable.pick_icon_search);
        offset = 0;
        current_page = 1;
        tv_current_page.setText("1");
        //loadCards("all", "");
        OPERATION_NAME = "getOrderItems";
        new MyTask().execute("all", "");
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static Integer pxFromDp(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public void validatePressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Post Document");
        builder.setMessage("Are You Sure to Post?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                OPERATION_NAME = "validateOrder";
                new MyTask().execute(order_no, user);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.show();
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

    public void nextPressed(View view) {
        System.out.println("rowcount: " + rowcount);
        System.out.println("offset: " + offset);
        if (rowcount > offset + line_per_page) {
            current_page++;
            tv_current_page.setText(String.valueOf(current_page));
            offset += line_per_page;
            btn_search.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_rounded));
            btn_search.setImageResource(R.drawable.pick_icon_search);
            loadCards("all", "");
            OPERATION_NAME = "getOrderItems";
            new MyTask().execute("all", "");
        }
    }

    public void previousPressed(View view) {
        System.out.println("rowcount: " + rowcount);
        System.out.println("offset: " + offset);
        if (offset < rowcount && offset != 0) {
            current_page--;
            tv_current_page.setText(String.valueOf(current_page));
            offset -= line_per_page;
            btn_search.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button_bottom_rounded));
            btn_search.setImageResource(R.drawable.pick_icon_search);
            loadCards("all", "");
            OPERATION_NAME = "getOrderItems";
            new MyTask().execute("all", "");
        }
    }
}
