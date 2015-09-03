package be.ugent.iii.questions;

import org.simpleframework.xml.Attribute;

/**
 * 
 * @author Laurenz Ovaere
 */
public class Action {

	@Attribute(name = "action")
	private String action;

	@Attribute(name = "answer")
	private int answer;

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public int getAnswer() {
		return answer;
	}

	public void setAnswer(int answer) {
		this.answer = answer;
	}
}
