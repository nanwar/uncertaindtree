
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
	
	/**
	 * @param ins
	 * @param id
	 * 要修改的属性的序号
	 * @param leftBound
	 * 分裂后的instance的左边界
	 * @param rightBound
	 * 分裂后的instance的右边界
	 * @param weight
	 */
	public Instance(Instance ins, int id, double leftBound, double rightBound, double weight){
//		System.out.println("2:" + leftBound + " " + rightBound);
//		this.attrs = ins.attrs;
		this.attrs = new Attribute[Experiment.attriNum1];
		for (int i = 0; i < Experiment.attriNum1; i++) {
			this.attrs[i] = new Attribute();
			this.attrs[i].setDvalue(ins.attrs[i].getDvalue()[0], ins.attrs[i].getDvalue()[1]);
		}
		this.group = ins.group;
		this.weight = weight;
		this.attrs[id].setDvalue(leftBound,rightBound);
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
