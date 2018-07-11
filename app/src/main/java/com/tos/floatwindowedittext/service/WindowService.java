package com.tos.floatwindowedittext.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.tos.floatwindowedittext.R;
import com.tos.floatwindowedittext.util.ImeUtils;

/**
 * 悬浮窗,实现悬浮窗EditText随意输入和不影响其他界面
 */
public class WindowService extends Service {

    public static void startService(Context context) {
        context.startService(new Intent(context, WindowService.class));
    }

    private LayoutParams wlp;
    private WindowManager windowManager;
    private LinearLayout floatView;
    private EditText et1;
    private EditText et2;
    private Button btnClose;
    private View rootView;
    private Rect rootViewRect = new Rect();
    private Handler mHandler = new Handler();
    private boolean isCanMove = true;
    private Runnable action = new Runnable() {
        @Override
        public void run() {
            isCanMove = true;//延时取消
        }
    };

    @Override
    public IBinder onBind(Intent p1) {
        return null;
    }


    public void onCreate() {
        //--------------------------------------------悬浮窗创建------------------------------------------------
        createFloatWindows();
        //--------------------------------------------找到视图------------------------------------------------
        bindFloatView();
        //--------------------------------------------绑定事件视图------------------------------------------------
        bindEvent();
    }

    public void bindEvent() {
        floatView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                rootView.getGlobalVisibleRect(rootViewRect);
                rootViewRect.left = Math.round(wlp.x );
                rootViewRect.top = Math.round(wlp.y);
                rootViewRect.right = rootViewRect.left + rootViewRect.right;
                rootViewRect.bottom = rootViewRect.top + rootViewRect.bottom;
                //W=600,H=653
                System.out.println("RECT=" + rootViewRect + ", X=" + event.getRawX() + ",Y=" + event.getRawY() + ",W=" + rootView.getWidth() + ",H=" + rootView.getHeight());
                //(627, 519 - 1227, 1172)
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //即时更新rootView(悬浮窗口,可见的那个) 的矩形位置(View是矩形的...)

                    //最关键-判断触摸是否在窗口之外
                    if (!rootViewRect.contains((int) event.getRawX(), (int) event.getRawY())) {//0,75,900,611
                        isCanMove = false;
                        outSizeClose(v);
                        Toast.makeText(WindowService.this, "你点了窗口的外面", Toast.LENGTH_SHORT).show();
                        mHandler.postDelayed(action, 1000);//延时取消
                        return true;//拦截事件
                    }
                }
                if (isCanMove) {//点击窗口外时不能触发移动窗口 ,否则窗口跟着手指走
                    wlp.x = (int) event.getRawX() - floatView.getMeasuredWidth() / 2;
                    wlp.y = (int) event.getRawY() - floatView.getMeasuredHeight() / 2 - 25;
                    et2.setText(String.format("%d,%d", wlp.x, wlp.y));//顺便显示下xy
                    windowManager.updateViewLayout(floatView, wlp);//刷新窗口
                }
                return false;
            }
        });

        btnClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View p1) {
                windowManager.removeView(floatView);
                stopSelf();
            }
        });
        setTouchShow(et2);
        setTouchShow(et1);
    }

    /**
     * 创建悬浮窗并并设置布局
     */
    public void createFloatWindows() {
        wlp = new LayoutParams();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        wlp.type = LayoutParams.TYPE_SYSTEM_ERROR;
        wlp.format = PixelFormat.RGBA_8888;
        wlp.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wlp.gravity = Gravity.LEFT | Gravity.TOP;
        wlp.x = 0;
        wlp.y = 0;
        wlp.width = LayoutParams.WRAP_CONTENT;
        wlp.height = LayoutParams.WRAP_CONTENT;
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        floatView = (LinearLayout) inflater.inflate(R.layout.window_layout, null);
        windowManager.addView(floatView, wlp);
        floatView.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    }


    /**
     * 触摸显示输入法并且去掉焦点和请求焦点
     *
     * @param et
     */
    private void setTouchShow(final EditText et) {
        et.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    wlp.flags = LayoutParams.FLAG_FULLSCREEN;//关键,全屏模式,这样就能监控到触摸点是否在悬浮窗口外
                    windowManager.updateViewLayout(floatView, wlp);
                    et.setFocusable(true);
                    et.setFocusableInTouchMode(true);
                    et.requestFocus();//请求,我要焦点
                    et.setSelection(et.getText().length());//选择末尾文本
                    v.postDelayed(new Runnable() {//关键,强制弹出输入法,需要延时,因为setFocusable(true)后,如果立马呼叫输入法时 输入法会看到输入框没有焦点,会出现不弹出输入法bug
                        @Override
                        public void run() {
                            ImeUtils.showIme(v);
                        }
                    }, 200);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 关键-超出窗口时关闭
     *
     * @param p1
     */
    private void outSizeClose(View p1) {
        wlp.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        windowManager.updateViewLayout(floatView, wlp);
        toResetEditTextStatus();
        ImeUtils.hideIme(p1);
    }

    /**
     * 关键-重置输入框焦点
     */
    private void toResetEditTextStatus() {
        et1.setFocusable(false);
        et1.setFocusableInTouchMode(false);
        et2.setFocusable(false);
        et2.setFocusableInTouchMode(false);
        //关键- Enabled(可见)关了又开,彻底去掉焦点
        et1.setEnabled(false);
        et2.setEnabled(false);
        et1.setEnabled(true);
        et2.setEnabled(true);
    }

    private void bindFloatView() {
        et1 = (EditText) floatView.findViewById(R.id.et1);
        rootView = floatView.findViewById(R.id.llRoot);
        et2 = (EditText) floatView.findViewById(R.id.et2);
        btnClose = (Button) floatView.findViewById(R.id.btn_close);
    }

}
