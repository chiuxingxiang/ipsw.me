package com.q.ipswme;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    String devicesJson;
    String versionsJson;
    String detailJson;
    String IPSWDownloadLink;
    ArrayList<String> devices = new ArrayList<>();
    ArrayList<String> identifier = new ArrayList<>();
    ArrayList<String> versions = new ArrayList<>();
    ArrayList<String> buildID = new ArrayList<>();
    ArrayAdapter<String> devicesAdapter;
    ArrayAdapter<String> versionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner devicesSpinner = findViewById(R.id.devicesSpinner);
        Spinner versionsSpinner = findViewById(R.id.versionsSpinner);
        Button copyLink = findViewById(R.id.copyLink);
        TextView versionDetail = findViewById(R.id.versionDetail);
        TextView issueDateDetail = findViewById(R.id.issueDateDetail);
        TextView MD5Detail = findViewById(R.id.MD5Detail);
        TextView SHA1Detail = findViewById(R.id.SHA1Detail);
        TextView ifSignedDetail = findViewById(R.id.ifSignedDetail);

        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, devices);
        versionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, versions);

        copyLink.setEnabled(false);

        NetworkUtil checkNetworkStatus = new NetworkUtil();
        if(checkNetworkStatus.isNetworkConnected(getApplicationContext())){
            Toast.makeText(MainActivity.this, "网络连接成功", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(MainActivity.this, "网络连接失败", Toast.LENGTH_SHORT).show();
        }

        devicesSpinner.setAdapter(devicesAdapter);
        versionsSpinner.setAdapter(versionsAdapter);

        String url = "https://api.ipsw.me/v4/devices";
        httpGet(url);

        devicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                //Toast.makeText(MainActivity.this, "你点击的是:"+ devices.get(pos), 2000).show();
                String url = "https://api.ipsw.me/v4/device/" + identifier.get(pos) + "?type=ipsw";
                Log.d("url",url);
                httpGet(url);
                versionDetail.setText("");
                issueDateDetail.setText("");
                MD5Detail.setText("");
                SHA1Detail.setText("");
                ifSignedDetail.setText("");
                IPSWDownloadLink = "";
                copyLink.setEnabled(false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        versionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                //Toast.makeText(MainActivity.this, "你点击的是:"+ devices.get(pos), 2000).show();
                String url = "https://api.ipsw.me/v4/ipsw/" + identifier.get(devicesSpinner.getSelectedItemPosition()) + "/" + buildID.get(pos);
                Log.d("url",url);
                httpGet(url);
                versionDetail.setText("");
                issueDateDetail.setText("");
                MD5Detail.setText("");
                SHA1Detail.setText("");
                ifSignedDetail.setText("");
                IPSWDownloadLink = "";
                copyLink.setEnabled(false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        copyLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("IPSWDownloadLink", IPSWDownloadLink);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "已复制下载链接", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void httpGet(final String url) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                OkHttpClient client = new OkHttpClient();//创建okhttp实例
                Request request = new Request.Builder()
                        .url(url).build();
                Call call = client.newCall(request);
                try {
                    Response response = call.execute();
                    if (response.isSuccessful()) {
                        Message msg = Message.obtain();
                        msg.obj = response.body().string();

                        if (url.equals("https://api.ipsw.me/v4/devices")){
                            devicesHandler.sendMessage(msg);
                        }else if (url.startsWith("https://api.ipsw.me/v4/ipsw/")){
                            detailHandler.sendMessage(msg);
                        }else {
                            versionsHandler.sendMessage(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    Handler devicesHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);
            devicesJson=(String)msg.obj;
            //Log.d("he",result);

            try {
                JSONArray array = new JSONArray(devicesJson);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject device = array.getJSONObject(i);
                    //Log.d("test","-----------------");
                    //Log.d("test","name= " + device.getString("name"));
                    //Log.d("test","identifier= " + device.getString("identifier"));
                    devices.add(device.getString("name"));
                    identifier.add(device.getString("identifier"));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.d("test",devices.toString());
            devicesAdapter.notifyDataSetChanged();
        }
    };

    Handler versionsHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            versionsJson=(String)msg.obj;
            //Log.d("url",versionsJson);

            try {
                JSONObject allVersionsJson = new JSONObject(versionsJson);
                JSONArray array = new JSONArray(allVersionsJson.getJSONArray("firmwares").toString());
                Log.d("url",array.toString());
                versions.clear();
                buildID.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject version = array.getJSONObject(i);
                    versions.add(version.getString("version"));
                    buildID.add(version.getString("buildid"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.d("test",devices.toString());
            Log.d("url",versions.toString());
            versionsAdapter.notifyDataSetChanged();
        }
    };

    Handler detailHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            detailJson=(String)msg.obj;

            try {
                JSONObject detail = new JSONObject(detailJson);
                Log.d("url",detail.getString("version"));
                TextView versionDetail = findViewById(R.id.versionDetail);
                versionDetail.setText(detail.getString("version"));

                TextView issueDateDetail = findViewById(R.id.issueDateDetail);
                issueDateDetail.setText(detail.getString("releasedate"));

                TextView MD5Detail = findViewById(R.id.MD5Detail);
                MD5Detail.setText(detail.getString("md5sum"));

                TextView SHA1Detail = findViewById(R.id.SHA1Detail);
                SHA1Detail.setText(detail.getString("sha1sum"));

                TextView ifSignedDetail = findViewById(R.id.ifSignedDetail);
                ifSignedDetail.setText(detail.getString("signed"));

                IPSWDownloadLink = detail.getString("url");

                Button copyLink = findViewById(R.id.copyLink);
                copyLink.setEnabled(true);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };
}