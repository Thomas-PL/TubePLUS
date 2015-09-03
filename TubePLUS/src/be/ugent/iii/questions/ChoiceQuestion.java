package be.ugent.iii.questions;

import java.util.TreeMap;

import org.simpleframework.xml.ElementMap;

/**
 * 
 * @author Laurenz Ovaere
 */
public class ChoiceQuestion extends Question {

	@ElementMap(entry = "choice", key = "key", attribute = true, inline = true)
	private TreeMap<Integer, String> choices = new TreeMap<Integer, String>();

	// De gekozen waarde (als antwoord) wordt bijgehouden.
	// Wanneer er nog geen antwoord werd geselecteerd
	// is de waarde -1!
	private int selectedChoice = -1;

	public TreeMap<Integer, String> getChoices() {
		return choices;
	}

	public void setChoices(TreeMap<Integer, String> choices) {
		this.choices = choices;
	}

	public void addChoice(int key, String choice) {
		choices.put(key, choice);
	}

	public void setSelectedChoice(int selection) {
		selectedChoice = selection;
	}

	public void clearSelectedChoice() {
		selectedChoice = -1;
	}

	public int getSelectedChoice() {
		return selectedChoice;
	}

	public String getSelectedChoiceDescription() {
		return choices.get(selectedChoice);
	}

	// Controleert de choice-waarde met de opgegeven grenzen.
	// De grenzen zelf inbegrepen!
	public boolean betweenBorders(double lowerlimit, double upperlimit) {
		if (selectedChoice >= lowerlimit && selectedChoice <= upperlimit)
			return true;
		else
			return false;
	}
}
