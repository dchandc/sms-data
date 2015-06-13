package com.example.laptop.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private SmsService sr;
    private boolean m_bound = false;
    TextView tv;
    Context context;
    public String filterNumber = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        // Launch the SmsService, which runs in the background and allows for a persistent
        // broadcast receiver.
        Intent intent = new Intent(this, SmsService.class);
        bindService(intent, mConnection, Service.BIND_AUTO_CREATE);

        tv = (TextView) findViewById(R.id.text_status);
        tv.setMovementMethod(new ScrollingMovementMethod());

        Button b_debug = (Button) findViewById(R.id.button_debug);
        b_debug.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Send test SMS message");
                builder.setMessage("Enter phone number");
                final EditText eText = new EditText(context);
                eText.setInputType(InputType.TYPE_CLASS_TEXT);
                eText.setTextColor(Color.rgb(0, 0, 0));
                builder.setView(eText);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String str = eText.getText().toString();
                        if (str != null && str.matches("\\d+(\\.\\d+)?")) {
                            new SmsTask(context, tv, str).execute();
                        } else {
                            Toast.makeText(context, "Invalid phone number",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        Button b_filter = (Button) findViewById(R.id.button_filter);
        b_debug.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Set filter");
                builder.setMessage("Enter phone number");
                final EditText eText = new EditText(context);
                eText.setInputType(InputType.TYPE_CLASS_TEXT);
                eText.setTextColor(Color.rgb(0, 0, 0));
                builder.setView(eText);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String str = eText.getText().toString();
                        if (str != null && str.matches("\\d+(\\.\\d+)?")) {
                            filterNumber = str;
                        } else {
                            Toast.makeText(context, "Invalid phone number",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        appendText("Listening for SMS messages...\n");

        Button b_clear = (Button) findViewById(R.id.button_clear);
        b_clear.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                setText("Listening for SMS messages...\n");
            }
        });
    }

    /**
     * In order to bind a service (which allows it to communicate with other components of the
     * application rather than running in isolation, you need to pass a ServiceConnection. When
     * you call bindService the service will be created and it will attempt to bind it to the
     * component that created it. If successful, it'll call the onServiceConnected function.
     * This allows the calling component to get an object that corresponds to the newly created
     * Service. setCallingActivity is called so that the BroadcastReceiver has access to the
     * MainActivity, allowing it to change the text on the calling Activity.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            SmsService.SmsBinder binder = (SmsService.SmsBinder) service;
            sr = binder.getService();
            sr.setCallingActivity(MainActivity.this);
            m_bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            m_bound = false;
        }
    };

    public void setText(String s)
    {
        TextView tv = (TextView) findViewById(R.id.text_status);
        tv.setText(s);
    }

    public void appendText(String s)
    {
        TextView tv = (TextView) findViewById(R.id.text_status);
        tv.append(s);

        final Layout layout = tv.getLayout();
        if(layout != null) {
            int scrollDelta = layout.getLineBottom(tv.getLineCount() - 1)
                    - tv.getScrollY() - tv.getHeight();
            if (scrollDelta > 0)
                tv.scrollBy(0, scrollDelta);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu. This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * When an application fully closes (by pressing "back"), all of the lifecycle methods will
     * be called, including onStop. However, when the application is put into a suspended state
     * (pressing "home", changing the orientation, turning off the screen), some of the termination
     * methods including onStop will be called. If we want the service to stay alive during
     * suspensions, move the following to onDestroy.
     */
    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        if (m_bound) {
            unbindService(mConnection);
            m_bound = false;
        }
        TextView tv = (TextView) findViewById(R.id.text_status);
        tv.setText("Service stopped. Close the app and restart.");
        super.onDestroy();
    }
}