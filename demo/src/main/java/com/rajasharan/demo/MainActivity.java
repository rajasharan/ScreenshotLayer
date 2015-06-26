package com.rajasharan.demo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.rajasharan.layout.ScreenshotLayer;

public class MainActivity extends ActionBarActivity {
    private ScreenshotLayer root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        root = (ScreenshotLayer) findViewById(R.id.layer);
        root.setScreenshotListener(new ScreenshotLayer.ScreenshotListener() {
            @Override
            public void onScreenshotSaved(String path) {
                Toast.makeText(MainActivity.this, "Screenshot saved in " + path, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScreenshotError(String reason) {
                Toast.makeText(MainActivity.this, "Screenshot failed: " + reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
