package be.ugent.iii.questions;

import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * 
 * @author Laurenz Ovaere
 */
public abstract class Question {

	@Attribute(name = "id")
	private int id;

	@Element(name = "description")
	private String description;

	@ElementList(entry = "dependency", inline = true, required = false)
	private ArrayList<Dependency> dependencies = new ArrayList<Dependency>();

	@ElementList(entry = "action", inline = true, required = false)
	private ArrayList<Action> actions = new ArrayList<Action>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<Dependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(ArrayList<Dependency> dependencies) {
		this.dependencies = dependencies;
	}

	public void addDependency(Dependency dependency) {
		dependencies.add(dependency);
	}

	public ArrayList<Action> getActions() {
		return actions;
	}

	public void setActions(ArrayList<Action> actions) {
		this.actions = actions;
	}

	public void addAction(Action action) {
		actions.add(action);
	}

	public abstract boolean betweenBorders(double lowerlimit, double upperlimit);
}
