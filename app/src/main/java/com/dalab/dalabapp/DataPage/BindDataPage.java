package com.dalab.dalabapp.DataPage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dalab.dalabapp.MainPage;
import com.dalab.dalabapp.R;
import com.dalab.dalabapp.SelfDefineViews.DrawLineChart;
import com.dalab.dalabapp.TrainingPages.ResBind;
import com.dalab.dalabapp.TrainingPages.ResHomeostasis;
import com.dalab.dalabapp.Trains.BindPage;
import com.dalab.dalabapp.Utils.BindModel;
import com.dalab.dalabapp.Utils.GenerateData;
import com.dalab.dalabapp.constant.Global;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class BindDataPage extends AppCompatActivity {
    // 时间
    TimerTask timerTask;
    Timer timer1;
    int interval = 10;
    // 生成的数据
    ArrayList<Float> storage = new ArrayList<Float>();
    ArrayList<Float> Values = new ArrayList<Float>();
    int sampleDistance = 10;
    // 组件
    BindModel bindModel;
    GenerateData generateData;
    // 最大最小值
    int max = Global.global.down_high_value, min = Global.global.down_low_value;
    int count;
    // 页面组件
    TextView forceText;
    TextView totalTimeText;
    TextView validTimeText;
    Button jump;
    // utils
    int speed = 10;
    float currentData;
    boolean broken = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_data_page);
        // 获取最大最小值
//        max = Global.global.down_High;
//        min = Global.global.down_Low;
        //根据当前的type来判断是那种类型。
        if (Global.global.currentType == 10) {
            //上肢
            max = Global.global.up_high_value;
            min = Global.global.up_low_value;
        } else {
            max = Global.global.down_high_value;
            min = Global.global.down_low_value;
        }
        // 初始化表格和子组件
        init();
        //手动调整生成数据以测试
        generateData = new GenerateData(max, min);
//        generateData = new GenerateData(10,5);
        bindModel = new BindModel(max, min);
        // 预留给页面组件初始化
        forceText = findViewById(R.id.forceText);
        totalTimeText = findViewById(R.id.timerText);
        validTimeText = findViewById(R.id.timerText2);
        validTimeText.setTextColor(Color.rgb(255, 130, 71));//有效时间和训练时间用不同的颜色
        totalTimeText.setTextColor(Color.rgb(0, 238, 0));
        jump = findViewById(R.id.JumpToRes);
        // 开始
        timer1 = new Timer();
        StartTimer();
        //
        jump.setVisibility(View.INVISIBLE);
        jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextPage(v);
            }
        });
    }

    private void StartTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 对待压力较小的态度：提前打断，初次容忍10s，回落容忍2s
                        // 对待压力过大的态度，到最后看平均值，如果大了就扣分
                        if (count >= Global.global.bindTime * 60 * 1000) {
                            over();
                        }
                        count += speed;
                       // currentData = generateData.generate(false);
                        //现在的currentData应该是从全局变量读取
                        currentData=Global.global.pressure;
                        //更新
                        if (!bindModel.update(currentData, speed)) {
                            bre();
                        }
                        // 页面元素
                        changeUI();
                    }
                });
            }
        };
        timer1.schedule(timerTask, 0, interval);
    }

    private void bre() {
        timer1.cancel();
        broken = true;
        jump.setVisibility(View.VISIBLE);
    }

    private void over() {
        timer1.cancel();
        jump.setVisibility(View.VISIBLE);
    }

    private void changeUI() {
        // 更新表格
        changeChart();
        totalTimeText.setText(getStringTime(bindModel.getTime()));
        validTimeText.setText(getStringTime(bindModel.getValidTime()));
    }

    // 每次时间数据更新都调用这个函数，在这个函数里面模拟数据的生成和更新
    private void changeChart() {
        DrawLineChart chart = findViewById(R.id.chart);
        float data;
        // 可以写成一个函数来生成这里的data
        data = currentData;
        storage.add(data);
        if (storage.size() == sampleDistance) {//存满了之后才计算一次。
            float average = 0;
            for (final Float value : storage) {
                average += value;
            }
            average /= sampleDistance;
            Values.add(average);
            float[] floats = new float[Values.size()];
            int index = 0;
            for (final Float value : Values) {
                floats[index++] = value;
            }
            chart.setValue(floats);
//            chart.setX_MaxVlaue(count/1000.0f);
            chart.invalidate();//重绘
            storage.clear();
            //最后再修改text
            String newForce = (int) average + "牛";
            forceText.setText(newForce);
            //还要检查一下颜色
            checkColor((int) average);
        }
    }

    private void checkColor(int cnt) {
        if (cnt < min) {
            forceText.setTextColor(Color.rgb(255, 160, 0));
        } else if (min < cnt && cnt < max) {
            forceText.setTextColor(Color.rgb(0, 238, 0));
        } else {
            forceText.setTextColor(Color.RED);
        }
    }

    private void init() {
        DrawLineChart chart = findViewById(R.id.chart);
        chart.setBrokenLineLTRB(50, 15, 10, 13);
        chart.setRadius(2.5f);
        chart.setCircleWidth(1f);
        chart.setBorderTextSize(15);//修改边框文字大小
        chart.setBrokenLineTextSize(10);//修改这线上文字大小
        int maxValue = 45 < max ? max : 45;
//        chart.setMaxVlaue(45);
        chart.setMaxVlaue(maxValue);
        chart.setMinValue(0);
//        chart.setX_MaxVlaue(12000);
//        chart.setX_MinValue(0);
        chart.setNumberLine(4);//5根线
//        chart.setX_NumberLine(4);//5根线
        chart.setBorderWidth(1f);
        chart.setBrokenLineWidth(1.5f);
        chart.setBorderTransverseLineWidth(1.0f);//中间横线的宽度
        chart.setUpper((float) max);//这两个就是上下限范围...
        chart.setLower((float) min);
    }

    private String getStringTime(int cnt) {
        int min = cnt / 60000;
        int second = cnt % 60000 / 1000;
        return String.format(Locale.CHINA, "%02d:%02d", min, second);
    }

    public void nextPage(View view) {
        Intent intent = new Intent();
        intent.setClass(BindDataPage.this, ResBind.class);
        //然后把一些参数输入进去
        intent.putExtra("validTime", bindModel.validTime);
        intent.putExtra("delayTime", bindModel.delayTime);
        intent.putExtra("average", bindModel.getAverage());
        intent.putExtra("broken", broken);
        intent.putExtra("max", max);
        startActivity(intent);
    }
}