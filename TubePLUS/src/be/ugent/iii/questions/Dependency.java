package be.ugent.iii.questions;

import org.simpleframework.xml.Attribute;

/**
 * 
 * @author Laurenz Ovaere
 */
public class Dependency {

	@Attribute(name = "questionnumber")
	private int questionNumber;

	@Attribute(name = "lowerlimit")
	private int lowerlimit;

	@Attribute(name = "upperlimit")
	private int uppderlimit;

	public int getQuestionNumber() {
		return questionNumber;
	}

	public void setQuestionNumber(int questionNumber) {
		this.questionNumber = questionNumber;
	}

	public int getLowerlimit() {
		return lowerlimit;
	}

	public void setLowerlimit(int lowerlimit) {
		this.lowerlimit = lowerlimit;
	}

	public int getUppderlimit() {
		return uppderlimit;
	}

	public void setUppderlimit(int uppderlimit) {
		this.uppderlimit = uppderlimit;
	}
}
