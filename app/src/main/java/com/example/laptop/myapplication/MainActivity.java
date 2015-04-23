package com.example.laptop.myapplication;

//import android.support.v7.app.ActionBarActivity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.provider.*;
import android.content.*;

//public class MainActivity extends ActionBarActivity {
public class MainActivity extends Activity {

    private SmsService sr;
    private boolean m_bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Launch the SmsService, which runs in the background and allows for a persistent broadcast receiver
        Intent intent = new Intent(this, SmsService.class);
        bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
    }

    /*
    In order to bind a service (which allows it to communicate with other components of the application rather
    than running in isolation, you need to pass a ServiceConnection. When you call bindService the service
    will be created and it will attempt to bind it to the component that created it. If successful, it'll
    call the onServiceConnected function. This allows the calling component to get an object that
    corresponds to the newly created Service. setCallingActivity is called so that the BroadcastReceiver
    has access to the MainActivity, allowing it to change the text on the calling Activity.
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

    public void onButtonClick(View view)
    {
        TextView tv = (TextView) findViewById(R.id.text);
        Button b = (Button) findViewById(R.id.button);
        b.setText("I lied");
    }

    public void changeText(String s)
    {
        TextView tv = (TextView) findViewById(R.id.text);
        tv.setText(s);
    }

    /*
    When an application fully closes (by pressing "back"), all of the lifecycle methods will be called,
    including onStop. However, when the application is put into a suspended state (pressing "home",
    changing the orientation, turning off the screen), some of the termination methods including
    onStop will be called. If we want the service to stay alive during suspensions, move the following
    to onDestroy.
     */
    @Override
    protected void onStop()
    {
        if(m_bound)
        {
            unbindService(mConnection);
            m_bound = false;
        }
        TextView tv = (TextView) findViewById(R.id.text);
        tv.setText("Service stopped. Close the app (via the back button rather than the home button) and restart.");
        super.onStop();
    }
}
