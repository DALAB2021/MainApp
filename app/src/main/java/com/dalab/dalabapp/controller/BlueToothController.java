package com.dalab.dalabapp.controller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

//蓝牙适配器
public class BlueToothController {
    private BluetoothAdapter mAdapter; //本机蓝牙适配器
    public BlueToothController()
    {
        mAdapter=BluetoothAdapter.getDefaultAdapter();
    }

    //是否支持蓝牙
    public boolean isSupportBlueTooth()
    {
        if(mAdapter!=null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }

    //判断当前蓝牙状态
    //true打开，false关闭
    public boolean getBlueToothStatus()
    {
        assert (mAdapter!=null);
        return mAdapter.isEnabled();
    }
    //打开蓝牙
    public void turnOnBlueTooth(Activity activity,int requestCode)
    {
        Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent,requestCode);//这里的“result”就是BlueTooth文件里面onActivityResult函数的result
        //mAdapter.enable();//这个函数通常是系统自动调用的,不推荐手动调用
    }
    //关闭蓝牙
    public void turnOffBlueTooth() {
        mAdapter.disable();//所以其实这里也可以写成上面的形式
    }

    //打开蓝牙可见性
    public void enableVisibly(Context context){
        //然后是搜索蓝牙，让自己的蓝牙设备可被别人发现（？
        if(mAdapter.getScanMode()!=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)//不在可被搜索的范围
        {
            Intent discoverableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //还需要额外的参数
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);//设置本机蓝牙在300秒内可见，如果是0那就是一直可见
            context.startActivity(discoverableIntent);//开始上面的那个活动(注意和上面的打开蓝牙进行对比?)
            //不需要ForResult,因为有多个设备,需要和广播一起用

        }
    }
    /**
     * 打开蓝牙可见性
     */
    public void enableVisibily(Context context){
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        context.startActivity(intent);
    }
    //查找设备
    public void findDevice()
    {
        assert(mAdapter!=null);
        mAdapter.startDiscovery();

    }

    //获取绑定设备
    public List<BluetoothDevice> getBondedDeviceList()
    {
        return new ArrayList<>(mAdapter.getBondedDevices());
    }
}
