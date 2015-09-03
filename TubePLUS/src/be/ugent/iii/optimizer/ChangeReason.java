package be.ugent.iii.optimizer;

/**
 * Klasse die bijhoudt waarom de kwaliteit van een video veranderd werd.
 * @author Thomas
 */
public enum ChangeReason {

	UNKNOWN(0), MOBILE(1), WIFI(2), LOCATION(3), CPU(4), THROUGHPUT(5);

	public int number;

	private ChangeReason(int number) {
		this.number = number;
	}

	@Override
	public String toString() {
		switch (number) {
		case 0:
			return "UNKNOWN";
		case 1:
			return "MOBILE";
		case 2:
			return "WIFI";
		case 3:
			return "LOCATION";
		case 4:
			return "CPU";
                case 5:
                        return "THROUGHPUT";
		default:
			return super.toString();
		}
	}
}
