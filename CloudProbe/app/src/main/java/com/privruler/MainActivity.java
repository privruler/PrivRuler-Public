package com.privruler;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.privruler.alibaba.AlibabaClientFactory;
import com.privruler.aws.AWSClientFactory;
import com.privruler.azure.AzureClientFactory;

public class MainActivity extends Activity {
    public static String LOG_TAG = "PrivRulerLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        Button testAWSButton = (Button) this.findViewById(R.id.test_aws_bt);
        testAWSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AWSClientFactory fac = AWSClientFactory.getInstance(MainActivity.this);
                        fac.evaluateCredCapabilities();
                    }
                }).start();
            }
        });

        Button testAzureButton = (Button) this.findViewById(R.id.test_azure_bt);
        testAzureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AzureClientFactory fac = AzureClientFactory.getInstance(MainActivity.this);
                        fac.evaluateCredCapabilities();
                    }
                }).start();
            }
        });

        Button testAlibabaButton = (Button) this.findViewById(R.id.test_alibaba_bt);
        testAlibabaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AlibabaClientFactory fac = AlibabaClientFactory.getInstance(MainActivity.this);
                        fac.evaluateCredCapabilities();
                    }
                }).start();
            }
        });
    }
}