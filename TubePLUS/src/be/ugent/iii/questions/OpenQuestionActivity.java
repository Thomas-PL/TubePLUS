/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.questions;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import be.ugent.iii.youtube.R;

/**
 *
 * @author Thomas
 */
public class OpenQuestionActivity extends QuestionActivity {


    private TextView txtAnswer;
    private OpenQuestion question;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        question = (OpenQuestion) questions.get(questionIndex);

        // ContentView instellen:
        setContentView(R.layout.activity_survey_open);

        txtQuestion = (TextView) findViewById(R.id.txtQuestion);
        txtQuestion.setText(question.getDescription());
        txtQuestion.setTextSize(20);

        txtAnswer = (EditText) findViewById(R.id.txtAnswer);

        // Buttonacties instellen:
        Button btnPrev = (Button) findViewById(R.id.btnPrevious);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                questionController.getPreviousQuestion(isComplete, OpenQuestionActivity.this);
            }
        });

        Button btnDone = (Button) findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                question.setAnswer(txtAnswer.getText().toString());
                questionController.checkNextQuestion(questionIndex, isComplete, OpenQuestionActivity.this);
            }
        });
    }

}
