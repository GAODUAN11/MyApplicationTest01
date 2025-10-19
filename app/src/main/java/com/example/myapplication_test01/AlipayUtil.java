package com.example.myapplication_test01;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;

import java.util.Map;

public class AlipayUtil {
    // 支付宝支付结果回调标识
    private static final int SDK_PAY_FLAG = 1;

    /**
     * 支付宝支付业务
     * @param activity 当前Activity
     * @param orderInfo 订单信息
     * @param handler 用于接收支付结果的Handler
     */
    public static void pay(final Activity activity, final String orderInfo, final Handler handler) {
        // 必须异步调用
        Thread payThread = new Thread(() -> {
            // 构造PayTask对象
            PayTask alipay = new PayTask(activity);
            // 调用支付接口，获取支付结果
            Map<String, String> result = alipay.payV2(orderInfo, true);

            Message msg = new Message();
            msg.what = SDK_PAY_FLAG;
            msg.obj = result;
            handler.sendMessage(msg);
        });
        payThread.start();
    }

    /**
     * 处理支付结果
     * @param context 上下文
     * @param rawResult 支付结果数据
     * @param onPayResultListener 支付结果回调
     */
    public static void handlePayResult(Context context, Map<String, String> rawResult, OnPayResultListener onPayResultListener) {
        if (rawResult == null) {
            Toast.makeText(context, "支付失败，支付结果为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 截取关键结果值
        String resultStatus = rawResult.get("resultStatus");
        if (TextUtils.equals(resultStatus, "9000")) {
            // 支付成功
            Toast.makeText(context, "支付成功", Toast.LENGTH_SHORT).show();
            if (onPayResultListener != null) {
                onPayResultListener.onSuccess(rawResult);
            }
        } else {
            // 支付失败或取消
            if (TextUtils.equals(resultStatus, "8000")) {
                Toast.makeText(context, "支付结果确认中", Toast.LENGTH_SHORT).show();
                if (onPayResultListener != null) {
                    onPayResultListener.onConfirming(rawResult);
                }
            } else {
                Toast.makeText(context, "支付失败，错误码：" + resultStatus, Toast.LENGTH_SHORT).show();
                if (onPayResultListener != null) {
                    onPayResultListener.onFail(rawResult);
                }
            }
        }
    }

    /**
     * 支付结果回调接口
     */
    public interface OnPayResultListener {
        /**
         * 支付成功
         * @param result 支付结果
         */
        void onSuccess(Map<String, String> result);

        /**
         * 支付结果确认中
         * @param result 支付结果
         */
        void onConfirming(Map<String, String> result);

        /**
         * 支付失败
         * @param result 支付结果
         */
        void onFail(Map<String, String> result);
    }
}