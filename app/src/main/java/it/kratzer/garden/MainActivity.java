package it.kratzer.garden;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener{

    private ToggleButton light;
    private boolean light_stat;
    private ToggleButton socket;
    private boolean socket_stat;
    private EditText min;
    private int minute = 0;

    private static String HOST = "http://ESP-garden.fritz.box";
    //private static String HOST = "http://192.168.150.28";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        light = (ToggleButton) findViewById(R.id.toggleButton_light);
        light.setOnCheckedChangeListener(this);

        socket = (ToggleButton) findViewById(R.id.toggleButton_socket);
        socket.setOnCheckedChangeListener(this);

        min = (EditText) findViewById(R.id.editText_min);

        this.updateApp();
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if(isChecked){

            int val = Integer.parseInt(min.getText().toString());
            if (val != minute) {
                minute = val;
                new RetrieveTask("timeout") {
                    protected void onPostExecute(String res) {
                        System.out.println("timeout " + res);
                    }
                }.execute(HOST + "/timeout", "val," +min.getText().toString() );
            }
        }

        if( buttonView.getId() == socket.getId() ){
            if( isChecked && !socket_stat ){
                new RetrieveTask( "socket" ) {
                    protected void onPostExecute(String res) {
                        System.out.println( "socket "+res );
                        socket.setText( "socket on");
                        socket_stat = true;
                    }
                }.execute(HOST+"/pin", "pin,12", "val,1");

            }else if(!isChecked && socket_stat){
                new RetrieveTask( "socket" ) {
                    protected void onPostExecute(String res) {
                        System.out.println( "socket "+res );
                        socket.setText( "socket off");
                        socket_stat = false;
                    }
                }.execute(HOST+"/pin", "pin,12", "val,0");
            }
        }else if(buttonView.getId() == light.getId() ) {
            if (isChecked && !light_stat) {
                new RetrieveTask("light") {
                    protected void onPostExecute(String res) {
                        System.out.println("light " + res);
                        light.setText("light on");
                        light_stat = true;
                    }
                }.execute(HOST + "/pin", "pin,14", "val,1");
            } else if(!isChecked && light_stat){
                new RetrieveTask("light") {
                    protected void onPostExecute(String res) {
                        System.out.println("light " + res);
                        light.setText("light off");
                        light_stat = false;
                    }
                }.execute(HOST + "/pin", "pin,14", "val,0");
            }
        }
    }

    public void updateTimeout(){
        new RetrieveTask( "min" ) {
            protected void onPostExecute(String res) {
                if(res != null) {
                    min.setText(res.trim());
                    minute = Integer.parseInt(res.trim());
                }
            }
        }.execute( HOST+"/timeout" );
    }

    public void updateApp(){

        updateTimeout();

        new RetrieveTask( "socket" ) {
            protected void onPostExecute(String res) {
                socket.setText( "socket "+res.trim());
                if(res.contains( "on" ) ) {
                    socket_stat = true;
                    socket.setChecked(true);
                }else{
                    socket_stat = false;
                    socket.setChecked(false);
                }
            }
        }.execute(HOST+"/pin?pin=12");

        new RetrieveTask( "light" ) {
            protected void onPostExecute(String res) {
                light.setText( "light "+res.trim() );
                if(res.contains( "on" ) ) {
                    light_stat = true;
                    light.setChecked(true);
                }else{
                    light_stat = false;
                    light.setChecked(false);
                }
            }
        }.execute(HOST+"/pin?pin=14");

    }

    class RetrieveTask extends AsyncTask<String, Void, String> {

        private Exception exception;
        private String type;

        public RetrieveTask( String type ){
            super();
            this.type = type;
        }

        protected String doInBackground(String ... urlstr) {

            URL url;
            String res = null;

            HttpURLConnection urlConnection = null;
            try {
                url = new URL(urlstr[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                if( urlstr.length > 1 ){
                    urlConnection.setRequestMethod("POST");
                    ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                    for( int i=1; i<urlstr.length; i++ ) {
                        String pair[] = urlstr[i].split(",");
                        params.add(new BasicNameValuePair(pair[0], pair[1]));
                    }
                    OutputStream os = urlConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(getQuery(params));
                    writer.flush();
                    writer.close();
                }
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader br = new BufferedReader( new InputStreamReader(in) );
                res = br.readLine();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }
            return res;

        }

        private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
        {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (NameValuePair pair : params)
            {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            }

            return result.toString();
        }

        protected void onPostExecute(String res) {
        }

    }
}
