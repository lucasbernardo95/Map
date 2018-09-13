package com.dev.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private static final int LOCATION_REQUEST = 500;//caso dê erro na solicitação
    private List<LatLng> listaPontos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        listaPontos = new ArrayList<>();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapa = googleMap;
        mapa.getUiSettings().setZoomControlsEnabled(true);
        //pede a permissão em tempo de execução
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);

            return;
        }
        //permite ativar minha localização
        mapa.setMyLocationEnabled(true);
        //implementa o click 'longo' no mapa
        /*Ao clicar e segurar em um ponto, insere a primeira marca indicando o ponto de partida.
        * Ao clicar e segurar pela segunda vez, insere a segunda marca, que é o destino*/
        mapa.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //redefinir marcador quando houverem dois pontos
                if (listaPontos.size() == 2 ){
                    listaPontos.clear(); //limpa a lista para evitar pegar lixo de pontos
                    mapa.clear(); //limpa também os pontos do mapa
                }

                //salva o primeiro ponto selecionado
                listaPontos.add(latLng);
                //Cria a marca
                MarkerOptions marca = new MarkerOptions();
                marca.position(latLng); //cria uma marca no ponto selecionado

                if (listaPontos.size() == 1){
                    //Adiciona a primeira marca no mapa
                    marca.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));////insere uma bolinha da cor verde
                } else {
                    //adiciona a segunda marca no mapa com a cor vermelha
                    marca.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                //insere os dois pontos no mapa
                mapa.addMarker(marca);

                //requisição para obter o  código de direção
                if (listaPontos.size() == 2){
                    //cria uma URL de requisição para os dois pontos marcados 'endereço no mapa' entre os dois pontos criados
                    String url = getRequestURL(listaPontos.get(0), listaPontos.get(1));
                    TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                    taskRequestDirections.execute(url);
                }

            }
        });
    }

    private String getRequestURL(LatLng origem, LatLng dest) {
        //Valor da origem
        String val_org = "origin=" + origem.latitude +","+origem.longitude;
        //Valor do destino
        String val_dest = "destination=" + dest.latitude +","+dest.longitude;
        //Valor o valor permitido do sensor
        String sensor = "sensor=false";
        //Modo para encontrar a direção
        String mode = "mode=driving";
        //Constre o parâmetro completo
        String param = val_org + "&" + val_dest + "&" + sensor + "&"+ mode;
        //Formato da saída em JSON
        String output = "json";
        //Cria a URL de requisição e retorna
        return "http://maps.googleapis.com/maps/api/directions/" + output + "?" + param;
    }

    //método para pegar a direção
    private String requestDirection (String reqURL) throws IOException {
        String response = "";
        InputStream input = null;
        HttpURLConnection urlConnection = null;
        try {

            URL url = new URL(reqURL); //Cria um objeto URL com a url de requisição dos pontos
            urlConnection = (HttpURLConnection) url.openConnection(); //abre a conexão
            urlConnection.connect(); //conecta

            //Pega o resultado da requisição
            input = urlConnection.getInputStream();
            InputStreamReader streamReader = new InputStreamReader(input);
            BufferedReader bufferedReader = new BufferedReader(streamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while ( (line = bufferedReader.readLine()) != null){ //Lê cada uma das linhas do conteúdo recebido da requisição
                stringBuffer.append(line); //adiciona a linha lida ao stringBuffer
            }

            //atribui o que foi lido à string de resposta
            response = stringBuffer.toString();
            bufferedReader.close(); //fecha para evitar dar algum erro
            streamReader.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null){
                input.close();
            }
            urlConnection.disconnect();//encerra a conexão
        }
        return response;
    }

    //analisa se a permição foi concebida
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mapa.setMyLocationEnabled(true);
                }
                break;
        }
    }

    //Cria um AsyncTask para chamar a requisição de direção
    public class TaskRequestDirections extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirection(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return  responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>> > {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get list route and display it into the map

            ArrayList points = null;

            PolylineOptions polylineOptions = null;

            for (List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for (HashMap<String, String> point : path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }

            if (polylineOptions!=null) {
                mapa.addPolyline(polylineOptions);
            } else {
                Toast.makeText(getApplicationContext(), "Direction not found!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    //Localização estática
//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//        mMap = googleMap;
//
//        try {
//            // Customise the styling of the base map using a JSON object defined
//            // in a raw resource file.
//            boolean success = googleMap.setMapStyle(
//                    MapStyleOptions.loadRawResourceStyle(
//                            this, R.raw.mapstyle));
//
//            if (!success) {
//                Log.e("teste", "Style parsing failed.");
//            }
//        } catch (Resources.NotFoundException e) {
//            Log.e("teste", "Can't find style. Error: ", e);
//        }
//
//        // Add a marker in Sydney and move the camera
//        //objeto latitude e longetude, oassa os parâmetros que quer e seta no mapa
//        LatLng sydney = new LatLng(-5.8499785, -35.3254218);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
//    }
}
