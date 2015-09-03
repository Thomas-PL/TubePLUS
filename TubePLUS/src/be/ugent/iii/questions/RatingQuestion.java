package be.ugent.iii.questions;

/**
 * 
 * @author Laurenz Ovaere
 */
public class RatingQuestion extends Question {

	// De huidige rating wordt bijgehouden.
	// Initieel is de ratingbar leeg
	// en is de waarde dus 0!
	private double rating = 0;

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	// Controleert de rating-waarde met de opgegeven grenzen.
	// De grenzen zelf inbegrepen!
	public boolean betweenBorders(double lowerlimit, double upperlimit) {
		if (rating >= lowerlimit && rating <= upperlimit)
			return true;
		else
			return false;
	}
}
