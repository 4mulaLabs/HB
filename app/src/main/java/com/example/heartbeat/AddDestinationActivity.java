package com.example.heartbeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.InetAddress;

import kotlin.UByte;

public class AddDestinationActivity extends AppCompatActivity {
    EditText ipEditText;
    Toolbar toolbar;

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_destination_activity);

        ipEditText = (EditText) findViewById(R.id.ipEditText);
        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar.setTitle("Add Connection");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void onFinishedClick(View view) {
        Intent intent = new Intent();
        String ip = ipEditText.getText().toString();
        InetAddressValidator validator = InetAddressValidator.getInstance();
        if (validator.isValidInet4Address(ip)) {
            intent.putExtra("result", ip);
            setResult(0, intent);
            super.onBackPressed();
        } else {
            Toast.makeText(this, "Invalid IP Address", Toast.LENGTH_SHORT).show();
        }
    }
}