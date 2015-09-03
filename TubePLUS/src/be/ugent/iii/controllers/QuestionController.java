package be.ugent.iii.controllers;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import be.ugent.iii.activities.OptimizerPrefsActivity;
import be.ugent.iii.database.QuestionsCommand;
import be.ugent.iii.operators.NetworkGeneration;
import be.ugent.iii.optimizer.AnalyseData;
import be.ugent.iii.optimizer.VideoOptimizer;
import be.ugent.iii.questions.Action;
import be.ugent.iii.questions.ChoiceQuestion;
import be.ugent.iii.questions.ChoiceQuestionActivity;
import be.ugent.iii.questions.Dependency;
import be.ugent.iii.questions.OpenQuestion;
import be.ugent.iii.questions.OpenQuestionActivity;
import be.ugent.iii.questions.Question;
import be.ugent.iii.questions.QuestionList;
import be.ugent.iii.questions.RatingQuestion;
import be.ugent.iii.questions.RatingQuestionActivity;
import be.ugent.iii.tasks.PushQuestionsTask;
import be.ugent.iii.youtube.R;
import java.util.Stack;

/**
 * Controller klasse voor het beheer van de vragen
 *
 * @author Thomas
 */
public class QuestionController {

    public static final String QUESTION_NUMBER = "vraagNummer";
    public static final String IS_COMPLETE = "isVolledig";
    public static final String ANSWER = "antwoord";
    private QuestionList completeList;
    private QuestionList incompleteList;
    private long sessionIdentifier;
    private Stack<Integer> askedQuestions = new Stack<Integer>();
    private Context context;

    private static QuestionController instance;

    /**
     * Singleton pattern
     * @param c
     * @return 
     */
    public static QuestionController getInstance(Context c) {
        if (instance == null) {
            instance = new QuestionController(c);
        }
        return instance;
    }

    /**
     * private constructor
     * @param c 
     */
    private QuestionController(Context c) {
        context = c;
        Serializer serializer = new Persister();
        try {
            completeList = serializer.read(QuestionList.class, context.getResources().openRawResource(R.raw.vragen_volledig));
            incompleteList = serializer.read(QuestionList.class, context.getResources().openRawResource(R.raw.vragen_onvolledig));
        } catch (Exception ex) {
            Log.e("QuestionController", "Error initializing questionLists");
        }
    }

    /**
     * Geef de volledige vragenlijst terug voor als de gebruiker het volledig filmfragment bekeek
     * @return 
     */
    public QuestionList getCompleteList() {
        return completeList;
    }

    /**
     * Geef de onvolledige vragenlijst terug voor als de gebruiker niet het volledig filmfragment bekeek
     * @return 
     */
    public QuestionList getIncompleteList() {
        return incompleteList;
    }

    /**
     * Zet de sessionidentifier
     * @param sessionIdentifier 
     */
    public void setSessionIdentifier(long sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }

    /**
     * Was er een vorige vraag?
     * Deze methode wordt gebruikt om de back-knop te enabelen.
     * @return 
     */
    public boolean hasPreviousQuestion() {
        return !askedQuestions.isEmpty();
    }

    /**
     * Geef de vorige vraag terug die de gebruiker beantwoorde.
     * @param isComplete
     * @param questionActivity 
     */
    public void getPreviousQuestion(boolean isComplete, Activity questionActivity) {
        int previousIndex = askedQuestions.pop();

        // Vragenlijst ophalen:
        ArrayList<Question> questions;
        if (isComplete) {
            questions = completeList.getQuestions();
        } else {
            questions = incompleteList.getQuestions();
        }

        Question previous = questions.get(previousIndex);
        Intent survey = null;
        double answer = 0;
        if (previous instanceof RatingQuestion) {
            answer = ((RatingQuestion) previous).getRating();
            survey = new Intent(questionActivity, RatingQuestionActivity.class);
        } else if (previous instanceof ChoiceQuestion) {
            answer = ((ChoiceQuestion) previous).getSelectedChoice() * 1.0;
            survey = new Intent(questionActivity, ChoiceQuestionActivity.class);
        }

        // Activity starten:
        if (survey != null) {
            survey.putExtra(QuestionController.ANSWER, answer);
            survey.putExtra(QuestionController.QUESTION_NUMBER, previousIndex);
            survey.putExtra(QuestionController.IS_COMPLETE, isComplete);
            questionActivity.startActivity(survey);
        }
        questionActivity.finish();
    }

    /**
     * Zoek een volgende vraag. Het framework zal zoeken naar vragen met een afhankelijkheid
     * ten opzichte van de net beantwoorde vraag en indien er een gevonden wordt, zal deze
     * ingesteld worden als huidige vraag en gesteld worden.
     * Indien er geen vragen meer gevonden worden, wordt het vragensysteem gestopt.
     * @param questionIndex
     * @param isComplete
     * @param questionActivity 
     */
    public void checkNextQuestion(int questionIndex, boolean isComplete, Activity questionActivity) {
        // Eventuele acties gekoppeld aan de huidige vraag uitvoeren:
        executeActions(questionIndex, isComplete, questionActivity);
        // Voorbereiding databasetransacties:
        String listType;
        // Vragenlijst ophalen:
        ArrayList<Question> questions;
        if (isComplete) {
            questions = completeList.getQuestions();
            listType = "Complete";
        } else {
            questions = incompleteList.getQuestions();
            listType = "Incomplete";
        }
        //Antwoord opslaan
        // Database-acties doorvoeren:
        QuestionsCommand questionsDb = new QuestionsCommand();
        Question currentQuestion = questions.get(questionIndex);
        askedQuestions.push(questionIndex);

        String deviceID = ((TelephonyManager) FrameworkController.getInstance().geefContext().getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

        String method;
        if (PlayerController.getInstance().isUsingYoutubeMethod()) {
            method = "You";
        } else {
            method = "me";
        }

        if (currentQuestion instanceof RatingQuestion) {
            String rating = Double.toString(((RatingQuestion) currentQuestion).getRating());
            questionsDb.addAnswer(sessionIdentifier, deviceID, currentQuestion.getId(), listType, currentQuestion.getDescription(), "RatingQuestion", rating, method);
        } else if (currentQuestion instanceof ChoiceQuestion) {
            String choice = ((ChoiceQuestion) currentQuestion).getSelectedChoiceDescription();
            questionsDb.addAnswer(sessionIdentifier, deviceID, currentQuestion.getId(), listType, currentQuestion.getDescription(), "ChoiceQuestion", choice, method);
        } else {
            String answer = ((OpenQuestion) currentQuestion).getAnswer();
            if (answer.length() != 0) {
                questionsDb.addAnswer(sessionIdentifier, deviceID, currentQuestion.getId(), listType, currentQuestion.getDescription(), "OpenQuestion", answer, method);
            }
        }

        // Controle voor de volgende vraag:
        if (questionIndex + 1 < questions.size()) {
            // Vraag bepalen a.d.h.v. de dependencies:
            boolean success = false;
            int index = questionIndex + 1;
            while (index < questions.size() && !success) {
                // Blijven zoeken naar een volgende vraag:
                Question possibleQuestion = questions.get(index);
                ArrayList<Dependency> dependencies = possibleQuestion.getDependencies();
                int numberOk = 0;
                for (Dependency dependency : dependencies) {
                    if (questions.get(dependency.getQuestionNumber() - 1).betweenBorders(dependency.getLowerlimit(), dependency.getUppderlimit())) {
                        numberOk++;
                    }
                }
                // Als alle dependencies in orde zijn, dan wordt de
                // vraag gesteld:
                if (numberOk == dependencies.size()) {
                    success = true;
                }
                // Volgende vraag controleren:
                index++;
            }
            // Vraag gevonden?
            if (success) {
                index--;
                Intent survey = null;
                if (questions.get(index) instanceof RatingQuestion) {
                    survey = new Intent(questionActivity, RatingQuestionActivity.class);
                } else if (questions.get(index) instanceof ChoiceQuestion) {
                    survey = new Intent(questionActivity, ChoiceQuestionActivity.class);
                } else if (questions.get(index) instanceof OpenQuestion) {
                    survey = new Intent(questionActivity, OpenQuestionActivity.class);
                }

                // Activity starten:
                if (survey != null) {
                    survey.putExtra(QuestionController.QUESTION_NUMBER, index);
                    survey.putExtra(QuestionController.IS_COMPLETE, isComplete);
                    questionActivity.startActivity(survey);
                }
                questionActivity.finish();
            } else {
                finishQuestions(questions, questionActivity);
            }
        } else {
            finishQuestions(questions, questionActivity);
        }
    }

    /**
     * Stop het vragensysteem.
     * De antwoorden worden voor de zekerheid nog eens naar de log gestuurd.
     * Uiteindelijk wordt de pushQuestionsTask gestart die zal proberen de lokaal opgeslagen vragen
     * te versturen naar de externe server.
     * @param questions
     * @param questionActivity 
     */
    private void finishQuestions(ArrayList<Question> questions, Activity questionActivity) {
        // Geen vragen meer, aan het einde van de vragenlijst.
        // Antwoorden uitschrijven:
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i) instanceof ChoiceQuestion) {
                // ChoiceQuestion:
                Log.v("Deserializer", "Vraag " + i + ": " + ((ChoiceQuestion) questions.get(i)).getSelectedChoice());
            } else if (questions.get(i) instanceof OpenQuestion) {
                // OpenQuestion:
                Log.v("Deserializer", "Vraag " + i + ": " + ((OpenQuestion) questions.get(i)).getAnswer());
            } else {
                // RatingQuestion:
                Log.v("Deserializer", "Vraag " + i + ": " + ((RatingQuestion) questions.get(i)).getRating());
            }
        }
        askedQuestions.empty();
        questionActivity.finish();

        //Antwoorden doorsturen naar externe database
        PushQuestionsTask questionTask = new PushQuestionsTask();
        questionTask.execute();
    }

    /**
     * Voer de acties uit die aan de vraag naar keuze gekoppeld waren.
     * @param questionIndex
     * @param isComplete
     * @param questionActivity 
     */
    private void executeActions(int questionIndex, boolean isComplete, Activity questionActivity) {
        // Vragenlijst ophalen:
        QuestionList questionList;
        if (isComplete) {
            questionList = completeList;
        } else {
            questionList = incompleteList;
        }
        // Activiteiten die gekoppeld zijn aan de vraag uitvoeren:
        Question question = questionList.getQuestions().get(questionIndex);
        ArrayList<Action> actions = question.getActions();
        for (Action action : actions) {
            if (question instanceof RatingQuestion && action.getAnswer() == ((RatingQuestion) question).getRating()) {
                executeAction(action, questionActivity, questionList);
            } else if (question instanceof ChoiceQuestion && action.getAnswer() == ((ChoiceQuestion) question).getSelectedChoice()) {
                executeAction(action, questionActivity, questionList);
            }
        }
    }

    /**
     * Voer de acties uit die aan de vraag naar keuze gekoppeld waren.
     * @param action
     * @param questionActivity
     * @param questionList 
     */
    private void executeAction(Action action, Activity questionActivity, QuestionList questionList) {
        SharedPreferences optimizerPrefs = questionActivity.getSharedPreferences(OptimizerPrefsActivity.OPTIMIZER_FILE, Context.MODE_PRIVATE);

        if (action.getAction().equals("increaseQuality")) {
            AnalyseData analyseData = questionList.getAnalyseData();
            if (!analyseData.isWifi() && !analyseData.isRoaming() && !analyseData.isLocationSpeedExceeded()) {
                // Rechtstreekse mobile aanpassing (geen roaming)
                changeMobileSetting(analyseData.getMobileNetworkGeneration(), false, true, optimizerPrefs);
            } else if (!analyseData.isWifi() && analyseData.isRoaming() && !analyseData.isLocationSpeedExceeded()) {
                // Rechtstreekse mobile aanpassing (wel roaming)
                changeMobileSetting(analyseData.getMobileNetworkGeneration(), true, true, optimizerPrefs);
            } else if (!analyseData.isWifi() && analyseData.isLocationSpeedExceeded()) {
                // Aanpassing aan de locatie-instelling
                changeLocationSetting(false, optimizerPrefs);
            }
        } else if (action.getAction().equals("decreaseQuality")) {
            AnalyseData analyseData = questionList.getAnalyseData();
            if (!analyseData.isWifi() && !analyseData.isRoaming() && !analyseData.isLocationSpeedExceeded()) {
                // Rechtstreekse mobile aanpassing (geen roaming)
                changeMobileSetting(analyseData.getMobileNetworkGeneration(), false, false, optimizerPrefs);
            } else if (!analyseData.isWifi() && analyseData.isRoaming() && !analyseData.isLocationSpeedExceeded()) {
                // Rechtstreekse mobile aanpassing (wel roaming)
                changeMobileSetting(analyseData.getMobileNetworkGeneration(), true, false, optimizerPrefs);
            } else if (!analyseData.isWifi() && analyseData.isLocationSpeedExceeded()) {
                // Aanpassing aan de locatie-instelling
                changeLocationSetting(true, optimizerPrefs);
            }
        }
//        else if (action.getAction().equals("increaseNumberOfBufferings")) {
//            SharedPreferences.Editor editor = optimizerPrefs.edit();
//            int numberOfRebufferings = Integer.parseInt(optimizerPrefs.getAll().get(VideoOptimizer.REBUFFERINGS).toString());
//            numberOfRebufferings++;
//            editor.putString(VideoOptimizer.REBUFFERINGS, Integer.toString(numberOfRebufferings));
//            editor.commit();
//        } else if (action.getAction().equals("decreaseNumberOfBufferings")) {
//            SharedPreferences.Editor editor = optimizerPrefs.edit();
//            int numberOfRebufferings = Integer.parseInt(optimizerPrefs.getAll().get(VideoOptimizer.REBUFFERINGS).toString());
//            numberOfRebufferings--;
//            editor.putString(VideoOptimizer.REBUFFERINGS, Integer.toString(numberOfRebufferings));
//            editor.commit();
//        }
    }

    private void changeMobileSetting(NetworkGeneration networkGeneration, boolean isRoaming, boolean increase, SharedPreferences optimizerPrefs) {
        String preferenceKey = "";
        if (isRoaming) {
            preferenceKey = "roaming_";
        }
        if (networkGeneration.number == NetworkGeneration.G2_5.number) {
            preferenceKey += "quality_2.5g";
            changeMobileSetting(preferenceKey, increase, optimizerPrefs);
        } else if (networkGeneration.number == NetworkGeneration.G2_75.number) {
            preferenceKey += "quality_2.75g";
            changeMobileSetting(preferenceKey, increase, optimizerPrefs);
        } else if (networkGeneration.number == NetworkGeneration.G3.number) {
            preferenceKey += "quality_3g";
            changeMobileSetting(preferenceKey, increase, optimizerPrefs);
        } else if (networkGeneration.number == NetworkGeneration.G3_5.number) {
            preferenceKey += "quality_3.5g";
            changeMobileSetting(preferenceKey, increase, optimizerPrefs);
        } else if (networkGeneration.number == NetworkGeneration.G_4.number) {
            preferenceKey += "quality_4g";
            changeMobileSetting(preferenceKey, increase, optimizerPrefs);
        }
    }

    private void changeMobileSetting(String preferenceKey, boolean increase, SharedPreferences optimizerPrefs) {
        int intValue = Integer.parseInt(optimizerPrefs.getAll().get(preferenceKey).toString());
        SharedPreferences.Editor editor = optimizerPrefs.edit();
        if (increase) {
            intValue++;
            if (intValue <= 6) {
                editor.putString(preferenceKey, Integer.toString(intValue));
            }
        } else {
            intValue--;
            if (intValue > 0) {
                editor.putString(preferenceKey, Integer.toString(intValue));
            }
        }
        editor.commit();
    }

    private void changeLocationSetting(boolean increase, SharedPreferences optimizerPrefs) {
        int qualityAdaption = Integer.parseInt(optimizerPrefs.getAll().get(VideoOptimizer.QUALITY_ADAPTION).toString());
        SharedPreferences.Editor editor = optimizerPrefs.edit();
        if (increase) {
            qualityAdaption++;
            if (qualityAdaption < 6) {
                editor.putString(VideoOptimizer.QUALITY_ADAPTION, Integer.toString(qualityAdaption));
            }
        } else {
            qualityAdaption--;
            if (qualityAdaption >= 0) {
                editor.putString(VideoOptimizer.QUALITY_ADAPTION, Integer.toString(qualityAdaption));
            }
        }
        editor.commit();
    }

}
