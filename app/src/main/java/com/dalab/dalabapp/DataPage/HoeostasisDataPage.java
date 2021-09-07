package com.dalab.dalabapp.DataPage;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dalab.dalabapp.MainPage;
import com.dalab.dalabapp.R;
import com.dalab.dalabapp.SelfDefineViews.DrawLineChart;

import com.dalab.dalabapp.TrainingPages.ResHomeostasis;
import com.dalab.dalabapp.Utils.GenerateData;
import com.dalab.dalabapp.Utils.HoeoIncreaseModel;
import com.dalab.dalabapp.Utils.RelaxModel;
import com.dalab.dalabapp.constant.Global;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class HoeostasisDataPage extends AppCompatActivity {
    TimerTask timerTask;
    TextView timerText;
    TextView validTimeText;
    TextView forceText;
    ImageView heartImage;
    TextView percentText;
    TextView bleedText;
    TextView stateText;
    TextView infoText;
    Button jump;


    Timer timer1;
    ArrayList<Float> Values = new ArrayList<Float>();
    ArrayList<Float> Times = new ArrayList<Float>();
    int sampleDistance = 10;//采样的间距，在demo里面体现为每点击n次按钮才会显示一次。实际上就是缓冲的大小，存满了就计算一次
    ArrayList<Float> storage = new ArrayList<Float>();
    int Volumn = 2000;//满血是2000ml，即流血量大于这个就会休克
    int max = 600;
    int min = 200;
    //-------更新获取数据-------------
    float currentData;
    int lastNum = 0;
    boolean release = false;
    //----------------子模块---------
    GenerateData generateData;
    HoeoIncreaseModel increaseModel;
    RelaxModel relaxModel;
    //-------------------------------
    int speed = 10;// 时间流速——一次interval中流过了多少毫秒。
    int interval = 10;// interval和speed一样的话就是现实中的流速。

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoeostasis_data_page);

        if(Global.global.currentType ==20)
        {
            //左上肢
            max = Global.global.left_up_high_value;
            min = Global.global.left_up_low_value;
        }
        else if(Global.global.currentType==21)//右上
        {
            max = Global.global.right_up_high_value;
            min = Global.global.right_up_low_value;
        }
        else if(Global.global.currentType==22)//左下
        {
            bleedspeed=171.1f;
            max = Global.global.left_down_high_value;
            min = Global.global.left_down_low_value;
        }
        else{
            bleedspeed=171.1f;
            max = Global.global.right_down_high_value;
            min = Global.global.right_down_low_value;
        }

        //手动调整max和min以测试
        generateData = new GenerateData(max, min);
        increaseModel = new HoeoIncreaseModel(max, min, Volumn, bleedspeed);
        relaxModel = new RelaxModel(min);

        heartImage = findViewById(R.id.imageView3);
        bleedText = findViewById(R.id.textView3);
        stateText = findViewById(R.id.textView6);
        infoText = findViewById(R.id.textView2);
        percentText = findViewById(R.id.percent);
        changeImage();//
        heartBeat(1000);

        init();//初始化坐标数据
        Values.add(0.0f);
        Times.add(0.0f);
        timerText = findViewById(R.id.timerText);
        validTimeText = findViewById(R.id.timerText2);
        validTimeText.setTextColor(Color.rgb(255, 130, 71));//有效时间和训练时间用不同的颜色
        timerText.setTextColor(Color.rgb(0, 238, 0));
        forceText = findViewById(R.id.forceText);
        timer1 = new Timer();
        startTimer();
        // jumpRelated
        jump = findViewById(R.id.JumpToRes);
        jump.setVisibility(View.INVISIBLE);
        jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextPage(view);
            }
        });
    }

    private void init() {
        DrawLineChart chart = findViewById(R.id.chart);
        chart.setBrokenLineLTRB(50, 15, 10, 13);
        chart.setRadius(2.5f);
        chart.setCircleWidth(1f);
        chart.setBorderTextSize(15);//修改边框文字大小
        chart.setBrokenLineTextSize(10);//修改这线上文字大小
        int maxValue=20<max?max:20;
        chart.setMaxVlaue(maxValue);
        chart.setMinValue(0);
        chart.setNumberLine(4);//5根线
        chart.setBorderWidth(1f);
        chart.setBrokenLineWidth(1.5f);
        chart.setBorderTransverseLineWidth(1.0f);//中间横线的宽度
        chart.setUpper((float) max);//这两个就是上下限范围...
        chart.setLower((float) min);

        chart.setValid(Global.global.hoeoTime);
    }
    int count;
    private void startTimer() {
        timerTask = new TimerTask() {

            int oldPercent = 100;
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (count >= 180000) {//3min的时候
                            speed = 800;
                            //或许还可以加上其他的提示信息
                            infoText.setText("时间加速跳动到15min...");
                        }
                        if (count >= Global.global.hoeoValidTime * 60 * 1000) {//15min
                            speed = 213;//15min之后流速又减慢一点点
                            release = true;//提示松开，之后的数据生成就是松开的数据...
                        }
                        count += speed;
//                        currentData = generateData.generate(release);//之前是生成数据来进行测试
                        //现在的currentData应该是从全局变量读取到传感器的数据
                        currentData=Global.global.pressure;
                        // 更改坐标表格
                        changeChart();
                        // 更改心脏模型的文本和图片
                        changeUI();
                        if(oldPercent != increaseModel.percent)
                        {
                            changeImage();
                            oldPercent = increaseModel.percent;
                        }
                        // 核心工作
                        if(release)
                        {
                            relaxModel.update(speed, currentData);
                        }
                        increaseModel.update(speed, currentData);
                        timerText.setText(getStringTime(count));
                        // 收尾工作
                        if (count >= Global.global.hoeoTime * 60 * 1000)//20min *60是因为是min为单位
                        {
                            timerText.setText("训练完成");
                            timer1.cancel();
                            jump.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        };
        timer1.schedule(timerTask, 0, interval);//每0.01s调用一次//period实际上也就是interval
    }

    private String getStringTime(int cnt) {
        int min = cnt / 60000;
        int second = cnt % 60000 / 1000;
//        int minisecond = cnt % 1000 / 10;
        return String.format(Locale.CHINA, "%02d:%02d", min, second);
    }

    // 每次时间数据更新都调用这个函数，在这个函数里面模拟数据的生成和更新
    private void changeChart() {
        DrawLineChart chart = findViewById(R.id.chart);
        float data;
        data = currentData;
        storage.add(data);
        if (storage.size() == sampleDistance) {//存满了之后才计算一次。
            float average = 0;
            for (final Float value : storage) {
                average += value;
            }
            average /= sampleDistance;
            Values.add(average);
            Times.add(count/1000.0f);
            float[] floats = new float[Values.size()];
            int index = 0;
            for (final Float value : Values) {
                floats[index++] = value;
            }
            float[] minutes = new float[Times.size()];
            index = 0;
            for (final Float value : Times) {
                minutes[index++] = value;
            }
            chart.setValue(floats);//纵坐标（压力值）的数组
            chart.setTime(minutes);//横坐标（时间）的数组
            chart.invalidate();//重绘
            storage.clear();
            //最后再修改text
            String newForce = (int) average + "牛";
            forceText.setText(newForce);
            //还要检查一下颜色
            checkColor((int) average);
        }
    }

    // UI相关
    private void checkColor(int cnt) {
        if (cnt < min) {
            forceText.setTextColor(Color.rgb(255, 160, 0));
        } else if (min < cnt && cnt < max) {
            forceText.setTextColor(Color.rgb(0, 238, 0));
        } else if(cnt>max) {
            forceText.setTextColor(Color.RED);
        }
    }

    private void changeImage() {
        int number = (100 - increaseModel.percent) * 45 / 100;//得到图片的编号
        if(number == lastNum)
            return;
        lastNum = number;
        String name = "h" + number;
        //根据文件名字来获得id号
        int id = this.getResources().getIdentifier(name, "drawable", this.getPackageName());
        heartImage.setImageResource(id);

        //心跳频率也会变化
        int interval = 1000 - number * 18;//血越少频率越快
        heartBeat(interval);
    }

    private void heartBeat(int interval) {
        Animation blur = new AlphaAnimation(1.0f, 0.85f);
        blur.setDuration(interval);//1s
        blur.setFillAfter(true);
        blur.setFillBefore(true);
        blur.setRepeatCount(-1);//-1就是无穷次
        blur.setRepeatMode(Animation.REVERSE);
        //跟着一起缩放
        Animation scale = new ScaleAnimation(1.0f, 0.85f, 1.0f, 0.85f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(interval);
        scale.setFillAfter(true);
        scale.setFillBefore(true);
        scale.setRepeatCount(-1);// -1就是无穷次
        scale.setRepeatMode(Animation.REVERSE);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(blur);
        animationSet.addAnimation(scale);
        heartImage.startAnimation(animationSet);
    }

    float bleedspeed = 26.9f;//如果是下肢，那么就是171.1f

    @SuppressLint("SetTextI18n")
    private void changeUI()
    {
        updateState(increaseModel.lose);
        bleedText.setText("失血量：" + String.format("%.2f", increaseModel.lose));//应该保留两位小数
        percentText.setText(String.valueOf(increaseModel.percent));
        validTimeText.setText(getStringTime(increaseModel.validTime));
        if(increaseModel.validTime >= Global.global.hoeoValidTime*60*1000)
            infoText.setText("请稍微放松止血带");
    }

    private void updateState(float lose)//根据流血量来更新状态
    {
        if (lose < 500) {
            String state = "当前状态:" + "没有明显症状";
            stateText.setText(state);
        } else if (lose >= 500 && lose < 1000) {
            String state = "当前状态:" + "神经焦虑";
            stateText.setText(state);
        } else if (lose > 1000 && lose < 1500) {
            String state = "当前状态:" + "面色发白，出冷汗";
            stateText.setText(state);
        } else if (lose > 1500 && lose < 2000) {
            String state = "当前状态:" + "萎靡不振，手脚无力";
            stateText.setText(state);
        } else if (lose > 2000) {
            String state = "当前状态:" + "昏迷休克";
            stateText.setText(state);
        }
    }

    public void nextPage(View view) {
        //这个函数是设计给跳转按钮的，需要传递一些数据过去到评价页面。
        Intent intent = new Intent();
        intent.setClass(HoeostasisDataPage.this, ResHomeostasis.class);
        //然后把一些参数输入进去。
        intent.putExtra("validTime", increaseModel.validTime);
        intent.putExtra("delayTime", increaseModel.delayTime);
        intent.putExtra("lose", increaseModel.lose);
        intent.putExtra("releaseTime", relaxModel.releaseTime);
        intent.putExtra("overTime", increaseModel.overTime);
        startActivity(intent);
    }

}
