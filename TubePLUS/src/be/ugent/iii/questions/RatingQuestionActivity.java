package be.ugent.iii.questions;

import be.ugent.iii.controllers.QuestionController;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import be.ugent.iii.youtube.R;

/**
 *
 * @author Laurenz Ovaere
 */
public class RatingQuestionActivity extends QuestionActivity {

    private RatingBar ratingBar;
    private RatingQuestion ratingQuestion;
    private float restore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ContentView instellen:
        setContentView(R.layout.activity_survey_rating);

        restore = (float)getIntent().getDoubleExtra(QuestionController.ANSWER, -1);
        
        ratingQuestion = (RatingQuestion) questions.get(questionIndex);

        // Viewcomponenten:
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        txtQuestion = (TextView) findViewById(R.id.txtQuestion);
        txtQuestion.setText(questions.get(questionIndex).getDescription());
        txtQuestion.setTextSize(20);

        if (restore != -1) {
            ratingBar.setRating(restore);
        }

        // Buttonacties instellen:
        Button btnPrev = (Button) findViewById(R.id.btnPrevious);
        btnPrev.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                questionController.getPreviousQuestion(isComplete, RatingQuestionActivity.this);
            }
        });
        if (!questionController.hasPreviousQuestion()) {
            btnPrev.setVisibility(View.GONE);
        }

        Button btnExit = (Button) findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button btnNext = (Button) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Antwoord van deze vraag opslaan:
                ratingQuestion.setRating(ratingBar.getRating());
                // Controle voor volgende vraag:
                questionController.checkNextQuestion(questionIndex, isComplete, RatingQuestionActivity.this);
            }
        });
    }

}
