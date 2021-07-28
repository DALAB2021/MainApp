package com.dalab.dalabapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.dalab.dalabapp.SelfDefineViews.DrawLineChart;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class HoeostasisDataPage extends AppCompatActivity {
    TimerTask timerTask;
    TextView timerText;
    TextView forceText;
    ImageView heartImage;
    TextView percentText;
    TextView bleedText;
    TextView stateText;
    TextView infoText;
    int percent;
    Timer timer1;
    ArrayList<Float> Values = new ArrayList<Float>();
    int sampleDistance = 10;//采样的间距，在demo里面体现为每点击n次按钮才会显示一次
    ArrayList<Float> storage = new ArrayList<Float>();
    int lowerValue = 200;
    int upperValue = 600;
    int Volumn = 1000;//满血是1000ml，即流血量大于这个就会休克
    int lose = 0;//流血量

    int overTime=0;//这两个是用来记录小于bond和大于bond的时间的
    int lowerTime=0;


    int mode=0;//这是一个随机数，用来表示我生成的数据是模拟：0:压力过小，止血失败。1：压力在范围内，止血成功。2：压力过大，肢体失血坏死
    int max=600;
    int min=200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoeostasis_data_page);
        heartImage = findViewById(R.id.imageView3);
        bleedText = findViewById(R.id.textView3);
        stateText = findViewById(R.id.textView6);
        infoText=findViewById(R.id.textView2);
        percentText = findViewById(R.id.percent);
        percent = Integer.valueOf(percentText.getText().toString());
        changeImage(100);//
        heartBeat(1000);

        lowerValue=MainPage.lowerValue;
        upperValue=MainPage.upperValue;

        Random random = new Random();
        mode=random.nextInt(3);//0,1,2
        if(mode==0)
        {
            max=300;
            min=100;
        }
        else if(mode==1)
        {
            max=600;
            min=200;
        }
        else if(mode==2)
        {
            max=700;
            min=500;
        }
        init();//初始化坐标数据
        Values.add(0.0f);
        timerText = findViewById(R.id.timerText);
        forceText = findViewById(R.id.forceText);
        timer1 = new Timer();
        startTimer();
    }

    private void init() {
        DrawLineChart chart = findViewById(R.id.chart);
        chart.setBrokenLineLTRB(50, 15, 10, 5);
        chart.setRadius(2.5f);
        chart.setCircleWidth(1f);
        chart.setBorderTextSize(15);//修改边框文字大小
        chart.setBrokenLineTextSize(10);//修改这线上文字大小
//        chart.setMaxVlaue(600);
        chart.setMaxVlaue(750);
        chart.setMinValue(0);
        chart.setNumberLine(4);//5根线
        chart.setBorderWidth(1f);
        chart.setBrokenLineWidth(1.5f);
        chart.setBorderTransverseLineWidth(1.0f);//中间横线的宽度
        chart.setUpper((float) upperValue);//这两个就是上下限范围...
        chart.setLower((float) lowerValue);

//        Random random = new Random();
//        float[] floats = new float[24];
//        for (int i = 0; i < floats.length; i++) {
//            float f = random.nextFloat();
//            floats[i] = f * 60 - 10;
////            Log.i("onCreate", "onCreate: f" + f);
//        }
//        chart.setValue(floats);//随机生成初始值
    }

    private void startTimer() {
        timerTask = new TimerTask() {
            int count;

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        count += 10;
                        changeWithSample();
                        timerText.setText(getStringTime(count));
                        if (count >= 5000) {
                            count += 1000;
                            //颜色变化作为提示
                            timerText.setTextColor(Color.rgb(255,120,71));
                            //或许还可以加上其他的提示信息
                            infoText.setText("时间跳动到15min...");

                            release=true;//提示松开，之后的数据生成就是松开的数据...
                            //然后此刻开始计时，时间用于判断是主动松开还是被动松开——实际上这个计时还是在generateData函数里面去进行
                        }
                        if (count >= 900000) {//15min
                            timerText.setText("止血成功！！");
                            System.out.println("lowerTime:"+lowerTime);
                            System.out.println("overTime:"+overTime);
                            System.out.println("releaseTime:"+releaseTime);
                            timer1.cancel();
                        }
                    }
                });
            }
        };
        timer1.schedule(timerTask, 0, 10);//每0.01s调用一次//period实际上也就是interval
    }

    private String getStringTime(int cnt) {
        int min = cnt / 60000;
        int second = cnt % 60000 / 1000;
        int minisecond = cnt % 1000 / 10;
        return String.format(Locale.CHINA, "%02d:%02d:%02d", min, second, minisecond);
    }

    private void changeWithSample() {
        DrawLineChart chart = findViewById(R.id.chart);

        Random random = new Random();
        float f = random.nextFloat();
        float data = f * 600;//这个是生成的data，之后当然是用传递过来的数据
        //可以写成一个函数来生成这里的data
        data=generateData();

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
            chart.invalidate();//重绘
            storage.clear();

            //最后再修改text
            String newForce = String.valueOf((int) average) + "mmHg";
            forceText.setText(newForce);
            //还要检查一下颜色
            checkColor((int)average);
            //以及处理全局变量，lower&overvalue来为评分做准备
            updateValue((int)average);



            //调用流血的展示函数
            minus();
        }
    }
    private void updateValue(int cnt)
    {
        if(cnt>upperValue)
        {
            overTime++;
        }
        else if(cnt<lowerValue){
            lowerTime++;
        }
        //处理完了之后就保存在这个类里面，当点击按钮切换到评价页面的时候就可以把这两个数据传递过去。
    }

    private void checkColor(int cnt)
    {
        if(cnt<lowerValue)
        {
            forceText.setTextColor(Color.rgb(255,160,0));
        }
        else if(lowerValue < cnt && cnt <upperValue)
        {
            forceText.setTextColor(Color.rgb(0,238,0));
        }
        else{
            forceText.setTextColor(Color.RED);
        }
    }
    private void changeImage(int oldPercent) {

        int number = (100 - percent) * 45 / 100;//得到图片的编号
        //甚至可以检查number有无变化来决定是否重载资源——所以传入的是old的percent
        int oldNumber = (100 - oldPercent) * 45 / 100;
        if (oldNumber == number) {
            return;
        }
        String name = "h" + String.valueOf(number);
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
        scale.setRepeatCount(-1);//-1就是无穷次
        scale.setRepeatMode(Animation.REVERSE);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(blur);
        animationSet.addAnimation(scale);
        heartImage.startAnimation(animationSet);
    }

    private void minus()//点击 - 按钮
    {
        if (percent <= 0)//不能再变小了
        {
            String zero = "0";
            percentText.setText(zero);
            return;
        }
//        int oldPercent=percent;
//        percent-=1;
        int oldPercent = percent;
        lose += 10;//流血量变多
        String bleed = "失血量：" + lose + "ml";
        bleedText.setText(bleed);
        updateState();
        percent = (Volumn - lose) * 100 / Volumn;//计算之后的比例
        String percentage = String.valueOf(percent);
        percentText.setText(percentage);
        changeImage(oldPercent);
    }

    private void updateState()//根据流血量来更新状态
    {
        if(lose<500)
        {
            String state="当前状态:"+"流血";
            stateText.setText(state);
        }
        else if (lose >= 800) {
            String state="当前状态:"+"昏迷";
            stateText.setText(state);
        }
    }

    boolean release=false;
    int speed=1;//下降的速度——10好像有点太快了...
    int declineValue=speed;
    int releaseTime=0;
    private int  generateData()
    {
        //判断decline？
        Random random=new Random();
        int value,range;
        if(!release)//还没有松开
        {
            range=max-min;
            value=min+random.nextInt(range);
        }
        else{//开始松开
            declineValue+=speed;

            value=max-declineValue;
            if(value<=0)
            {
                value=0;
            }
            if(value>lowerValue)
            {
                releaseTime++;
            }
        }

        return value;
    }
}