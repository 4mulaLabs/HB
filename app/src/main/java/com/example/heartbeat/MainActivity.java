package com.example.heartbeat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    static final int SECOND = 1000;
    static final int CHECKING_DELAY = 120 * SECOND;
    static final int REQUEST_PORT = 9000;
    static final String TAG = "Main Activity";
    FloatingActionButton addButton;
    RecyclerView destinationsRV;
    Button fileButton;
    TextView fileTV;
    Toolbar toolbar;
    DestinationAdapter adapter;
    List<Destination> destinations = new ArrayList<Destination>();
    HttpClient httpClient = new HttpClient();
    Handler handler = new Handler();
    Runnable runnable;
    String requestContent = "{}";
    SharedPreferences sharedPreferences;
    String preference;
    SharedPreferences.Editor prefEditor;
    final String REQUEST_CONTENT_KEY = "requestContent";
    final String CHOSEN_FILE_KEY = "chosenFile";
    final String IPS_KEY = "ips";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(preference, Context.MODE_PRIVATE);
        prefEditor = sharedPreferences.edit();

        destinationsRV = (RecyclerView) findViewById(R.id.ipAddressRV);
        addButton = (FloatingActionButton) findViewById(R.id.addButton);
        fileButton = (Button) findViewById(R.id.fileButton);
        fileTV = (TextView) findViewById(R.id.fileTextView);
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar.setTitle("Connections");

        setSupportActionBar(toolbar);

        adapter = new DestinationAdapter(this, destinations);
        destinationsRV.setAdapter(adapter);
        destinationsRV.setLayoutManager(new LinearLayoutManager(this));

        adapter.setOnItemClickListener(new DestinationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                destinations.remove(position);
                adapter.notifyItemRemoved(position);
            }
        });

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
        if (sharedPreferences.contains(REQUEST_CONTENT_KEY)) {
            requestContent = sharedPreferences.getString(REQUEST_CONTENT_KEY, requestContent);
        }
        if (sharedPreferences.contains(CHOSEN_FILE_KEY)) {
            fileTV.setText(
                    sharedPreferences.getString(CHOSEN_FILE_KEY, fileTV.getText().toString()));
        }
        if (sharedPreferences.contains(IPS_KEY)) {
            for (String ip : sharedPreferences.getStringSet(IPS_KEY, new HashSet<>())) {
                destinations.add(new Destination(ip));
            }
        }
    }

    @Override
    protected void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, CHECKING_DELAY);
                for (int i = 0; i < destinations.size(); i++) {
                    checkConnection(i);
                }
            }
        }, CHECKING_DELAY);
        super.onResume();
    }

    public void checkConnection(int destinationIndex) {
        Destination destination = destinations.get(destinationIndex);
        Callback callback = new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, String.format("connection check failed for ip `%s`",
                                destination.getIpAddress()));
                        Log.d(TAG, e.toString());
                        destination.isUp = false;
                        adapter.notifyItemChanged(destinationIndex);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, String.format("Got status code `%s` from ip `%s`",
                                response.code(), destination.getIpAddress()));
                        destination.setIsUp(response.code() == 200);
                        adapter.notifyItemChanged(destinationIndex);
                    }
                });
            }
        };
        String url = String.format("https://%s:%s/",
                destination.getIpAddress(), REQUEST_PORT);
        httpClient.send(url, requestContent, callback);
    }

    ActivityResultLauncher<Intent> addDestinationActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == 0) {
                        Intent intent = result.getData();
                        if (intent != null) {
                            String ip = intent.getStringExtra("result");
                            Destination newDestination = new Destination(ip);
                            destinations.add(newDestination);
                            prefEditor.putStringSet(IPS_KEY, new HashSet<>(getIps()));
                            prefEditor.apply();
                            adapter.notifyItemInserted(destinations.size());
                        }
                    }
                }
            });

    public void onAddClick(View view) {
        Intent intent = new Intent(this, AddDestinationActivity.class);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        addDestinationActivityLauncher.launch(intent);
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String res = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = this.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    res = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
            if (res == null) {
                res = uri.getPath();
                int cutt = res.lastIndexOf('/');
                if (cutt != -1) {
                    res = res.substring(cutt + 1);

                }
            }
        }
        return res;
    }

    ActivityResultLauncher<Intent> openFileActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "chosen file");
                    if (result.getData() != null) {
                        Uri uri = result.getData().getData();

                        fileTV.setText(getFileName(uri));
                        prefEditor.putString(CHOSEN_FILE_KEY, getFileName(uri));
                        prefEditor.apply();
                        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                            InputStreamReader streamReader = new InputStreamReader(inputStream);
                            BufferedReader bufferReader = new BufferedReader(streamReader);
                            String line;
                            StringBuilder content = new StringBuilder();
                            while ((line = bufferReader.readLine()) != null) {
                                content.append(line);
                            }

                            requestContent = content.toString();
                            prefEditor.putString(REQUEST_CONTENT_KEY, requestContent);
                            prefEditor.apply();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
    );

    public void onChooseFileClick(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        openFileActivityLauncher.launch(intent);
    }

    private ArrayList<String> getIps() {
        ArrayList<String> ips = new ArrayList<>();
        for (Destination destination : destinations) {
            ips.add(destination.getIpAddress());
        }
        return ips;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(REQUEST_CONTENT_KEY, requestContent);
        outState.putCharSequence(CHOSEN_FILE_KEY, fileTV.getText());

        ArrayList<String> ips = getIps();
        outState.putStringArrayList(IPS_KEY, ips);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        requestContent = savedInstanceState.getString(REQUEST_CONTENT_KEY);
        fileTV.setText(savedInstanceState.getCharSequence(CHOSEN_FILE_KEY));

        for (String ip : savedInstanceState.getStringArrayList(IPS_KEY)) {
            destinations.add(new Destination(ip));
        }
        super.onRestoreInstanceState(savedInstanceState);
    }
}