package kr.ac.hs.recipe.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import kr.ac.hs.recipe.R;
import kr.ac.hs.recipe.fragment.HomeFragment;
import kr.ac.hs.recipe.fragment.UserInfoFragment;
import kr.ac.hs.recipe.fragment.UserListFragment;
import kr.ac.hs.recipe.recipeDB.ingredientsData;
import kr.ac.hs.recipe.recipeDB.recipeData;
import kr.ac.hs.recipe.recipeDB.stepData;
import kr.ac.hs.recipe.ui.LoadingActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends BasicActivity {
    private static final String TAG = "MainActivity";

    String key = "1c74fe1f5913c684ec9bb14cc1dd45295904903af4c2012cb985cb757b1a322e";
    int start, end;
    int v_RECIPE_ID, v_NATION_CODE, v_TY_CODE, v_IRDNT_TY_CODE, v_COOKING_NO;
    String v_RECIPE_NM_KO, v_SUMRY, v_NATION_NM, v_TY_NM, v_COOKING_TIME, v_CALORIE, v_QNT, v_LEVEL_NM, v_IRDNT_CODE, v_IRDNT_NM, v_IRDNT_CPCTY, v_IRDNT_TY_NM, v_COOKING_DC, v_STRE_STEP_IMAGE_URL, v_STEP_TIP, v_IMG_URL, v_DET_URL;

    // Write a message to the database
    DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference recipeDBRef = myRef.child("recipeDB");
    recipeData recipeData = null; // 레시피 기본정보
    ingredientsData ingredientsData = null; // 레시피 재료정보
    stepData stepData = null; // 레시피 과정정보

    LocalDate now = LocalDate.now();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setToolbarTitle(getResources().getString(R.string.app_name));

        Intent intent = new Intent(this, LoadingActivity.class); // 로딩 화면
        startActivity(intent);

        //myRef.removeValue(); // drop all DB
        //updateData();


        // 앱 최초 실행 여부 판단
        SharedPreferences pref = getSharedPreferences("isFirst", Activity.MODE_PRIVATE);
        boolean first = pref.getBoolean("isFirst", false);
        if (!first) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("isFirst", true);
            editor.apply();
            recipeDBRef.child("Last_Update").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    String last_update = snapshot.getValue().toString();
                    String now_date = now.toString();
                    if (!last_update.equals(now_date)){
                        updateData();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {

                }
            });
        }

        // 최초 실행 이후, 일주일에 한 번만 데이터 갱신
        if (now.getDayOfWeek().getValue() == 7) { // 일요일에만 데이터 업데이트 (주기 설정, 월=1 ~ 일=7)
            recipeDBRef.child("Last_Update").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    String last_update = snapshot.getValue().toString();
                    String now_date = now.toString();
                    if (!last_update.equals(now_date)){
                        updateData();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {

                }
            });
        }

        // Navigation 구성
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_search, R.id.navigation_myInfo, R.id.navigation_userList)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                init();
                break;
        }
    }

    private void init(){
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            myStartActivity(SignUpActivity.class);
        } else {
            DocumentReference documentReference = FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid());
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null) {
                            if (document.exists()) {
                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            } else {
                                Log.d(TAG, "No such document");
                                myStartActivity(MemberInitActivity.class);
                            }
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });

/*            HomeFragment homeFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, homeFragment)
                    .commit();

            BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.home:
                            HomeFragment homeFragment = new HomeFragment();
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, homeFragment)
                                    .commit();
                            return true;
                        case R.id.myInfo:
                            UserInfoFragment userInfoFragment = new UserInfoFragment();
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, userInfoFragment)
                                    .commit();
                            return true;
                        case R.id.userList:
                            UserListFragment userListFragment = new UserListFragment();
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, userListFragment)
                                    .commit();
                            return true;
                    }
                    return false;
                }
            });*/
        }
    }

    private void myStartActivity(Class c) {
        Intent intent = new Intent(this, c);
        startActivityForResult(intent, 1);
    }

    // 데이터 update
    public void updateData() {
        Thread getData = new getData();
        Thread getData_IRDNT = new getData_IRDNT();
        Thread getData_STEP = new getData_STEP();

        recipeDBRef.child("Last_Update").setValue(now.toString()); // 오늘의 날짜, 시간

        getData.start(); // 레시피 기본정보
        try {
            getData.join(); // getData가 종료될 때까지 기다림
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getData_IRDNT.start(); // 레시피 재료정보
        try {
            getData_IRDNT.join(); // getData_IRDNT가 종료될 때까지 기다림
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        getData_STEP.start(); // 레시피 과정정보
    }

    // Open API Json Parsing
    // 레시피 기본정보
    public void JsonParse(String str) {
        try {
            JSONObject obj = new JSONObject(str);
            JSONArray rows = obj.getJSONObject("Grid_20150827000000000226_1").getJSONArray("row");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject jObject = rows.getJSONObject(i);
                v_RECIPE_ID = jObject.getInt("RECIPE_ID");
                v_RECIPE_NM_KO = jObject.getString("RECIPE_NM_KO");
                v_SUMRY = jObject.getString("SUMRY");
                v_NATION_CODE = jObject.getInt("NATION_CODE");
                v_NATION_NM = jObject.getString("NATION_NM");
                v_TY_CODE = jObject.getInt("TY_CODE");
                v_TY_NM = jObject.getString("TY_NM");
                v_COOKING_TIME = jObject.getString("COOKING_TIME");
                v_CALORIE = jObject.getString("CALORIE");
                v_QNT = jObject.getString("QNT");
                v_LEVEL_NM = jObject.getString("LEVEL_NM");
                v_IRDNT_CODE = jObject.getString("IRDNT_CODE");
                v_IMG_URL = jObject.getString("IMG_URL");
                v_DET_URL = jObject.getString("DET_URL");
                /*img_url = jObject.getString("IMG_URL");
                v_IMG_URL = urlToBitmap(img_url);
                det_url = jObject.getString("DET_URL");
                v_DET_URL = urlToBitmap(det_url);*/

                //firebase에 data input
                recipeData = new recipeData(v_RECIPE_ID, v_RECIPE_NM_KO, v_SUMRY, v_NATION_CODE, v_NATION_NM, v_TY_CODE, v_TY_NM, v_COOKING_TIME, v_CALORIE, v_QNT, v_LEVEL_NM, v_IRDNT_CODE, v_IMG_URL, v_DET_URL);
                recipeDBRef.child("recipe_ID").child(String.valueOf(v_RECIPE_ID)).setValue(recipeData);
            }
        } catch (Exception e) {
        }
    }

    // 레시피 재료정보
    public void JsonParse_IRDNT(String str) {
        try {
            JSONObject obj = new JSONObject(str);
            JSONArray rows = obj.getJSONObject("Grid_20150827000000000227_1").getJSONArray("row");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject jObject = rows.getJSONObject(i);
                v_RECIPE_ID = jObject.getInt("RECIPE_ID");
                v_IRDNT_NM = jObject.getString("IRDNT_NM");
                v_IRDNT_CPCTY = jObject.getString("IRDNT_CPCTY");
                v_IRDNT_TY_CODE = jObject.getInt("IRDNT_TY_CODE");
                v_IRDNT_TY_NM = jObject.getString("IRDNT_TY_NM");

                //firebase에 data input
                ingredientsData = new ingredientsData(v_RECIPE_ID, v_IRDNT_NM, v_IRDNT_CPCTY, v_IRDNT_TY_CODE, v_IRDNT_TY_NM);
                recipeDBRef.child("recipe_ID").child(String.valueOf(v_RECIPE_ID)).child("IRDNT_LIST").child(String.valueOf(v_IRDNT_NM)).setValue(ingredientsData);
            }
        } catch (Exception e) {
        }
    }

    // 레시피 과정정보
    public void JsonParse_STEP(String str) {
        try {
            JSONObject obj = new JSONObject(str);
            JSONArray rows = obj.getJSONObject("Grid_20150827000000000228_1").getJSONArray("row");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject jObject = rows.getJSONObject(i);
                v_RECIPE_ID = jObject.getInt("RECIPE_ID");
                v_COOKING_NO = jObject.getInt("COOKING_NO");
                v_COOKING_DC = jObject.getString("COOKING_DC");
                v_STRE_STEP_IMAGE_URL = jObject.getString("STRE_STEP_IMAGE_URL");
                v_STEP_TIP = jObject.getString("STEP_TIP");

                //firebase에 data input
                stepData = new stepData(v_RECIPE_ID, v_COOKING_NO, v_COOKING_DC, v_STRE_STEP_IMAGE_URL, v_STEP_TIP);
                recipeDBRef.child("recipe_ID").child(String.valueOf(v_RECIPE_ID)).child("STEP").child(String.valueOf(v_COOKING_NO)).setValue(stepData);
            }
        } catch (Exception e) {
        }
    }

    // 레시피 기본정보
    class getData extends Thread {
        public void run() {
            //http://211.237.50.150:7080/openapi/1c74fe1f5913c684ec9bb14cc1dd45295904903af4c2012cb985cb757b1a322e/json/Grid_20150827000000000226_1/1/10
            String queryUrl = "http://211.237.50.150:7080/openapi/" + key + "/json/Grid_20150827000000000226_1/1/537";
            try {
                URL url = new URL(queryUrl);
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                String result;
                while ((result = br.readLine()) != null) {
                    sb.append(result + "\n");
                }
                result = sb.toString();
                JsonParse(result);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 레시피 재료정보
    class getData_IRDNT extends Thread {
        public void run() {
            start = 0;
            end = 0; // 초기화
            //http://211.237.50.150:7080/openapi/1c74fe1f5913c684ec9bb14cc1dd45295904903af4c2012cb985cb757b1a322e/json/Grid_20150827000000000227_1/1/6104
            for (start = 1; start < 6104; start += 1000) {
                end = start + 999; // 한 번에 1000개씩 호출 가능
                String queryUrl = "http://211.237.50.150:7080/openapi/" + key + "/json/Grid_20150827000000000227_1/" + start + "/" + end;
                try {
                    URL url = new URL(queryUrl);
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                    String result;
                    while ((result = br.readLine()) != null) {
                        sb.append(result + "\n");
                    }
                    result = sb.toString();
                    JsonParse_IRDNT(result);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 레시피 과정정보
    class getData_STEP extends Thread {
        public void run() {
            start = 0;
            end = 0; // 초기화
            //http://211.237.50.150:7080/openapi/1c74fe1f5913c684ec9bb14cc1dd45295904903af4c2012cb985cb757b1a322e/json/Grid_20150827000000000228_1/1/5
            for (start = 1; start < 3022; start += 1000) {
                end = start + 999; // 한 번에 1000개씩 호출 가능
                String queryUrl = "http://211.237.50.150:7080/openapi/" + key + "/json/Grid_20150827000000000228_1/" + start + "/" + end;
                try {
                    URL url = new URL(queryUrl);
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                    String result;
                    while ((result = br.readLine()) != null) {
                        sb.append(result + "\n");
                    }
                    result = sb.toString();
                    JsonParse_STEP(result);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}