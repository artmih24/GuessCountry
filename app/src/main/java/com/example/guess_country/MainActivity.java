package com.example.guess_country;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    public Context mainActivity = this;
    private ImageView picture;
    private RadioGroup radioGroup;
    private Button btn1;
    public ArrayList<RadioButton> newRadioButtons = new ArrayList<>();
    boolean firstPress = false;
    private String linkToFlags = "https://www.dorogavrim.ru/articles/flagi_stran_mira/";
    private String domain = "https://www.dorogavrim.ru";
    private ArrayList<String> FlagsNames = new ArrayList<>();
    private ArrayList<String> urls = new ArrayList<>();
    private int numberTrueOfImage = 0;
    private String context = "";
    private int round = 1;
    private int rightAnswers = 0;
    private boolean firstLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle(R.string.app_title);
        picture = (ImageView) findViewById(R.id.guessWho);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        btn1 = (Button) findViewById(R.id.check);
        if (getIntent().getExtras() != null) {
            this.round = getIntent().getExtras().getInt("rounds");
            this.rightAnswers = getIntent().getExtras().getInt("right");
            if (this.round > 1) {
                TextView textView2 = findViewById(R.id.textView2);
                String result = "Правильных ответов: " + this.rightAnswers + '/' + this.round;
                textView2.setText(result);
            }
            this.round = 1;
            this.rightAnswers = 0;
        }

        //Скачиваем и парсим данные с сайта
        DownloadTask task = new DownloadTask();
        try {
            context = task.execute(linkToFlags).get();
            getResources(context);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void randomOptions() {
        int numberOfRadioButton = (int) (Math.random() * 4);//3);
        int i = 0;
        while (i < 4) {
            int numberOfFlagImage = (int) (Math.random() * urls.size());
            while (numberOfFlagImage == numberTrueOfImage || numberOfFlagImage == numberOfRadioButton) {
                numberOfFlagImage = (int) (Math.random() * urls.size());
            }
            RadioButton radioButton;
            radioGroup.clearCheck();
            if (!firstPress) {
                radioButton = new RadioButton(this);
                radioButton.setId(View.generateViewId());
                radioGroup.addView(radioButton);
            } else {
                radioButton = (RadioButton) radioGroup.getChildAt(i);
            }
            if (numberOfRadioButton != i) {
                radioButton.setText(FlagsNames.get(numberOfFlagImage));
            } else {
                radioButton.setText(FlagsNames.get(numberTrueOfImage));
            }
            radioButton.setTextSize(20);
            i++;
        }
    }

    public void onClick (View view) {
        try {   //первое нажатие на кнопку: выводит картинку и поле для ввода фамилии
            if (!firstPress) {
                playGame();
                randomOptions();
                btn1.setText("Ответ");
                radioGroup.setVisibility(View.VISIBLE);
                firstPress = true;
                Button skipButton = findViewById(R.id.skip);
                skipButton.setVisibility(View.VISIBLE);
            }
            //второе и последющие нажатия на кнопку проверяют
            // введенную фамилию и выводят новую картинку
            else {
                int id = radioGroup.getCheckedRadioButtonId();
                if (id != -1) {
                    String answer = ((RadioButton) findViewById(id)).getText().toString();
                    if (!(answer.equals(""))) {
                        if (answer.equals(FlagsNames.get(numberTrueOfImage))) {
                            Toast toast = Toast.makeText(this, "Правильно!", Toast.LENGTH_SHORT);
                            toast.show();
                            this.rightAnswers++;
                        } else {
                            Toast toast = Toast.makeText(this, "Неправильно!", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                        playGame();
                        randomOptions();
                    }
                    this.round++;
                } else {
                    Toast toast = Toast.makeText(this, "Ответ не выбран", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            TextView textView2 = findViewById(R.id.textView2);
            String questionNo = "Вопрос № " + this.round;
            textView2.setText(questionNo);
            TextView textView3 = findViewById(R.id.textView3);
            String count = this.rightAnswers + "/" + (this.round - 1);
            textView3.setText(count);
            if (this.round == 2) {
                Button finishButton = findViewById(R.id.finish);
                finishButton.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void skip(View view) {
        playGame();
        randomOptions();
        this.round++;
        TextView textView2 = findViewById(R.id.textView2);
        String questionNo = "Вопрос № " + this.round;
        textView2.setText(questionNo);
        TextView textView3 = findViewById(R.id.textView3);
        String count = this.rightAnswers + "/" + (this.round - 1);
        textView3.setText(count);
        Toast toast = Toast.makeText(this, "Вопрос пропущен", Toast.LENGTH_SHORT);
        toast.show();
        if (this.round == 2) {
            Button finishButton = findViewById(R.id.finish);
            finishButton.setVisibility(View.VISIBLE);
        }
    }

    public void finish(View view) {
        Intent intent = new Intent(this.mainActivity, MainActivity.class);
        intent.putExtra("rounds", this.round - 1);
        intent.putExtra("right", this.rightAnswers);
        startActivity(intent);
        Toast toast = Toast.makeText(this, "Тест завершен", Toast.LENGTH_SHORT);
        toast.show();
    }

    //парсинг полученных данных
    protected  void getResources(String cont) {
        String  start = "<table style=\"width: 100%;\" align=\"center\">";
        String finish = "</table>";

        Pattern pattern = Pattern.compile(start + "(.*?)" + finish);
        Matcher matcher = pattern.matcher(cont);
        String splitContent = "";
        while(matcher.find()) {
            splitContent = matcher.group(1);
        }
        //ищем ссылки на изображение
        start = " src=\"";
        finish = "\" style=";
        pattern = Pattern.compile(start + "(.*?)" + finish);
        matcher = pattern.matcher(splitContent);
        while(matcher.find()) {
            String[] splitContent2 = matcher.group(1).split(" ");
            urls.add(domain + splitContent2[0]);
        }
        //ищем названия стран
        start = "<br>\t\t ";
        finish = "</";
        pattern = Pattern.compile(start + "(.*?)" + finish);
        matcher = pattern.matcher(splitContent);
        while(matcher.find()) {
            String splitContent2 = matcher.group(1);
            FlagsNames.add(splitContent2.trim());
        }
    }

    //Формирует страницу activity с готовым контентом
    private void playGame() {
        try {//в случайном порядке изображения будут появляться
            numberTrueOfImage = (int) (Math.random() * urls.size());
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            ImageRequest imageRequest = new ImageRequest(
                    urls.get(numberTrueOfImage), // Image URL
                    new Response.Listener<Bitmap>() { // Bitmap listener
                        @Override
                        public void onResponse(Bitmap response) {
                            // Do something with response
                            picture.setImageBitmap(response);
                        }
                    },
                    0, // Image width
                    0, // Image height
                    ImageView.ScaleType.CENTER_CROP, // Image scale type
                    Bitmap.Config.RGB_565, //Image decode configuration
                    new Response.ErrorListener() { // Error listener
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // Do something with error response
                            Log.i("JSON error:", error.getMessage());
                            error.printStackTrace();
                        }
                    }
            );
            // Add ImageRequest to the RequestQueue
            requestQueue.add(imageRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Получение html страницы
    private static class DownloadTask extends AsyncTask<String,Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            StringBuilder result = new StringBuilder();
            URL url = null;
            HttpsURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = bufferedReader.readLine();
                while (line!=null) {
                    result.append(line);
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection!=null)
                    urlConnection.disconnect();
            }
            return  result.toString();
        }
    }
}