
public class Instance {
//	private int attrnum;
	private Attribute[] attrs;
	
	/**
	 * used to define the group of an instance: 1~7
	 */
	private int group;
	
	private double weight = 1;
	
	public Instance(Attribute[] attrs, int group) {
		this.attrs = attrs;
		this.group = group;
	}
	
	public Instance(Instance ins, double weight) {
		this.attrs = ins.attrs;
		this.group = ins.group;
		this.weight = weight;
	}
	
	public Attribute getAttr(int id) {
		return this.attrs[id];
	}
	
	public int getGroup() {
		return group;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
		return;
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public String toString() {
		String outputStr = "";
		for (int i = 0; i < attrs.length; i++)
			outputStr += attrs[i].toString() + ",";
			outputStr += group;
		return outputStr;
	}
}
