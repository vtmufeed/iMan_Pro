package com.grand.imanpro;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Calendar;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {
    public String SOAP_ACTION = "";
    public String OPERATION_NAME = "";
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public String SOAP_ADDRESS = "", user = "";
    SQLiteDatabase db = null;
    public String ip = "", loc = "";
    JSONArray menu_array;
    LayoutTransition transition;
    TextView tv_loc;
    String invent_type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.VISIBLE);

        tv_loc = (TextView) findViewById(R.id.txt_loc);
        user = getUser();
        TextView txt = (TextView) findViewById(R.id.txt_user_id);
        txt.setText(user.split(",")[1]);
        txt = (TextView) findViewById(R.id.txt_username);
        txt.setText(user.split(",")[1]);
        String user_type = user.split(",")[2];
        user = user.split(",")[0];

        Button btn = (Button) findViewById(R.id.btn_settings);
        if (user_type.equals("A"))
            btn.setVisibility(View.VISIBLE);
        else
            btn.setVisibility(View.GONE);

        db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from server_ip", null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                String[] ips = (cursor.getString(cursor.getColumnIndex("ip")).split("/"));
                ip = ips[0];
                SOAP_ADDRESS = "http://" + ip + "/iManWebService/Service.asmx";
                loc = cursor.getString(cursor.getColumnIndex("loc"));
                tv_loc.setText(loc);
                tv_loc.setTextColor(Color.parseColor("#000000"));
            }
            if (loc == null) {
                Toast.makeText(this, "Location is not Set! Please contact system administrator", LENGTH_SHORT).show();
                tv_loc.setText("!");
                tv_loc.setTextColor(Color.parseColor("#ff2200"));
            }
        }
        TextView textView = (TextView) findViewById(R.id.txt_greeting);
        TextPaint paint = textView.getPaint();
        float width = paint.measureText("Tianjin, China");

        ImageView imageView = (ImageView) findViewById(R.id.img_profile);
        imageView.setImageDrawable(null);
        imageView.setImageResource(0);
        Picasso.get().load("http://" + ip + "/iManWebService/Images/Profile/" + user + ".png").memoryPolicy(MemoryPolicy.NO_CACHE).networkPolicy(NetworkPolicy.NO_CACHE)
                .placeholder(R.drawable.user_black).into(imageView);
        System.out.println("http://" + ip + "/iManWebService/Images/Profile/" + user + ".png");

        Shader textShader = new LinearGradient(0, 0, width, textView.getTextSize(),
                new int[]{
                        Color.parseColor("#84348b"),
                        Color.parseColor("#578ce8"),
                }, null, Shader.TileMode.CLAMP);
        textView.getPaint().setShader(textShader);
        textView = (TextView) findViewById(R.id.txt_username);
        textView.getPaint().setShader(textShader);
        transition = new LayoutTransition();

        OPERATION_NAME = "appGetUserMenu";
        new MyTask().execute(user);
    }

    public String getUser() {
        String user_id = "", user_name = "", user_type = "";
        SQLiteDatabase db = openOrCreateDatabase("imanpro", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("select * from current_user", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                user_id = cursor.getString(cursor.getColumnIndex("id"));
                user_name = cursor.getString(cursor.getColumnIndex("name"));
                user_type = cursor.getString(cursor.getColumnIndex("type"));
                invent_type = cursor.getString(cursor.getColumnIndex("invent_type"));
                System.out.println(user_type);
            }
        }
        return user_id + "," + user_name + "," + user_type;
    }

    private class MyTask extends AsyncTask<String, String, String> {
        public String strJson = "";
        public String str = "";
        public int fl = 0;
        public String flag = "";

        @Override
        protected void onPreExecute() {
            /*ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);*/
        }

        @Override
        protected String doInBackground(String... params) {
            Object response = null;
            if (OPERATION_NAME.equals("appGetUserMenu")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(user);
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
                    response = "error " + exception.toString();
                }
            }
            if (OPERATION_NAME.equals("appGetUserCat")) {
                SOAP_ACTION = "http://tempuri.org/" + OPERATION_NAME;
                SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, OPERATION_NAME);

                PropertyInfo pi = new PropertyInfo();
                pi.setName("user");
                pi.setType(String.class);
                pi.setValue(user);
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
                    response = "error " + exception.toString();
                }
            }
            return response.toString();
        }

        protected void onProgressUpdate(String... value) {
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(String result) {
            /*ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            progressBar.setVisibility(View.INVISIBLE);*/
            if (OPERATION_NAME.equals("appGetUserMenu")) {
                try {
                    JSONObject json = new JSONObject(result);
                    int hour = Integer.parseInt(json.getString("hour"));
                    System.out.println(hour);
                    String greeting = "Good ";
                    if (hour >= 5 && hour <= 11)
                        greeting += "Morning";
                    else if (hour >= 12 && hour <= 16)
                        greeting += "Afternoon";
                    else if (hour >= 17 && hour <= 23)
                        greeting += "Evening";
                    else if (hour >= 0 && hour <= 4)
                        greeting += "Evening";
                    greeting += ",";
                    TextView textView = (TextView) findViewById(R.id.txt_greeting);
                    textView.setText(greeting);
                    json = new JSONObject(json.getString("menu"));
                    JSONArray leaders = json.getJSONArray("IMAN_MENU_ITEMS");
                    menu_array = leaders;
                    OPERATION_NAME = "appGetUserCat";
                    new MyTask().execute();
                    //System.out.println(result);
                } catch (JSONException e) {
                    System.out.println(e.getMessage());
                }
            }
            if (OPERATION_NAME.equals("appGetUserCat")) {
                LinearLayout main_layout = (LinearLayout) findViewById(R.id.main_layout);
                JSONObject json = null;
                try {
                    System.out.println(result);
                    json = new JSONObject(result);
                    JSONArray array_cat = json.getJSONArray("IMAN_MENU_CAT");

                    for (int i = 0; i < array_cat.length(); i++) {
                        JSONObject row_cat = array_cat.getJSONObject(i);
                        /*String menu_id = row_cat.getString("MENU_ID");
                        String menu_name = row_cat.getString("MENU_NAME");
                        String menu_image = row_cat.getString("MENU_IMG_NAME");*/
                        String cat_code = row_cat.getString("CAT_CODE").replace(".0", "");
                        //System.out.println(cat_code);
                        LinearLayout linearLayout = new LinearLayout(getApplicationContext());
                        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 220);
                        layoutParams.setMargins(0, 5, 0, 10);
                        linearLayout.setLayoutParams(layoutParams);

                        for (int j = 0; j < menu_array.length(); j++) {
                            JSONObject row_menu = menu_array.getJSONObject(j);
                            String menu_cat = row_menu.getString("MENU_CATEGORY").replace(".0", "");
                            final String menu_id = row_menu.getString("MENU_ID");
                            String menu_name = row_menu.getString("MENU_NAME");
                            String menu_image = row_menu.getString("MENU_IMG_NAME");
                            String color1 = row_menu.getString("MENU_COLOR1");
                            String color2 = row_menu.getString("MENU_COLOR2");
                            //System.out.println(cat_code);
                            int menu_image_width = Integer.parseInt(row_menu.getString("MENU_IMG_HEIGHT").replace(".0", ""));
                            int menu_image_height = Integer.parseInt(row_menu.getString("MENU_IMG_HEIGHT").replace(".0", ""));
                            if (cat_code.equals(menu_cat)) {
                                LinearLayout linearLayout1 = new LinearLayout(getApplicationContext());
                                linearLayout1.setOrientation(LinearLayout.HORIZONTAL);
                                LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
                                layoutParams1.setMargins(4, 0, 4, 0);
                                layoutParams1.weight = 1;

                                final int resourceId = MainActivity.this.getResources().getIdentifier("menu_card_" + menu_id, "drawable", getApplicationContext().getPackageName());
                                System.out.println("menu_card_" + menu_id);
                                LayerDrawable layerDrawable;
                                /*layerDrawable = (LayerDrawable) getResources()
                                        .getDrawable(resourceId);*/
                                try {
                                    layerDrawable = (LayerDrawable) getResources()
                                            .getDrawable(resourceId);
                                }
                                catch (Exception ex)
                                {
                                    final int rId = MainActivity.this.getResources().getIdentifier("menu_card_1", "drawable", getApplicationContext().getPackageName());
                                    layerDrawable = (LayerDrawable) getResources()
                                            .getDrawable(rId);
                                }
                                GradientDrawable gradientDrawable = (GradientDrawable) layerDrawable
                                        .findDrawableByLayerId(R.id.gradient);
                                gradientDrawable.setColors(new int[]{
                                        Color.parseColor(color1),
                                        Color.parseColor(color2)
                                });
                                linearLayout1.setBackground(layerDrawable);


                                linearLayout1.setLayoutParams(layoutParams1);
                                linearLayout1.setId(i);
                                final int index = i;
                                linearLayout1.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        if (menu_id.replace(".0", "").equals("2")) {
                                            Intent intent = new Intent(getApplicationContext(), Inventory.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("4")) {
                                            Intent intent = new Intent(getApplicationContext(), ArticleData.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("5")) {
                                            Intent intent = new Intent(getApplicationContext(), StockTransfer.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("1")) {
                                            Intent intent = new Intent(getApplicationContext(), LabelSelectPrinter.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("3")) {
                                            Intent intent = new Intent(getApplicationContext(), GoldVsVision.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("6")) {
                                            Intent intent = new Intent(getApplicationContext(), Wh_pick.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("7")) {
                                            Intent intent = new Intent(getApplicationContext(), VisionOtherBar.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("9")) {
                                            Intent intent = new Intent(getApplicationContext(), Wh_rcv_po.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("10")) {
                                            Intent intent = new Intent(getApplicationContext(), Ret_request.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("11")) {
                                            Intent intent = new Intent(getApplicationContext(), Shelf_qty.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("12")) {
                                            Intent intent = new Intent(getApplicationContext(), BarcodeGen.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        if (menu_id.replace(".0", "").equals("13")) {
                                            Intent intent = new Intent(getApplicationContext(), PeopleCount.class);
                                            intent.putExtra("user", user);
                                            startActivity(intent);
                                        }
                                        System.out.println("The index is" + index);
                                    }
                                });

                                ImageView imageView = new ImageView(getApplicationContext());
                                LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(menu_image_width, menu_image_height);
                                layoutParams2.setMargins(10, 0, 0, 0);
                                layoutParams2.gravity = Gravity.CENTER_VERTICAL;
                                int resID = getResId("health", R.drawable.class);
                                //imageView.setImageResource(resID);
                                Picasso.get().load("http://" + ip + "/iManWebService/Images/" + menu_image).into(imageView);
                                imageView.setLayoutParams(layoutParams2);

                                TextView textView = new TextView(getApplicationContext());
                                textView.setText(menu_name);
                                textView.setGravity(Gravity.CENTER_VERTICAL + Gravity.LEFT);
                                LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                                layoutParams3.setMargins(5, 0, 5, 0);
                                textView.setLayoutParams(layoutParams3);
                                textView.setTextAppearance(MainActivity.this, R.style.menu_item_text);
                                final Typeface typeface = ResourcesCompat.getFont(MainActivity.this, R.font.tcm);
                                textView.setTypeface(typeface);
                                textView.setTypeface(textView.getTypeface(), Typeface.BOLD);

                                linearLayout1.addView(imageView);
                                linearLayout1.addView(textView);
                                linearLayout.addView(linearLayout1);
                                //linearLayout.setLayoutTransition(transition);
                                //linearLayout1.setLayoutTransition(transition);
                            }
                        }
                        main_layout.addView(linearLayout);
                        //main_layout.setLayoutTransition(transition);
                    }
                    TextView tv_copy_right = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams tv_params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tv_params.setMargins(0, 0, 0, 8);
                    tv_copy_right.setGravity(Gravity.CENTER);
                    tv_copy_right.setTextColor(Color.parseColor("#000000"));
                    //tv_copy_right.setTypeface(null, Typeface.BOLD);
                    int year = Calendar.getInstance().get(Calendar.YEAR);
                    tv_copy_right.setText("© Copy Right 2020 IT Dept. Grand Qatar");
                    tv_copy_right.setLayoutParams(tv_params);
                    main_layout.addView(tv_copy_right);
                } catch (JSONException e) {
                    //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    //ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar2);
                    //progressBar.setVisibility(View.GONE);
                    e.printStackTrace();
                }
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar2);
                progressBar.setVisibility(View.GONE);
            }
        }
    }
    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void settingsPressed(View view) {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    public void logoutClikced(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}
