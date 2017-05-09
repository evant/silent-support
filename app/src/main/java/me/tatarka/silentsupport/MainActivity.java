package me.tatarka.silentsupport;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.text)).setCompoundDrawablesWithIntrinsicBounds(
                getDrawable(R.drawable.ic_action_name),
                null,
                null,
                null
        );

//        createConfigurationContext(getResources().getConfiguration());
    }
}
