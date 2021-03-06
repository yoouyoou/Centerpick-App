package ddwucom.contest.centerpick.activity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.os.Bundle;

import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;
import android.Manifest;
import android.location.LocationManager;
import android.content.DialogInterface;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.odsay.odsayandroidsdk.API;
import com.odsay.odsayandroidsdk.ODsayData;
import com.odsay.odsayandroidsdk.ODsayService;
import com.odsay.odsayandroidsdk.OnResultCallbackListener;

import ddwucom.contest.centerpick.model.category_search.CategoryResult;
import ddwucom.contest.centerpick.model.category_search.Document;
import ddwucom.contest.centerpick.R;
import ddwucom.contest.centerpick.api.ApiClient;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import ddwucom.contest.centerpick.api.ApiInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener,
        MapReverseGeoCoder.ReverseGeoCodingResultListener, MapView.MapViewEventListener, MapView.POIItemEventListener,
        MapView.OpenAPIKeyAuthenticationResultListener, View.OnClickListener {
    final static String TAG = "MapTAG";
    private MapView mapView;

    private ODsayService odsayService;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    public static MapPOIItem.CalloutBalloonButtonType MainButton;


    final int REQ_CODE = 100;
    final int REQ_USER_CODE = 200;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};


    public static ArrayList<String> latitude = new ArrayList<>(); //?????? ?????? ?????? ?????????
    public static ArrayList<String> longitude = new ArrayList<>(); //?????? ?????? ?????? ?????????

    public static ArrayList<String> remove_latitude = new ArrayList<>(); //????????? ??????
    public static ArrayList<String> remove_longitude = new ArrayList<>(); //????????? ??????



    public static double touch_x; //?????? ?????? ->timeActivity
    public static double touch_y; //?????? ?????? ->timeActivity

    //Double??? ?????????
    ArrayList<Double> lat = new ArrayList<>();
    ArrayList<Double> lon = new ArrayList<>();

    public static ArrayList<String> pick_addressList = new ArrayList<>(); //?????? ?????? ??????
    public static ArrayList<String> pick_placeNameList = new ArrayList<>();

    public static ArrayList<MapPOIItem> pois = new ArrayList<>();

    public static String pick_address; //????????? ??????
    public static String pick_placeName; //????????? ?????? ??????

    public static ArrayList<Integer> remove_pos = new ArrayList<>(); //?????? pos???

    static String center_pick = null;

    public static ArrayList<Integer> time = new ArrayList<>(); //???????????? ?????????
    static int min = 100000;

    //??????'s turn
    private FloatingActionButton fab, fab1, fab2, fab3;
    private Animation fab_open, fab_close;
    private Boolean isFabOpen = false;

    public static int count = 1;

    ArrayList<Document> bigMartList = new ArrayList<>(); //???????????? MT1
    ArrayList<Document> gs24List = new ArrayList<>(); //????????? CS2
    ArrayList<Document> subwayList = new ArrayList<>(); //????????? SW8
    ArrayList<Document> bankList = new ArrayList<>(); //?????? BK9
    ArrayList<Document> cafeList = new ArrayList<>(); //??????
    ArrayList<Document> restaurantList = new ArrayList<>(); //????????? FD6

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Context context = null;

        getHashKey();

        // ????????? ??????, Key ?????? ???????????? ?????? ??????
        odsayService = ODsayService.init(this, "UQpy5z5jYlkG7opjycDDO26k1WqH5NwUshX6iB5fir8");
        // ?????? ?????? ?????? ??????(??????(???), default : 5???)
        odsayService.setReadTimeout(5000);
        // ????????? ?????? ?????? ??????(??????(???), default : 5???)
        odsayService.setConnectionTimeout(5000);

        mapView = (MapView)findViewById(R.id.map_view);
        mapView.setCurrentLocationEventListener(this);

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }
//?????? ??????
        initView();

    }

    private void initView() {

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        fab = findViewById(R.id.fab);
        fab1 = findViewById(R.id.fab1);
        fab2 = findViewById(R.id.fab2);
        fab3 = findViewById(R.id.fab3);

//        //???????????????
        fab.setOnClickListener(this);
        fab1.setOnClickListener(this);
        fab2.setOnClickListener(this);
        fab3.setOnClickListener(this);

    }

    protected void onDestroy(){
        super.onDestroy();
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        mapView.setShowCurrentLocationMarker(false);
    }

    private void getHashKey(){    //????????? ??????
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }


    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        mapReverseGeoCoder.toString();

    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {

    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    /*
    ActivityCompat.requestPermissions ????????? ????????? ????????? ?????? ???????????? ?????????
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length){
            //?????? ????????? PERMISSIONS_REQUEST_CODE ??????, ????????? ????????? ???????????? ??????

            boolean check_result = true;

            //?????? ????????? ??????????????? ??????
            for (int result : grantResults){
                if (result != PackageManager.PERMISSION_GRANTED){
                    check_result = false;
                    break;
                }
            }

            if (check_result){
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
            }
            else{
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])){
                    Toast.makeText(MapActivity.this, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????.",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                else{
                    Toast.makeText(MapActivity.this, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    void checkRunTimePermission(){
        //????????? ????????? ??????

        //1. ?????? ????????? ????????? ????????? ??????
        int hasFindLocationPermission = ContextCompat.checkSelfPermission(MapActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFindLocationPermission == PackageManager.PERMISSION_GRANTED){
            //?????? ???????????? ????????? ?????????

            //?????? ??? ???????????? ??????
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        }
        else{
            //????????? ?????? ????????? ??? ????????? ????????? ?????? ??????

            //????????? ??? ?????? ??????
            if (ActivityCompat.shouldShowRequestPermissionRationale(MapActivity.this, REQUIRED_PERMISSIONS[0])){
                Toast.makeText(MapActivity.this, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.",
                        Toast.LENGTH_LONG).show();

                ActivityCompat.requestPermissions(MapActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
            else{
                //????????? ?????? ??? ??? ?????? ??????
                ActivityCompat.requestPermissions(MapActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ???????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int size = latitude.size();
        double lat;
        double lon;

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:

                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        checkRunTimePermission();
                        return;
                    }
                }
                break;

            case REQ_CODE:
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
                //????????? ?????? ???, ?????? ???????????? ?????? ?????? ?????? ?????????

                switch(resultCode){
                    case RESULT_OK:
                        lat = Double.parseDouble(latitude.get(size - 1));
                        lon = Double.parseDouble(longitude.get(size - 1));

                        Log.d(TAG, "?????? ????????? ????????????");
                        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(lat, lon), true); //????????? ????????? ????????? ??????

                        //?????? ?????????
                        MapPoint MARKER_POINT = MapPoint.mapPointWithGeoCoord(lat, lon);
                        MapPOIItem marker = new MapPOIItem();
                        marker.setItemName("?????????");
                        marker.setTag(0);
                        marker.setMapPoint(MARKER_POINT);
                        marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // ???????????? ???????????? BluePin ?????? ??????.
                        marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin); // ????????? ???????????????, ???????????? ???????????? RedPin ?????? ??????.

                        mapView.addPOIItem(marker);
                        pois.add(marker);
                        break;
//                    case RESULT_CANCELED:
//                        Toast.makeText(this, "????????? ?????? ?????? ??????", Toast.LENGTH_SHORT).show();
//                        break;
                }
                break;

            case REQ_USER_CODE:

                switch(resultCode){
                    case RESULT_OK:

                        for (int i = 0; i < pois.size(); i++){
                            for (int j = 0; j < remove_pos.size(); j++){
                                mapView.removePOIItem(pois.get(remove_pos.get(j)));
                            }
                        }
                }
                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.fab:
                anim();
                break;
            case R.id.fab1: //???????????????????????? 1~3???
                Toast.makeText(this, "????????? ?????? ??? ??????", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, UserListActivity.class);
                startActivityForResult(intent, REQ_USER_CODE);

                anim();
                break;
            case R.id.fab2:

                //???????????? ?????????
                double max_x = -1; //???????????? x?????? ??? ?????? ??? ???
                double min_x = 1000000000; //???????????? x?????? ??? ?????? ?????? ???

                double max_y = -1; //???????????? y?????? ??? ?????? ??? ???
                double min_y = 1000000000; //???????????? y?????? ??? ?????? ?????? ???

                //????????? x?????? ??? ?????? ??? ??? ?????????
                for (int i = 0; i < latitude.size(); i++){
                    lat.add(Double.parseDouble(latitude.get(i)));
                    if (max_x < lat.get(i)){
                        max_x = lat.get(i);
                    }
                }

                //????????? x?????? ??? ?????? ?????? ??? ?????????
                for (int i = 0; i < latitude.size(); i++){
                    if (min_x > lat.get(i)){
                        min_x = lat.get(i);
                    }
                }

                //????????? y?????? ??? ?????? ??? ??? ?????????
                for (int i = 0; i < longitude.size(); i++){
                    lon.add(Double.parseDouble(longitude.get(i)));
                    if (max_y < lon.get(i)){
                        max_y = lon.get(i);
                    }
                }

                for (int i = 0; i < longitude.size(); i++){
                    if (min_y > lon.get(i)){
                        min_y = lon.get(i);
                    }
                }

                double center_x = min_x + ((max_x - min_x) / 2); //x?????? ??????
                double center_y = min_y + ((max_y - min_y) / 2); //y?????? ??????

                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
                mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(center_x, center_y), true); //????????? ????????? ????????? ??????

                requestSearchLocal(center_y, center_x);

                Toast.makeText(this, "?????? ????????? ??????????????????.", Toast.LENGTH_LONG).show();

//                mapView.setMapViewEventListener(this);
                mapView.setPOIItemEventListener(this);


                break;

            case R.id.fab3:
                intent = new Intent(this, SearchActivity.class);
//                startActivity(intent);
                startActivityForResult(intent, REQ_CODE);
                break;

        }
    }

    public void anim() {
        if (isFabOpen) {
            fab1.startAnimation(fab_close);
            fab2.startAnimation(fab_close);
            fab3.startAnimation(fab_close);
            fab1.setClickable(false);
            fab2.setClickable(false);
            fab3.setClickable(false);
            isFabOpen = false;
        } else {
            fab1.startAnimation(fab_open);
            fab2.startAnimation(fab_open);
            fab3.startAnimation(fab_open);
            fab1.setClickable(true);
            fab2.setClickable(true);
            fab3.setClickable(true);
            isFabOpen = true;
        }
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {
        //?????? ?????? ?????????
//        touch_x = mapPoint.getMapPointGeoCoord().longitude; //?????? ?????? ??????
//        touch_y = mapPoint.getMapPointGeoCoord().longitude; //?????? ?????? ??????
//
//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        List<Address> addresses = null;
//        Address address = null;
//
//        try{
//            addresses = geocoder.getFromLocation(touch_x, touch_y, 1);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        if (addresses == null || addresses.size() == 0){
//            //?????? ?????????
//        }
//        else{
//            address = addresses.get(0);
//        }
//
//        Toast.makeText(this, String.valueOf(touch_x), Toast.LENGTH_LONG).show();
//        Log.d(TAG, "onMapViewSingleTapped");





    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onDaumMapOpenAPIKeyAuthenticationResult(MapView mapView, int i, String s) {

    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {


    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
//        touch_x = mapPoint.getMapPointGeoCoord().longitude; //?????? ?????? ??????
//        touch_y = mapPoint.getMapPointGeoCoord().longitude; //?????? ?????? ??????

        touch_x = mapPOIItem.getMapPoint().getMapPointGeoCoord().latitude;
        touch_y = mapPOIItem.getMapPoint().getMapPointGeoCoord().longitude;

        center_pick = mapPOIItem.getItemName();

        Log.d(TAG, String.valueOf(latitude.size()));
        Log.d(TAG, String.valueOf(longitude.size()));
        Log.d(TAG, String.valueOf(touch_x));
        Log.d(TAG, String.valueOf(touch_y));

        Log.d(TAG, "????????? ??????");
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
        builder.setTitle("?????? ?????? ??????")
                .setMessage(center_pick + " ??? ?????? ???????????? ?????????????????????????")
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        odsayParsing();

                    }
                })
                .setNegativeButton("??????", null)
                .setCancelable(false)
                .show();
    }

    public void odsayParsing(){


        OnResultCallbackListener onResultCallbackListener = new OnResultCallbackListener() {
            @Override
            public void onSuccess(ODsayData odsayData, API api) {
                try {
                    if (api == API.SEARCH_PUB_TRANS_PATH) {
                        doJSONParser(odsayData);
                        count++;
                        Log.d(TAG,"????????????1 ????????????: " + min);
                        time.add(min);
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(int i, String errorMessage, API api) {
                //?????? ??????
                if (api == API.SEARCH_PUB_TRANS_PATH) {
//                    tv_data.setText("API : " + api.name() + "\n" + errorMessage);
                }
            }
        };

        String x = String.valueOf(touch_x);
        String y = String.valueOf(touch_y);

        Log.d(TAG, "?????????5" + String.valueOf(time.size()));

        for (int i = 0; i < latitude.size(); i++){
            min = 100000;
            odsayService.requestSearchPubTransPath(longitude.get(i), latitude.get(i), y, x, "0", "0", "0", onResultCallbackListener);

            Log.d(TAG, String.valueOf("?????????1" + time.size()));

        }

        Log.d(TAG, String.valueOf("?????????2" + time.size()));



    }


    public void doJSONParser(ODsayData odsayData) throws JSONException{

        ArrayList<Integer> times = new ArrayList<>(); //??? ????????? ????????? ????????? ?????????

//        min = 100000;
        times.clear();
        Log.d(TAG, String.valueOf(time.size()));

        JSONObject result = odsayData.getJson().getJSONObject("result");

        JSONArray path = (JSONArray)result.get("path");

        for (int i = 0; i < path.length(); i++){
            JSONObject j = path.getJSONObject(i);

//            String pathType = j.getString("pathType");
            JSONObject info = j.getJSONObject("info");

            String totalTime = info.getString("totalTime");
            int t = Integer.parseInt(totalTime);
            times.add(t);


        }
        for (int k = 0; k < times.size(); k++){
            if (min > times.get(k)){
                min = times.get(k);
            }
        }

        Log.d(TAG, "????????????: " + min);

        if (count % latitude.size() == 0){
            Intent intent = new Intent(MapActivity.this, TimeActivity.class);

            startActivity(intent);
        }



//        time.add(min);
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    private void requestSearchLocal(double x, double y) {
        bigMartList.clear();
        gs24List.clear();
        subwayList.clear();
        bankList.clear();
        restaurantList.clear();
        cafeList.clear();

        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<CategoryResult> call = apiInterface.getSearchCategory(getString(R.string.restapi_key), "MT1", x + "", y + "", 1000);
        call.enqueue(new Callback<CategoryResult>() {
            @Override
            public void onResponse(@NotNull Call<CategoryResult> call, @NotNull Response<CategoryResult> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    if (response.body().getDocuments() != null) {
                        Log.d(TAG, "bigMartList Success");
                        bigMartList.addAll(response.body().getDocuments());
                    }
                    call = apiInterface.getSearchCategory(getString(R.string.restapi_key), "CS2", x + "", y + "", 1000);
                    call.enqueue(new Callback<CategoryResult>() {
                        @Override
                        public void onResponse(@NotNull Call<CategoryResult> call, @NotNull Response<CategoryResult> response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                Log.d(TAG, "gs24List Success");
                                gs24List.addAll(response.body().getDocuments());

                                call = apiInterface.getSearchCategory(getString(R.string.restapi_key), "SW8", x + "", y + "", 1000);
                                call.enqueue(new Callback<CategoryResult>() {
                                    @Override
                                    public void onResponse(@NotNull Call<CategoryResult> call, @NotNull Response<CategoryResult> response) {
                                        if (response.isSuccessful()) {
                                            assert response.body() != null;
                                            Log.d(TAG, "subwayList Success");
                                            subwayList.addAll(response.body().getDocuments());

                                            call = apiInterface.getSearchCategory(getString(R.string.restapi_key), "FD6", x + "", y + "", 1000);
                                            call.enqueue(new Callback<CategoryResult>() {
                                                @Override
                                                public void onResponse(@NotNull Call<CategoryResult> call, @NotNull Response<CategoryResult> response) {
                                                    if (response.isSuccessful()) {
                                                        assert response.body() != null;
                                                        Log.d(TAG, "restaurantList Success");
                                                        restaurantList.addAll(response.body().getDocuments());
                                                        call = apiInterface.getSearchCategory(getString(R.string.restapi_key), "CE7", x + "", y + "", 1000);
                                                        call.enqueue(new Callback<CategoryResult>() {
                                                            @Override
                                                            public void onResponse(@NotNull Call<CategoryResult> call, @NotNull Response<CategoryResult> response) {
                                                                if (response.isSuccessful()) {
                                                                    assert response.body() != null;
                                                                    Log.d(TAG, "cafeList Success");
                                                                    cafeList.addAll(response.body().getDocuments());
                                                                    //?????? ?????? ?????? ??? circle ??????
                                                                    MapCircle circle1 = new MapCircle(
                                                                            MapPoint.mapPointWithGeoCoord(y, x), // center
                                                                            1000, // radius
                                                                            Color.argb(128, 255, 0, 0), // strokeColor
                                                                            Color.argb(128, 0, 255, 0) // fillColor
                                                                    );
                                                                    circle1.setTag(100);
                                                                    mapView.addCircle(circle1);
                                                                    Log.d(TAG, "??? ??????");

                                                                    Log.d("SIZE1", bigMartList.size() + "");
                                                                    Log.d("SIZE2", gs24List.size() + "");
                                                                    Log.d("SIZE3", subwayList.size() + "");
                                                                    Log.d("SIZE4", bankList.size() + "");
                                                                    Log.d("SIZE5", restaurantList.size() + "");
                                                                    //?????? ??????
                                                                    int tagNum = 10;
                                                                    for (Document document : bigMartList) {
                                                                        MapPOIItem marker = new MapPOIItem();
                                                                        marker.setItemName(document.getPlaceName()); //????????? ??? ?????? ????????? ?????? ??????

                                                                        marker.setTag(tagNum++);
                                                                        double x = Double.parseDouble(document.getY());
                                                                        double y = Double.parseDouble(document.getX());
                                                                        //??????????????? ????????? new MapPoint()???  ????????????. ??????????????? ???????????? ????????? ???????????? ???????????????
                                                                        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                                                        marker.setMapPoint(mapPoint);
//                                                                                                                        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ??????????????? ????????? ????????? ??????.
//                                                                                                                        marker.setCustomImageResourceId(R.drawable.ic_big_mart_marker); // ?????? ?????????.
                                                                        marker.setMarkerType(MapPOIItem.MarkerType.YellowPin);
                                                                        marker.setCustomImageAutoscale(false); // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                                        marker.setCustomImageAnchor(0.5f, 1.0f); // ?????? ???????????? ????????? ?????? ??????(???????????????) ?????? - ?????? ????????? ?????? ?????? ?????? x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) ???.
                                                                        mapView.addPOIItem(marker);

                                                                        marker.setShowAnimationType(MapPOIItem.ShowAnimationType.SpringFromGround);
                                                                    }
                                                                    for (Document document : gs24List) {
                                                                        MapPOIItem marker = new MapPOIItem();
                                                                        marker.setItemName(document.getPlaceName());

                                                                        marker.setTag(tagNum++);
                                                                        double x = Double.parseDouble(document.getY());
                                                                        double y = Double.parseDouble(document.getX());
                                                                        //??????????????? ????????? new MapPoint()???  ????????????. ??????????????? ???????????? ????????? ???????????? ???????????????
                                                                        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                                                        marker.setMapPoint(mapPoint);
//                                                                                                                        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ??????????????? ????????? ????????? ??????.
//                                                                                                                        marker.setCustomImageResourceId(R.drawable.ic_24_mart_marker); // ?????? ?????????.
                                                                        marker.setMarkerType(MapPOIItem.MarkerType.YellowPin);
                                                                        marker.setCustomImageAutoscale(false); // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                                        marker.setCustomImageAnchor(0.5f, 1.0f);
                                                                        mapView.addPOIItem(marker);

                                                                        marker.setShowAnimationType(MapPOIItem.ShowAnimationType.SpringFromGround);
                                                                    }
                                                                    for (Document document : subwayList) {
                                                                        MapPOIItem marker = new MapPOIItem();
                                                                        marker.setItemName(document.getPlaceName());

                                                                        marker.setTag(tagNum++);
                                                                        double x = Double.parseDouble(document.getY());
                                                                        double y = Double.parseDouble(document.getX());
                                                                        //??????????????? ????????? new MapPoint()???  ????????????. ??????????????? ???????????? ????????? ???????????? ???????????????
                                                                        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                                                        marker.setMapPoint(mapPoint);
//                                                                                                                        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ??????????????? ????????? ????????? ??????.
//                                                                                                                        marker.setCustomImageResourceId(R.drawable.ic_subway_marker); // ?????? ?????????.
                                                                        marker.setMarkerType(MapPOIItem.MarkerType.YellowPin);

                                                                        marker.setCustomImageAutoscale(false); // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                                        marker.setCustomImageAnchor(0.5f, 1.0f);
                                                                        mapView.addPOIItem(marker);

                                                                        marker.setShowAnimationType(MapPOIItem.ShowAnimationType.SpringFromGround);
                                                                    }
                                                                    for (Document document : bankList) {
                                                                        MapPOIItem marker = new MapPOIItem();
                                                                        marker.setItemName(document.getPlaceName());

                                                                        marker.setTag(tagNum++);
                                                                        double x = Double.parseDouble(document.getY());
                                                                        double y = Double.parseDouble(document.getX());
                                                                        //??????????????? ????????? new MapPoint()???  ????????????. ??????????????? ???????????? ????????? ???????????? ???????????????
                                                                        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                                                        marker.setMapPoint(mapPoint);
//                                                                                                                        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ??????????????? ????????? ????????? ??????.
//                                                                                                                        marker.setCustomImageResourceId(R.drawable.ic_bank_marker); // ?????? ?????????.
                                                                        marker.setMarkerType(MapPOIItem.MarkerType.YellowPin);

                                                                        marker.setCustomImageAutoscale(false); // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                                        marker.setCustomImageAnchor(0.5f, 1.0f);
                                                                        mapView.addPOIItem(marker);

                                                                        marker.setShowAnimationType(MapPOIItem.ShowAnimationType.SpringFromGround);
                                                                    }
                                                                    for (Document document : restaurantList) {
                                                                        MapPOIItem marker = new MapPOIItem();
                                                                        marker.setItemName(document.getPlaceName());

                                                                        marker.setTag(tagNum++);
                                                                        double x = Double.parseDouble(document.getY());
                                                                        double y = Double.parseDouble(document.getX());
                                                                        //??????????????? ????????? new MapPoint()???  ????????????. ??????????????? ???????????? ????????? ???????????? ???????????????
                                                                        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                                                        marker.setMapPoint(mapPoint);
//                                                                                                                        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ??????????????? ????????? ????????? ??????.
//                                                                                                                        marker.setCustomImageResourceId(R.drawable.ic_pharmacy_marker); // ?????? ?????????.
                                                                        marker.setMarkerType(MapPOIItem.MarkerType.YellowPin);

                                                                        marker.setCustomImageAutoscale(false); // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                                        marker.setCustomImageAnchor(0.5f, 1.0f);
                                                                        mapView.addPOIItem(marker);

                                                                        marker.setShowAnimationType(MapPOIItem.ShowAnimationType.SpringFromGround);
                                                                        //??????????????? fab ?????? ?????????
//                                                                                                                        mLoaderLayout.setVisibility(View.GONE);
//                                                                                                                        searchDetailFab.setVisibility(View.VISIBLE);
                                                                    }
                                                                    for (Document document : cafeList) {
                                                                        MapPOIItem marker = new MapPOIItem();
                                                                        marker.setItemName(document.getPlaceName());

                                                                        marker.setTag(tagNum++);
                                                                        double x = Double.parseDouble(document.getY());
                                                                        double y = Double.parseDouble(document.getX());
                                                                        //??????????????? ????????? new MapPoint()???  ????????????. ??????????????? ???????????? ????????? ???????????? ???????????????
                                                                        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(x, y);
                                                                        marker.setMapPoint(mapPoint);
//                                                                                                                        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // ??????????????? ????????? ????????? ??????.
//                                                                                                                        marker.setCustomImageResourceId(R.drawable.ic_cafe_marker); // ?????? ?????????.
                                                                        marker.setMarkerType(MapPOIItem.MarkerType.YellowPin);

                                                                        marker.setCustomImageAutoscale(false); // hdpi, xhdpi ??? ??????????????? ???????????? ???????????? ????????? ?????? ?????? ?????????????????? ????????? ????????? ??????.
                                                                        marker.setCustomImageAnchor(0.5f, 1.0f);
                                                                        mapView.addPOIItem(marker);

                                                                        marker.setShowAnimationType(MapPOIItem.ShowAnimationType.SpringFromGround);

                                                                    }

                                                                }

                                                            }

                                                            @Override
                                                            public void onFailure(@NotNull Call<CategoryResult> call, @NotNull Throwable t) {

                                                            }
                                                        });
                                                    }
                                                }

                                                @Override
                                                public void onFailure(@NotNull Call<CategoryResult> call, Throwable t) {

                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onFailure(@NotNull Call<CategoryResult> call, @NotNull Throwable t) {

                                    }
                                });
                            }
                        }

                        @Override
                        public void onFailure(@NotNull Call<CategoryResult> call, @NotNull Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onFailure(@NotNull Call<CategoryResult> call, @NotNull Throwable t) {

            }
        });
    }
}