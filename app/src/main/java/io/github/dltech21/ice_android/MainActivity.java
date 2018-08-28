package io.github.dltech21.ice_android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import IceInternal.Ex;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            IceClientUtil.getServicePrx(this, Class.forName("d"));
        } catch (Exception e) {

        }
    }
}
