/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.iii.questions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import be.ugent.iii.controllers.QuestionController;
import java.util.ArrayList;

/**
 *
 * @author Thomas
 */
public class QuestionActivity extends Activity {

    protected TextView txtQuestion;
    protected ArrayList<Question> questions;
    protected boolean isComplete;
    protected int questionIndex;
    protected QuestionController questionController;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //Controllers instellen:
        questionController = QuestionController.getInstance(this);
        // Parameters van intent ophalen:
        Intent startIntent = getIntent();
        questionIndex = startIntent.getIntExtra(QuestionController.QUESTION_NUMBER, 0);
        isComplete = startIntent.getBooleanExtra(QuestionController.IS_COMPLETE, true);

        if (isComplete) {
            questions = questionController.getCompleteList().getQuestions();
        } else {
            questions = questionController.getIncompleteList().getQuestions();
        }
    }

}
