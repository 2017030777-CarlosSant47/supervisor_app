package com.carlossant47.testingfunctions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.carlossant47.testingfunctions.Clases.Fecha;
import com.carlossant47.testingfunctions.Clases.ListInstituciones;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private LinearLayout secList, vNoSup, linNoSupervicion;
    public static String action = "action";
    ListView lv;
    private static final int MY_ALLPERMISSIONS = 3;
    TextView lbNombe, lbCorreo;
    DBSupervicion supervicion = new DBSupervicion(MenuActivity.this);
    Permissions.URLDEFINIIONSSERVER urlsData;
    private BottomNavigationView mNavigationMenu;
    private LinearLayout mLinSupervicion;
    private TextView mLbInstitucion;
    private TextView mLbFecha;
    private LinearLayout mLinSupervicionActual;
    private TextView mLbDomicilio;
    private LinearLayout mLinNoSupervicion;
    private TextView mLbZona;
    private Button mBtnStart;

    {
        urlsData = new Permissions.URLDEFINIIONSSERVER();
    }

    SharedPreferences pref;
    ArrayList<ListInstituciones> listSupervision = new ArrayList<>();
    ListInstituciones supervicionHoy = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Calendar calendar = Calendar.getInstance();
        Log.w("DIA", String.valueOf(calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.MONTH)));
        pref = getSharedPreferences(Permissions.fileconfig, MODE_PRIVATE);
        lv = findViewById(R.id.listInstituciones);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        secList = findViewById(R.id.secList);
        vNoSup = findViewById(R.id.vNoSup);
        Permissions();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        //deterScreenSize();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mNavigationMenu = findViewById(R.id.NavigationMenu);
        mNavigationMenu.setOnNavigationItemSelectedListener(navigationBottom);
        mLinSupervicion = findViewById(R.id.linSupervicion);
        mLinSupervicion.setVisibility(View.VISIBLE);
        secList.setVisibility(View.GONE);
        lv.setVisibility(View.GONE);
        linNoSupervicion = findViewById(R.id.linNoSupervicion);
        setInformation(navigationView);
        mLbInstitucion = findViewById(R.id.lbInstitucion);
        mLbFecha = findViewById(R.id.lbFecha);
        mLinSupervicionActual = findViewById(R.id.linSupervicionActual);
        mLbDomicilio = findViewById(R.id.lbDomicilio);
        mLbZona = findViewById(R.id.lbZona);
        mBtnStart = findViewById(R.id.btnStart);
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Infraestructura.SupervisionInfo supervision = new Infraestructura.SupervisionInfo();
                Intent intent = new Intent(MenuActivity.this, Infraestructura.class);
                supervision.setIdInstitucion(supervicionHoy.getIdIntitucion());
                supervision.setIdSupervision(supervicionHoy.getId());
                supervision.setNombreInstitucion(supervicionHoy.getNombre());
                intent.putExtra("supervision", supervision);
                startActivity(intent);
            }
        });


        if (Config.getDowloadData(getApplicationContext()) == 1)//ESTE ES COMO CONFIGURACION
        {
            //EN CASO DE SER DESCARGADOS LOS SACARA DE SQLLITE PARA CONSULTA MAS RAPIDA
            Log.w("Datos", "Descargados");
            supervicion.openDatabase();
            listSupervision = supervicion.DataSupervisiones();
            supervicion.closeDatabase();
            Log.w("TAMANO ARR", String.valueOf(listSupervision.size()));
            AdapterInstASuper adaptador = new AdapterInstASuper(getApplicationContext(), listSupervision);
            lv.setAdapter(adaptador);
            supervicion.closeDatabase();
        } else {
            Log.w("Datos", "No Descargados");
            ViewDataSupervision();
            Thread rvoes = new Thread(new Runnable() {
                @Override
                public void run() {
                    consultRvoeAll();
                }
            });

            rvoes.run();

        }
        supervicionActual();
    }

    private void setInformation(NavigationView nav) {
        View v = nav.getHeaderView(0);
        lbCorreo = v.findViewById(R.id.lbCorreo);
        lbNombe = v.findViewById(R.id.lbNombre);
        Config config = new Config(MenuActivity.this);
        lbNombe.setText(config.getNombre());
        lbCorreo.setText(config.getEmail());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);

        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(MenuActivity.this, settings.class);
            int actions = 0;
            i.putExtra(action, actions);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_ALLPERMISSIONS:

                if (grantResults.length > 0) {
                    boolean CameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean ExternalStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (CameraPermission && ExternalStorage) {

                        Log.i("Permisos", "Habilitados");
                        if (Permissions.CrearCarpeta(getApplicationContext())) {
                            Log.w("Carpeta", "Creada");
                        }
                    } else {

                        Log.i("Permisos", "Denagados");
                    }
                }
        }
    }
    private BottomNavigationView.OnNavigationItemSelectedListener navigationBottom = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.btnActual:
                    mLinSupervicion.setVisibility(View.VISIBLE);
                    secList.setVisibility(View.GONE);
                    lv.setVisibility(View.GONE);
                    return true;
                case R.id.btnPendientes: {
                    ArrayList<ListInstituciones> listInstituciones = new ArrayList<>();
                    for (int x = 0; x < listSupervision.size(); x++) {
                        if (listSupervision.get(x).getStatus() == 1) {
                            listInstituciones.add(listSupervision.get(x));
                        }
                    }
                    AdapterInstASuper adaptador = new AdapterInstASuper(getApplicationContext(), listInstituciones);
                    lv.setAdapter(adaptador);
                    mLinSupervicion.setVisibility(View.GONE);
                    secList.setVisibility(View.VISIBLE);
                    lv.setVisibility(View.VISIBLE);
                    return true;
                }


                case R.id.btnCompletadas: {
                    ArrayList<ListInstituciones> listInstituciones = new ArrayList<>();
                    for (int x = 0; x < listSupervision.size(); x++) {
                        if (listSupervision.get(x).getStatus() == 0 || listSupervision.get(x).getStatus() == 2) {
                            listInstituciones.add(listSupervision.get(x));
                        }
                    }
                    AdapterInstASuper adaptador = new AdapterInstASuper(getApplicationContext(), listInstituciones);
                    lv.setAdapter(adaptador);
                    mLinSupervicion.setVisibility(View.GONE);
                    secList.setVisibility(View.VISIBLE);
                    lv.setVisibility(View.VISIBLE);
                    return true;
                }

            }
            return false;
        }
    };


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnlogout: {

                Permissions.alertDialogDesision(R.string.cerrar_s_mensaje, R.string.cerrar_sesion, MenuActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        DBSupervicion db = new DBSupervicion(getApplicationContext());
                        db.openDatabase();
                        db.deleteDataTable();
                        db.closeDatabase();
                        Config config = new Config(getApplicationContext());
                        config.LogutOption();
                        finish();
                        Intent i = new Intent(MenuActivity.this, LoginActivity.class);
                        startActivity(i);
                    }
                });
                break;
            }
            case R.id.btnconfig: {
                Intent intent = new Intent(MenuActivity.this, settings.class);
                startActivity(intent);
                break;
            }
            case R.id.btnPendientes: {
                Intent intent = new Intent(MenuActivity.this, Pendientes.class);
                startActivity(intent);
                break;
            }
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    class AdapterInstASuper extends ArrayAdapter<ListInstituciones> {
        private Context context;
        private ArrayList<ListInstituciones> objects;

        public AdapterInstASuper(Context contexto, ArrayList<ListInstituciones> objects) {
            super(contexto, R.layout.instituciones_listlayout, objects);
            this.context = contexto;
            this.objects = objects;
        }


        @SuppressLint("SetTextI18n")
        public View getView(final int position, View convertview, ViewGroup viewGroup) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(R.layout.instituciones_listlayout, null);
            final TextView lbescuela = view.findViewById(R.id.lbescuela);
            final LinearLayout btnLisist = view.findViewById(R.id.btnList);
            final TextView lbfecha = view.findViewById(R.id.lbfecha), txtstatus = view.findViewById(R.id.txtstatus);
            final TextView lbzona = view.findViewById(R.id.lbzona);
            final Calendar calendar = Calendar.getInstance();
            if (objects.get(position).getStatus() == 1) {
                if (objects.get(position).getFecha().getMonth() == calendar.get(Calendar.MONTH) + 1 && objects.get(position).getFecha().getYear() == calendar.get(Calendar.YEAR) &&
                        objects.get(position).getFecha().getDay() == calendar.get(Calendar.DAY_OF_MONTH)) {
                    lbfecha.setText("Programada para HOY");
                    txtstatus.setText("PENDIENTE");
                } else if (objects.get(position).getFecha().getMonth() < calendar.get(Calendar.MONTH) + 1 || objects.get(position).getFecha().getYear() < calendar.get(Calendar.YEAR) ||
                        objects.get(position).getFecha().getDay() < calendar.get(Calendar.DAY_OF_MONTH)) {
                    lbfecha.setText("Programada para el " + objects.get(position).getFecha().returnFecha());
                    txtstatus.setText("ATRASADA");
                } else {
                    lbfecha.setText("Programada para el " + objects.get(position).getFecha().returnFecha());
                    txtstatus.setText("PENDIENTE");
                }

            } else if (objects.get(position).getStatus() == 0 || objects.get(position).getStatus() == 2) {
                if(objects.get(position).getStatus() == 2)
                {
                    txtstatus.setText("COMPLETADA");
                }
                else
                {
                    txtstatus.setText("COMPLETADA - PENDIENTE DE SUBIR");
                }

            }
            //2000-10-05
            lbzona.setText("Zona " + objects.get(position).getZona());
            lbescuela.setText(objects.get(position).getNombre());
            btnLisist.setBackgroundColor(objects.get(position).getColor());
            btnLisist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (objects.get(position).getStatus() == 2 || objects.get(position).getStatus() == 0) {
                        Toast.makeText(getApplicationContext(), "Supervision Completada", Toast.LENGTH_SHORT).show();
                    } else if (objects.get(position).getFecha().getMonth() >= (calendar.get(Calendar.MONTH) + 1) && objects.get(position).getFecha().getYear() >= calendar.get(Calendar.YEAR) &&
                            objects.get(position).getFecha().getDay() > calendar.get(Calendar.DAY_OF_MONTH)) {
                        Permissions.alertDialog("Esta supervision no esta programada para el dia de hoy", "Error", MenuActivity.this);
                    } else {
                        Permissions.alertDialogDesision(objects.get(position).getInformacion(),
                                getResources().getString(R.string.infoinstitucion),
                                MenuActivity.this,
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        Infraestructura.SupervisionInfo supervision = new Infraestructura.SupervisionInfo();
                                        Intent intent = new Intent(MenuActivity.this, Infraestructura.class);
                                        supervision.setIdInstitucion(objects.get(position).getIdIntitucion());
                                        supervision.setIdSupervision(objects.get(position).getId());
                                        supervision.setNombreInstitucion(objects.get(position).getNombre());
                                        Log.w("NO", supervision.getNombreInstitucion());
                                        intent.putExtra("supervision", supervision);
                                        startActivity(intent);
                                    }
                                });
                    }

                }
            });
            return view;
        }


    }

    private void Permissions() {
        if (Permissions.chekStoragePermission(getApplicationContext()) && Permissions.checkCameraPermission(getApplicationContext())) {
            if (Permissions.CrearCarpeta(getApplicationContext())) {
                Log.w("Si", "Permisos");
            }
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                ActivityCompat.requestPermissions(MenuActivity.this, new String[]
                        {
                                CAMERA,
                                WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE
                        }, MY_ALLPERMISSIONS);
            }
        }
    }

    @Override //STE METODO DETECTA CUANDO LA ACTIVIDAD QUE SE ABRIO RENCIENTE REVUELDA DATOS
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(data != null)
        {
            supervicion.openDatabase();
            listSupervision = supervicion.DataSupervisiones();
            supervicion.closeDatabase();
            AdapterInstASuper adaptador = new AdapterInstASuper(getApplicationContext(), listSupervision);
            lv.setAdapter(adaptador);
            supervicion.closeDatabase();
            supervicionActual();
        }
    }

    private void ViewDataSupervision() {
        final String[] colors;
        colors = new String[4];
        colors[0] = "#0B8043";
        colors[1] = "#4285F4";
        colors[2] = "#4B4B4B";
        colors[3] = "#4A148C";
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("IDSUP", String.valueOf(Config.getIdSupervisor(getApplicationContext())));
        client.post(Config.concatUrlConection(Permissions.urlDataSupervision, getApplicationContext()), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headerses, JSONArray responseArray) {
                try {
                    int co = 0;
                    if (responseArray.length() == 0) {
                        vNoSup.setVisibility(View.VISIBLE);
                    } else {
                        for (int i = 0; i < responseArray.length(); i++) {
                            JSONObject data = responseArray.getJSONObject(i);
                            ListInstituciones ListSuper = new ListInstituciones();
                            ListSuper.setNombre(data.getString(urlsData.NombreEscuela));
                            ListSuper.setId(Integer.valueOf(data.getString(urlsData.id)));
                            ListSuper.setZona(data.getString(urlsData.Zona));
                            ListSuper.setIdIntitucion((Integer.valueOf(data.getString(urlsData.IDINSTITITUCION))));
                            ListSuper.setFecha(new Fecha(data.getString(urlsData.fechaSupernvision)));
                            //ListSuper.numRVOE = (data.getInt(urlsData.RVOE));
                            ListSuper.setStatus(Integer.parseInt(data.getString("estado")));
                            ListSuper.setInformacion(data.getString(urlsData.Direccion));
                            int color = Color.parseColor(colors[co]);
                            ListSuper.setColor(color);
                            listSupervision.add(i, ListSuper);
                            co = co + 1;
                            if (co >= 4) {
                                co = 0;
                            }
                        }
                        supervicion.openDatabase();
                        supervicion.insertSupervisiones(listSupervision);
                        supervicion.closeDatabase();
                        AdapterInstASuper adaptador = new AdapterInstASuper(getApplicationContext(), listSupervision);
                        lv.setAdapter(adaptador);
                        Config config = new Config(getApplicationContext());
                        config.setDowloadData(1);
                        secList.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable
                    throwable) {
                Log.i("Error", responseString);
            }
        });


    }

    public void consultRvoeAll() {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        Config config = new Config(getApplicationContext());

        params.put("email", config.getEmail());
        client.post(Config.concatUrlConection("ConData/OnConsultRVOEAll", getApplicationContext()), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headerses, JSONArray responseArray) {
                DBSupervicion dbSupervicion = new DBSupervicion(MenuActivity.this);
                dbSupervicion.openDatabase();
                dbSupervicion.insertarRvoes(responseArray);
                dbSupervicion.closeDatabase();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable
                    throwable) {
                Log.i("Error", responseString);
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void supervicionActual() {
        final Calendar calendar = Calendar.getInstance();
        int x = 0;
        for (x = 0; x < listSupervision.size(); x++) {
            if (listSupervision.get(x).getFecha().getMonth() == (calendar.get(Calendar.MONTH) + 1) && listSupervision.get(x).getFecha().getDay() == calendar.get(Calendar.DAY_OF_MONTH)
                    && listSupervision.get(x).getFecha().getYear() == calendar.get(Calendar.YEAR) && listSupervision.get(x).getStatus() == 1) {
                supervicionHoy = new ListInstituciones(listSupervision.get(x));
                mLbDomicilio.setText(supervicionHoy.getInformacion());
                mLbFecha.setText("nin");
                mLbInstitucion.setText(supervicionHoy.getNombre());
                linNoSupervicion.setVisibility(View.GONE);
                mLinSupervicionActual.setVisibility(View.VISIBLE);
                mLbZona.setText("Zona " + supervicionHoy.getZona());
                Log.w("Ecnontra", "1");
                break;
            }
        }
    }




}
