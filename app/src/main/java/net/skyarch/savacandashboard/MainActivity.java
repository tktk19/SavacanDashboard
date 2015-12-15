package net.skyarch.savacandashboard;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.*;
import java.util.List;

import android.hardware.usb.*;
import com.hoho.android.usbserial.driver.*;

public class MainActivity extends ActionBarActivity {
    // ログ用
    private static final String TAG = MainActivity.class.getSimpleName();
    // テキストビュー
    private TextView textView1;
    private TextView textView2;

    // X軸最低スワイプ距離
    private static final int SWIPE_MIN_DISTANCE = 50;

    // X軸最低スワイプスピード
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    // Y軸の移動距離　これ以上なら横移動を判定しない
    private static final int SWIPE_MAX_OFF_PATH = 250;

    // タッチイベントを処理するためのインタフェース
    private GestureDetector mGestureDetector;

    // シリアル通信用ポート
    private UsbSerialPort port;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGestureDetector = new GestureDetector(this, mOnGestureListener);
        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);

        // シリアル通信処理
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            Log.d(TAG, "Device NotFound serial communicate terminate");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            return;
        }

        // Read some data! Most have just one port (port 0).
        port = driver.getPorts().get(0);
        try {
            port.open(connection);
            port.setParameters(9960, 8, 1, 0);
//            byte buffer[] = new byte[16];
//            int numBytesRead = port.read(buffer, 1000);
//            Log.d(TAG, "Read " + numBytesRead + " bytes.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // タッチイベント
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    // タッチイベントのリスナー
    private final GestureDetector.SimpleOnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

        // フリックイベント
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

            try {

                // 移動距離・スピードを出力
                float distance_x = Math.abs((event1.getX() - event2.getX()));
                float velocity_x = Math.abs(velocityX);
                textView1.setText("横の移動距離:" + distance_x + " 横の移動スピード:" + velocity_x);

                // Y軸の移動距離が大きすぎる場合
                if (Math.abs(event1.getY() - event2.getY()) > SWIPE_MAX_OFF_PATH) {
                    textView2.setText("縦の移動距離が大きすぎ");
                }
                // 開始位置から終了位置の移動距離が指定値より大きい
                // X軸の移動速度が指定値より大きい
                else if  (event1.getX() - event2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    textView2.setText("右から左");

                    ImageView imageView = (ImageView)findViewById(R.id.imageView);
                    Drawable drawable = getResources().getDrawable(R.drawable.mode_g);
                    imageView.setImageDrawable(drawable);
                    try {
                        port.write("g".getBytes("UTF-8"),1);
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }
                }
                // 終了位置から開始位置の移動距離が指定値より大きい
                // X軸の移動速度が指定値より大きい
                else if (event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    textView2.setText("左から右");

                    ImageView imageView = (ImageView)findViewById(R.id.imageView);
                    Drawable drawable = getResources().getDrawable(R.drawable.mode_r);
                    imageView.setImageDrawable(drawable);

                    try {
                        port.write("r".getBytes("UTF-8"),1);
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                // TODO
            }

            return false;
        }
    };
}