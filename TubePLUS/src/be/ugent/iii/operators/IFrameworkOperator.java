package be.ugent.iii.operators;

import be.ugent.iii.observer.IObservable;


/**
 * Algemene frameworkoperator interface
 * @author Laurenz Ovaere
 */
public interface IFrameworkOperator extends IObservable{

	public void enableOperator();

	public void disableOperator();
}
