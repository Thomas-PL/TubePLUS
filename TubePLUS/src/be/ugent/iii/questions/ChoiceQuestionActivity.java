package be.ugent.iii.questions;

import be.ugent.iii.controllers.QuestionController;
import java.util.TreeMap;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import be.ugent.iii.youtube.R;

/**
 *
 * @author Laurenz Ovaere
 */
public class ChoiceQuestionActivity extends QuestionActivity {

    private RadioGroup questionGroup;
    
    private ChoiceQuestion choiceQuestion;
    private int restore;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ContentView instellen:
        setContentView(R.layout.activity_survey_choice);      

        restore = (int) getIntent().getDoubleExtra(QuestionController.ANSWER, -1);
        
        choiceQuestion = (ChoiceQuestion) questions.get(questionIndex);

        // Viewcomponenten:
        questionGroup = (RadioGroup) findViewById(R.id.radioGroup);
        if (restore != -1) {            
            questionGroup.check(restore);
        }
        questionGroup.refreshDrawableState();
        txtQuestion = (TextView) findViewById(R.id.txtQuestion);
        txtQuestion.setText(choiceQuestion.getDescription());
        txtQuestion.setTextSize(20);
        

        // Buttonacties instellen:
        Button btnExit = (Button) findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button btnPrev = (Button) findViewById(R.id.btnPrevious);
        btnPrev.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                questionController.getPreviousQuestion(isComplete, ChoiceQuestionActivity.this);
            }
        });
        if (!questionController.hasPreviousQuestion()) {
            btnPrev.setVisibility(View.GONE);
        }
        Button btnNext = (Button) findViewById(R.id.btnNext);
        btnNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (questionGroup.getCheckedRadioButtonId() == -1) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(ChoiceQuestionActivity.this);
                    builder.setTitle("Vraag onbeantwoord");
                    builder.setMessage("Er werd geen antwoord geselecteerd voor deze vraag.");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }

                    });
                    builder.show();
                } else {
                    // Antwoord van deze vraag opslaan:
                    choiceQuestion.setSelectedChoice(questionGroup.getCheckedRadioButtonId());
                    // Controle voor de volgende vraag:
                    questionController.checkNextQuestion(questionIndex, isComplete, ChoiceQuestionActivity.this);
                }
            }
        });

        // Radiobuttons toevoegen
        TreeMap<Integer, String> choices = choiceQuestion.getChoices();
        for (Integer key : choices.keySet()) {
            RadioButton btnRadio = new RadioButton(this);
            btnRadio.setId(key);
            btnRadio.setText(choices.get(key));
            btnRadio.setTextSize(20);
            questionGroup.addView(btnRadio);
        }
    }

}
