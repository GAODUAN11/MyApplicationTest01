package com.example.myapplication_test01;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

// 添加支付宝支付相关导入
import android.content.pm.PackageManager;
import android.text.TextUtils;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // MainActivity.java (添加成员变量)
    private int score = 0; // 游戏得分
    private int energy = 5; // 能量点数，初始为5
    private int initialEnergy = 5; // 保存初始能量值，用于新游戏
    private int lastCompletedEnergy = 5; // 保存上一局完成时的能量值，用于重置
    private int filledCells = 0; // 已填入的单元格数量
    private TextView scoreTextView; // 得分显示控件
    private TextView energyTextView; // 能量点显示控件

    // 数独游戏数据
    private int[][] sudokuGrid = new int[9][9];
    private int[][] originalGrid = new int[9][9];  // 存储完整解答
    private int[][] puzzleGrid = new int[9][9];    // 存储初始题目
    private Button[][] cellButtons = new Button[9][9];
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int difficulty = 0; // 0:简单, 1:中等, 2:困难

    // 支付宝支付相关常量
    private static final int SDK_PAY_FLAG = 1;
    
    // 支付宝支付Handler
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SDK_PAY_FLAG: {
                    @SuppressWarnings("unchecked")
                    Map<String, String> result = (Map<String, String>) msg.obj;
                    AlipayUtil.handlePayResult(MainActivity.this, result, new AlipayUtil.OnPayResultListener() {
                        @Override
                        public void onSuccess(Map<String, String> result) {
                            // 从按钮Tag中获取应该增加的能量点数
                            int energyToAdd = 5; // 默认值
                            Object tag = findViewById(R.id.btnPay).getTag();
                            if (tag instanceof Integer) {
                                energyToAdd = (Integer) tag;
                            }
                            
                            energy += energyToAdd;
                            updateEnergyDisplay();
                            Toast.makeText(MainActivity.this, "支付成功，获得" + energyToAdd + "点能量！", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onConfirming(Map<String, String> result) {
                            Toast.makeText(MainActivity.this, "支付结果确认中...", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFail(Map<String, String> result) {
                            Toast.makeText(MainActivity.this, "支付失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                }
                default:
                    break;
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化数独网格
        initializeSudokuGrid();
        // 生成数独谜题
        generateRandomSudoku(difficulty);
        // 创建UI网格
        createGridUI();
        // 设置数字按钮点击事件
        setupNumberButtons();
        // 设置控制按钮点击事件
        setupControlButtons();

        // 初始化得分显示控件
        scoreTextView = findViewById(R.id.score);
        
        // 初始化能量点显示控件
        energyTextView = findViewById(R.id.energy);
        
        // 初始化数独网格
        initializeSudokuGrid();
        // 生成数独谜题
        generateRandomSudoku(difficulty);
        // 创建UI网格
        createGridUI();
        // 设置数字按钮点击事件
        setupNumberButtons();
        // 设置控制按钮点击事件
        setupControlButtons();
        
        // 初始化能量点和已填入单元格计数
        energy = 5; // 初始能量值
        initialEnergy = 5; // 保存初始能量值
        lastCompletedEnergy = 5; // 初始完成时能量值
        filledCells = 0;
        updateEnergyDisplay();

        // 设置支付按钮点击事件
        setupPayButton();
    }

    // 初始化数独网格UI
    private void createGridUI() {
        GridLayout gridLayout = findViewById(R.id.sudokuGrid);
        gridLayout.removeAllViews(); // 清除现有视图

        // 精确计算单元格尺寸，确保9列完整显示
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        // 减去左右padding和网格边框占用的空间
        int availableWidth = screenWidth - dpToPx(32); // 32dp = 左右各16dp padding
        int cellSize = availableWidth / 9; // 平均分配给9列

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Button button = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);
                params.width = cellSize;
                params.height = cellSize;

                // 添加边框以区分3x3子网格
                int margin = 1;
                params.setMargins(
                        (j % 3 == 0) ? margin * 2 : margin,
                        (i % 3 == 0) ? margin * 2 : margin,
                        (j % 3 == 2) ? margin * 2 : margin,
                        (i % 3 == 2) ? margin * 2 : margin
                );

                button.setLayoutParams(params);
                button.setTextSize(18);
                button.setPadding(0, 0, 0, 0);

                // 系统预设数字样式
                if (sudokuGrid[i][j] != 0) {
                    button.setText(String.valueOf(sudokuGrid[i][j]));
                    button.setTextColor(getResources().getColor(android.R.color.white));
                    button.setBackgroundResource(R.drawable.cell_fixed); // 深色背景
                    button.setEnabled(false);
                } else {
                    // 空白单元格样式
                    button.setBackgroundResource(R.drawable.cell_default);
                    button.setEnabled(true);
                    final int row = i;
                    final int col = j;
                    button.setOnClickListener(v -> {
                        selectCell(row, col);
                    });
                }

                cellButtons[i][j] = button;
                gridLayout.addView(button);
            }
        }
    }

    // 设置控制按钮点击事件 - 添加新游戏按钮处理
    private void setupControlButtons() {
        // 新游戏按钮
        findViewById(R.id.btnNewGame).setOnClickListener(v -> showDifficultyDialog());

        // 重置按钮
        findViewById(R.id.btnReset).setOnClickListener(v -> resetGame());

        // 提示按钮
        findViewById(R.id.btnCheck).setOnClickListener(v -> showHint());
    }

    // 设置支付按钮点击事件
    private void setupPayButton() {
        Button payButton = findViewById(R.id.btnPay);
        if (payButton != null) {
            payButton.setOnClickListener(v -> showPayOptionsDialog());
        } else {
            Toast.makeText(this, "支付按钮未找到", Toast.LENGTH_SHORT).show();
        }
    }

    // 显示支付选项对话框
    private void showPayOptionsDialog() {
        final String[] options = {"2元充值1能量点", "3元充值2能量点", "5元充值4能量点"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("充值能量点")
               .setItems(options, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                       switch (which) {
                           case 0:
                               // 支付2元获得1点能量
                               payWithAlipay("2.00", "能量点充值", "充值1点能量", 1);
                               break;
                           case 1:
                               // 支付3元获得2点能量
                               payWithAlipay("3.00", "能量点充值", "充值2点能量", 2);
                               break;
                           case 2:
                               // 支付5元获得4点能量
                               payWithAlipay("5.00", "能量点充值", "充值4点能量", 4);
                               break;
                       }
                   }
               })
               .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // 用户取消了对话框
                   }
               });
        builder.create().show();
    }

    // 调用支付宝支付
    private void payWithAlipay(String money, String subject, String body, int energyPoints) {
        // 检查是否安装了支付宝
        if (!checkAliPayInstalled()) {
            Toast.makeText(this, "请先安装支付宝客户端", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构造订单信息（实际项目中应该从服务器获取）
        String orderInfo = getOrderInfo(subject, body, money);
        
        // 将能量点数存储在Tag中，以便支付完成后使用
        findViewById(R.id.btnPay).setTag(energyPoints);

        // 调用支付宝支付
        AlipayUtil.pay(this, orderInfo, mHandler);
    }

    /**
     * 构造支付订单信息（示例）
     * 实际项目中应该从服务器获取签名后的订单信息
     * 
     * @param subject 商品名称
     * @param body 商品描述
     * @param money 金额
     * @return 订单信息
     */
    private String getOrderInfo(String subject, String body, String money) {
        // 签约合作者身份ID（沙箱环境）
        String orderInfo = "partner=\"2088721084755160\"";

        // 签约卖家支付宝账号（沙箱环境）
        orderInfo += "&seller_id=\"feouyv6674@sandbox.com\"";

        // 商户网站唯一订单号（实际项目中应该是唯一的）
        orderInfo += "&out_trade_no=\"sudoku_energy_" + System.currentTimeMillis() + "\"";

        // 商品名称
        orderInfo += "&subject=\"" + subject + "\"";

        // 商品详情
        orderInfo += "&body=\"" + body + "\"";

        // 商品金额
        orderInfo += "&total_fee=\"" + money + "\"";

        // 服务器异步通知页面路径（沙箱测试环境）
        orderInfo += "&notify_url=\"http://notify.msp.hk/notify.htm\"";

        // 服务接口名称， 固定值
        orderInfo += "&service=\"mobile.securitypay.pay\"";

        // 支付类型， 固定值
        orderInfo += "&payment_type=\"1\"";

        // 参数编码， 固定值
        orderInfo += "&_input_charset=\"utf-8\"";

        // 设置未付款交易的超时时间
        // 默认30分钟，一旦超时，该笔交易就会自动被关闭。
        // 取值范围：1m～15d。
        // m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
        // 该参数数值不接受小数点，如1.5h，可转换为90m。
        orderInfo += "&it_b_pay=\"30m\"";

        // 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
        orderInfo += "&return_url=\"m.alipay.com\"";

        // 商户私钥，RSA私钥需要使用PKCS8格式
        // 注意：在实际项目中，这部分应该由服务器生成，不应该在客户端保存私钥
        orderInfo += "&key_type=\"PKCS8\"";

        // 添加防钓鱼时间戳
        orderInfo += "&anti_phishing_key=\"\"";

        // 客户端号
        orderInfo += "&exter_invoke_ip=\"\"";

        // 调用银行卡支付，需配置此参数，参与签名， 固定值
        // orderInfo += "&paymethod=\"expressGateway\"";

        return orderInfo;
    }

    /**
     * 检查是否安装了支付宝客户端（沙箱版）
     */
    private boolean checkAliPayInstalled() {
        try {
            // 只检查沙箱版支付宝
            PackageInfo info = getPackageManager().getPackageInfo("com.eg.android.AlipayGphoneRC", 0);
            return info != null;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    // 显示支付对话框（旧方法，保留作为备用）
    private void showPayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("充值能量点");
        builder.setMessage("点击充值按钮可增加能量点，用于获取提示。");

        builder.setPositiveButton("充值", (dialog, which) -> {
            // 增加5点能量
            energy += 5;
            updateEnergyDisplay();
            Toast.makeText(this, "充值成功，获得5点能量！", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 显示难度选择对话框
    private void showDifficultyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择难度");
        final String[] difficulties = {"简单", "中等", "困难"};

        builder.setSingleChoiceItems(difficulties, difficulty, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                difficulty = which;
            }
        });

        builder.setPositiveButton("开始", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 更新难度显示
                TextView statusTextView = findViewById(R.id.status);
                statusTextView.setText("难度：" + difficulties[difficulty]);

                //新游戏重置得分和能量值
                score = 0;
                energy = 5; // 重置能量值为初始值
                initialEnergy = 5; // 保存初始能量值
                filledCells = 0; // 重置已填入单元格计数
                updateScoreDisplay();
                updateEnergyDisplay();

                // 生成新的数独谜题
                generateRandomSudoku(difficulty);

                // 重新创建UI
                createGridUI();

                // 重置选中状态
                selectedRow = -1;
                selectedCol = -1;

                Toast.makeText(MainActivity.this, "新游戏已开始", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // 随机生成数独谜题
    private void generateRandomSudoku(int difficulty) {
        // 清空网格
        initializeSudokuGrid();

        // 生成完整的数独解
        fillGrid(0, 0);

        // 保存完整解作为提示答案
        for (int i = 0; i < 9; i++) {
            System.arraycopy(sudokuGrid[i], 0, originalGrid[i], 0, 9);
        }

        // 根据难度移除不同数量的数字
        int cellsToRemove;
        switch (difficulty) {
            case 0: // 简单
                cellsToRemove = 30;
                break;
            case 1: // 中等
                cellsToRemove = 40;
                break;
            case 2: // 困难
                cellsToRemove = 50;
                break;
            default:
                cellsToRemove = 30;
        }

        removeCells(cellsToRemove);

        // 保存初始题目用于重置
        for (int i = 0; i < 9; i++) {
            System.arraycopy(sudokuGrid[i], 0, puzzleGrid[i], 0, 9);
        }
    }

    // 填充数独网格（递归回溯算法）
    private boolean fillGrid(int row, int col) {
        // 如果到达最后一行，返回true表示填充完成
        if (row == 9) {
            return true;
        }

        // 计算下一个单元格的位置
        int nextRow = (col == 8) ? row + 1 : row;
        int nextCol = (col == 8) ? 0 : col + 1;

        // 尝试随机数字1-9
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        shuffleArray(numbers); // 随机排序

        for (int num : numbers) {
            if (isValidMove(row, col, num)) {
                sudokuGrid[row][col] = num;
                if (fillGrid(nextRow, nextCol)) {
                    return true;
                }
                sudokuGrid[row][col] = 0; // 回溯
            }
        }

        return false; // 没有有效数字可用，回溯
    }

    // 随机打乱数组顺序
    private void shuffleArray(int[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    // 从完整解中移除单元格以创建谜题
    private void removeCells(int count) {
        Random random = new Random();
        int removed = 0;

        while (removed < count) {
            int row = random.nextInt(9);
            int col = random.nextInt(9);

            // 只移除尚未移除的单元格
            if (sudokuGrid[row][col] != 0) {
                sudokuGrid[row][col] = 0;
                removed++;
            }
        }
    }

    // 选择单元格
    private void selectCell(int row, int col) {
        // 重置之前选中的单元格样式
        if (selectedRow != -1 && selectedCol != -1) {
            // 如果是用户填写的数字，恢复为固定样式
            if (sudokuGrid[selectedRow][selectedCol] != 0 && originalGrid[selectedRow][selectedCol] == 0) {
                cellButtons[selectedRow][selectedCol].setBackgroundResource(R.drawable.cell_fixed);
                cellButtons[selectedRow][selectedCol].setTextColor(getResources().getColor(android.R.color.white));
            } else if (puzzleGrid[selectedRow][selectedCol] == 0) {
                // 空白单元格恢复默认样式
                cellButtons[selectedRow][selectedCol].setBackgroundResource(R.drawable.cell_default);
            }
        }

        // 设置新选中的单元格样式（只对可编辑单元格生效）
        if (puzzleGrid[row][col] == 0) {
            selectedRow = row;
            selectedCol = col;
            cellButtons[row][col].setBackgroundResource(R.drawable.cell_selected);
            // 选中时文字颜色调整为黑色以确保可见性
            if (sudokuGrid[row][col] != 0) {
                cellButtons[row][col].setTextColor(getResources().getColor(android.R.color.black));
            }
        } else {
            // 选中固定单元格时清除选中状态
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    // 设置数字按钮点击事件
    private void setupNumberButtons() {
        findViewById(R.id.btn1).setOnClickListener(v -> enterNumber(1));
        findViewById(R.id.btn2).setOnClickListener(v -> enterNumber(2));
        findViewById(R.id.btn3).setOnClickListener(v -> enterNumber(3));
        findViewById(R.id.btn4).setOnClickListener(v -> enterNumber(4));
        findViewById(R.id.btn5).setOnClickListener(v -> enterNumber(5));
        findViewById(R.id.btn6).setOnClickListener(v -> enterNumber(6));
        findViewById(R.id.btn7).setOnClickListener(v -> enterNumber(7));
        findViewById(R.id.btn8).setOnClickListener(v -> enterNumber(8));
        findViewById(R.id.btn9).setOnClickListener(v -> enterNumber(9));
        findViewById(R.id.btnClear).setOnClickListener(v -> clearNumber());
    }

    // 输入数字
    private void enterNumber(int number) {
        if (selectedRow != -1 && selectedCol != -1) {
            // 检查数字是否符合数独规则
            if (isValidMove(selectedRow, selectedCol, number)) {
                sudokuGrid[selectedRow][selectedCol] = number;
                cellButtons[selectedRow][selectedCol].setText(String.valueOf(number));
                // 设置与系统预设数字相同的样式
                cellButtons[selectedRow][selectedCol].setTextColor(getResources().getColor(android.R.color.white));
                cellButtons[selectedRow][selectedCol].setBackgroundResource(R.drawable.cell_fixed);
                // 保持单元格可点击以支持修改
                cellButtons[selectedRow][selectedCol].setEnabled(true);

                // 清除选中状态
                selectedRow = -1;
                selectedCol = -1;

                // 增加已填入单元格计数
                filledCells++;
                // 每填入5个单元格增加1点能量
                if (filledCells % 5 == 0) {
                    energy++;
                    updateEnergyDisplay();
                }
                
                // 检查游戏是否完成
                if (isGameComplete()) {
                    score += 5;
                    // 保存完成时的能量值，用于重置
                    lastCompletedEnergy = energy;
                    updateScoreDisplay();
                    Toast.makeText(this, "恭喜你完成了数独！获得5分，即将开始下一局", Toast.LENGTH_LONG).show();
                    
                    // 延迟一段时间后自动开始下一局游戏
                    findViewById(R.id.btnNewGame).postDelayed(() -> {
                        generateRandomSudoku(difficulty);
                        createGridUI();
                        selectedRow = -1;
                        selectedCol = -1;
                        // 重置已填入单元格计数
                        filledCells = 0;
                        Toast.makeText(this, "新局游戏开始", Toast.LENGTH_SHORT).show();
                    }, 2000);
                }
            } else {
                Toast.makeText(this, "这个数字在这里无效", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "请先选择一个单元格", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateScoreDisplay() {
        scoreTextView.setText("得分：" + score);
    }
    
    // 更新能量点显示
    private void updateEnergyDisplay() {
        if (energyTextView != null) {
            energyTextView.setText("能量点：" + energy);
        }
    }

    // 清除数字
    private void clearNumber() {
        if (selectedRow != -1 && selectedCol != -1) {
            sudokuGrid[selectedRow][selectedCol] = 0;
            cellButtons[selectedRow][selectedCol].setText("");
            // 恢复为空白单元格样式
            cellButtons[selectedRow][selectedCol].setBackgroundResource(R.drawable.cell_default);
            cellButtons[selectedRow][selectedCol].setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    // 重置游戏
    private void resetGame() {
        // 恢复初始题目
        for (int i = 0; i < 9; i++) {
            System.arraycopy(puzzleGrid[i], 0, sudokuGrid[i], 0, 9);
        }

        // 更新UI
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (sudokuGrid[i][j] == 0) {
                    cellButtons[i][j].setText("");
                    cellButtons[i][j].setBackgroundResource(R.drawable.cell_default);
                } else {
                    cellButtons[i][j].setText(String.valueOf(sudokuGrid[i][j]));
                    cellButtons[i][j].setBackgroundResource(R.drawable.cell_fixed);
                    cellButtons[i][j].setTextColor(getResources().getColor(android.R.color.white));
                }
            }
        }

        // 恢复能量值到上一局完成时的状态
        energy = lastCompletedEnergy;
        filledCells = 0; // 重置已填入单元格计数
        updateEnergyDisplay();

        selectedRow = -1;
        selectedCol = -1;
        Toast.makeText(this, "游戏已重置", Toast.LENGTH_SHORT).show();
    }

    // 提供提示
    private void showHint() {
        // 检查是否有足够的能量点
        if (energy <= 0) {
            Toast.makeText(this, "能量点不足，无法使用提示功能", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 如果用户已经选择了一个单元格
        if (selectedRow != -1 && selectedCol != -1) {
            // 如果该单元格是空白的
            if (sudokuGrid[selectedRow][selectedCol] == 0) {
                // 获取正确答案
                int correctNumber = originalGrid[selectedRow][selectedCol];
                if (correctNumber != 0) {
                    // 消耗1点能量
                    energy--;
                    updateEnergyDisplay();
                    
                    // 填入正确答案
                    sudokuGrid[selectedRow][selectedCol] = correctNumber;
                    cellButtons[selectedRow][selectedCol].setText(String.valueOf(correctNumber));
                    cellButtons[selectedRow][selectedCol].setTextColor(getResources().getColor(android.R.color.white));
                    cellButtons[selectedRow][selectedCol].setBackgroundResource(R.drawable.cell_fixed);
                    
                    // 清除选中状态，与手动输入数字后行为一致
                    selectedRow = -1;
                    selectedCol = -1;
                    
                    Toast.makeText(this, "已填入提示数字", Toast.LENGTH_SHORT).show();
                    
                    // 检查游戏是否完成
                    if (isGameComplete()) {
                        score += 5;
                        lastCompletedEnergy = energy; // 保存完成时的能量值
                        updateScoreDisplay();
                        Toast.makeText(this, "恭喜你完成了数独！获得5分，即将开始下一局", Toast.LENGTH_LONG).show();
                        
                        // 延迟一段时间后自动开始下一局游戏
                        findViewById(R.id.btnNewGame).postDelayed(() -> {
                            generateRandomSudoku(difficulty);
                            createGridUI();
                            selectedRow = -1;
                            selectedCol = -1;
                            // 重置已填入单元格计数
                            filledCells = 0;
                            Toast.makeText(this, "新局游戏开始", Toast.LENGTH_SHORT).show();
                        }, 2000);
                    }
                }
            } else {
                Toast.makeText(this, "该单元格已经有数字了", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 随机选择一个空白单元格并给出提示
            boolean found = false;
            for (int i = 0; i < 9 && !found; i++) {
                for (int j = 0; j < 9 && !found; j++) {
                    if (sudokuGrid[i][j] == 0) {
                        // 获取正确答案
                        int correctNumber = originalGrid[i][j];
                        if (correctNumber != 0) {
                            // 消耗1点能量
                            energy--;
                            updateEnergyDisplay();
                            
                            // 填入正确答案
                            sudokuGrid[i][j] = correctNumber;
                            cellButtons[i][j].setText(String.valueOf(correctNumber));
                            cellButtons[i][j].setTextColor(getResources().getColor(android.R.color.white));
                            cellButtons[i][j].setBackgroundResource(R.drawable.cell_fixed);
                            
                            Toast.makeText(this, "提示：在第" + (i+1) + "行第" + (j+1) + "列填入" + correctNumber, Toast.LENGTH_LONG).show();
                            found = true;
                            
                            // 检查游戏是否完成
                            if (isGameComplete()) {
                                score += 5;
                                lastCompletedEnergy = energy; // 保存完成时的能量值
                                updateScoreDisplay();
                                Toast.makeText(this, "恭喜你完成了数独！获得5分，即将开始下一局", Toast.LENGTH_LONG).show();
                                
                                // 延迟一段时间后自动开始下一局游戏
                                findViewById(R.id.btnNewGame).postDelayed(() -> {
                                    generateRandomSudoku(difficulty);
                                    createGridUI();
                                    selectedRow = -1;
                                    selectedCol = -1;
                                    // 重置已填入单元格计数
                                    filledCells = 0;
                                    Toast.makeText(this, "新局游戏开始", Toast.LENGTH_SHORT).show();
                                }, 2000);
                            }
                        }
                    }
                }
            }
            
            if (!found) {
                Toast.makeText(this, "没有可提示的单元格", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 初始化数独网格
    private void initializeSudokuGrid() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                sudokuGrid[i][j] = 0;
            }
        }
    }

    // 检查移动是否有效
    private boolean isValidMove(int row, int col, int num) {
        // 检查行
        for (int i = 0; i < 9; i++) {
            if (sudokuGrid[row][i] == num) {
                return false;
            }
        }

        // 检查列
        for (int i = 0; i < 9; i++) {
            if (sudokuGrid[i][col] == num) {
                return false;
            }
        }

        // 检查3x3子网格
        int startRow = row - row % 3;
        int startCol = col - col % 3;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (sudokuGrid[startRow + i][startCol + j] == num) {
                    return false;
                }
            }
        }

        return true;
    }

    // 检查游戏是否完成
    private boolean isGameComplete() {
        // 检查是否有空格
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (sudokuGrid[i][j] == 0) {
                    return false;
                }
            }
        }

        // 检查所有行、列和子网格是否有效
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int num = sudokuGrid[i][j];
                sudokuGrid[i][j] = 0; // 临时清空以便检查
                if (!isValidMove(i, j, num)) {
                    sudokuGrid[i][j] = num; // 恢复值
                    return false;
                }
                sudokuGrid[i][j] = num; // 恢复值
            }
        }

        return true;
    }

    // dp转px工具方法
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

}