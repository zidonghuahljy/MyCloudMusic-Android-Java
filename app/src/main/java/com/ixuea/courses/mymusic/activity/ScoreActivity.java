package com.ixuea.courses.mymusic.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ixuea.courses.mymusic.R;
import com.ixuea.courses.mymusic.util.StringUtil;

public class ScoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        EditText etScore = findViewById(R.id.et_score);
        Button btnClassify = findViewById(R.id.btn_classify);
        TextView tvResult = findViewById(R.id.tv_result);

        btnClassify.setOnClickListener(v -> {
            String input = etScore.getText().toString().trim();
            if (input.isEmpty()) {
                tvResult.setText("请输入分数");
                return;
            }
            int score = Integer.parseInt(input);
            String grade = StringUtil.classify(score);
            tvResult.setText("等级：" + grade);
        });
    }
}
