package be.ugent.iii.questions;

import be.ugent.iii.optimizer.AnalyseData;
import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;


/**
 * 
 * @author Laurenz Ovaere
 */
@Root
public class QuestionList {

	@ElementList(inline = true)
	private ArrayList<Question> questions = new ArrayList<Question>();
	@Attribute(name = "upperlimit")
	private int upperLimit;
	@Attribute(name = "lowerlimit")
	private int lowerLimit;
	private AnalyseData analyseData;

	public ArrayList<Question> getQuestions() {
		return questions;
	}

	public void setQuestions(ArrayList<Question> questions) {
		this.questions = questions;
	}
        
        public void clearList(){
            questions.clear();
        }

	public void addQuestion(Question question) {
		questions.add(question);
	}

	public int getUpperLimit() {
		return upperLimit;
	}

	public void setUpperLimit(int upperLimit) {
		this.upperLimit = upperLimit;
	}

	public int getLowerLimit() {
		return lowerLimit;
	}

	public void setLowerLimit(int lowerLimit) {
		this.lowerLimit = lowerLimit;
	}

	public AnalyseData getAnalyseData() {
		return analyseData;
	}

	public void setAnalyseData(AnalyseData analyseData) {
		this.analyseData = analyseData;
	}

}
