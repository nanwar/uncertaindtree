
public class Attribute {
	
	private int function = 1;
	/**
	 * used when type is continuous;
	 */
	private double[] dvalue = new double[2];
	
	private boolean known;
	
	public Attribute(String strvalue1, String strvalue2) {
		if (strvalue1.equals("?") || strvalue2.equals("?"))
			known = false;
		else
			known = true;
		
		if (known == true) {
			this.dvalue[0] = Double.parseDouble(strvalue1);
			this.dvalue[1] = Double.parseDouble(strvalue2);
		}
	}
	
	public double[] getDvalue() {
		return dvalue;
	}
	
	public boolean isKnown() {
		return known;
	}
	
	public String toString() {
		return ""+dvalue[0]+" "+dvalue[1];
	}
}
