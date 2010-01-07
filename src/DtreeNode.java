import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class DtreeNode {
	/*
	 * the Dtree node choose which attribute
	 */
	private int attrID = -1;

	public int getAt() {
		return attrID;
	}

	/**
	 * if the chosen attribute is continuous, use which value to divide all the
	 * values into two parts
	 */
	private double conDivideValue;

	private boolean leafNode = false;

	public boolean isLeafNode() {
		return leafNode;
	}

	public void setLeafNode(boolean leafNode) {
		this.leafNode = leafNode;
	}

	private int level = -1;
	
	private int treeSize = -1;
	
	public int getTreeSize() {
		if (this.leafNode)
			treeSize = 1;
		else {
			treeSize = 1;
			for (int i = 0; i < childNode.size(); i++) {
				treeSize +=childNode.get(i).getTreeSize();
				//System.out.println("+ " + childNode.get(i).getTreeSize() + " = " + treeSize);
			}
		}
		return treeSize;
	}
	/*
	public int getsize() {
		return treeSize;
	}
	*/

	/**
	 * 记录group的数量，对于叶子节点记录落在每个group对应的概率
	 */
	private int groupNum = 0;
	private double[] decisionGroup;

	public double[] getDecisionGroup() {
		return decisionGroup;
	}

	/**
	 * record the child branch node
	 */
	private ArrayList<DtreeNode> childNode = new ArrayList<DtreeNode>();
	private DtreeNode parentNode = null;

	public void addChild(DtreeNode child) {
		childNode.add(child);
		child.parentNode = this;
	}

	public DtreeNode getChild(int id) {
		return childNode.get(id);
	}

	public DtreeNode getParent() {
		return parentNode;
	}

	public DtreeNode(int gn, int lev) {
		this.groupNum = gn;
		this.level = lev;
	}

	public void learn(ArrayList<Instance> trainList, boolean[] attrUsed) {
		// The learn could be stop if all the instances are "+" or "-"
		// or all the attributes have been used

		// first decide all come to a same group
//		System.out.println("level = " + this.level);
		
		boolean samegroup = true;
		// TODO:list may be null
		for (Instance ins : trainList) {
			if (ins.getGroup() != trainList.get(0).getGroup()) {
				samegroup = false;
				break;
			}
		}
		// System.out.println("\ntrainList.size:"+trainList.size());
		// leaf node
		if (samegroup) {
//			System.out.println("all of the same group");
			leafNode = true;
			decisionGroup = new double[groupNum];
			for (int i = 0; i < groupNum; i++)
				decisionGroup[i] = 0;
			decisionGroup[trainList.get(0).getGroup()] = 1;
			return;
		}
		// decide whether there is attribute to classify
		boolean attrAvail = false;

		for (int i = 0; i < attrUsed.length; i++)
			if (!attrUsed[i]) {
				attrAvail = true;
				break;
			}
		// no more attribute to classify although the set is not pure
		if (!attrAvail) {
//			System.out.println("no attravial");
			leafNode = true;
			decisionGroup = new double[groupNum];
			for (int i = 0; i < groupNum; i++)
				decisionGroup[i] = 0;
			double allweight = 0;
			for (Instance ins : trainList) {
				allweight += ins.getWeight();
				decisionGroup[ins.getGroup()] += ins.getWeight();
			}
			for (int i = 0; i < groupNum; i++)
				decisionGroup[i] /= allweight;
			return;
		}		
//		//前剪枝：限制树的高度，降低复杂度
//		if (level >= 20) {
//			leafNode = true;
//			decisionGroup = new double[groupNum];
//			for (int i = 0; i < groupNum; i++)
//				decisionGroup[i] = 0;
//			double allweight = 0;
//			for (Instance ins : trainList) {
//				allweight += ins.getWeight();
//				decisionGroup[ins.getGroup()] += ins.getWeight();
//			}
//			for (int i = 0; i < groupNum; i++)
//				decisionGroup[i] /= allweight;
//			return;
//		}
		//后剪枝:如果该节点的元组数小于一定数量就停止分裂
		if(trainList.size() <= ((int)(0.05*((double)Experiment.instanceNum)))){
//			System.out.println(trainList.size());
			leafNode = true;
			decisionGroup = new double[groupNum];
			for (int i = 0; i < groupNum; i++)
				decisionGroup[i] = 0;
			double allweight = 0;
			for (Instance ins : trainList) {
				allweight += ins.getWeight();
				decisionGroup[ins.getGroup()] += ins.getWeight();
			}
			for (int i = 0; i < groupNum; i++)
				decisionGroup[i] /= allweight;
			return;
		}
		// ------------------------------------------------------------------
		// means we have to use Entropy to decide choose which attribute
		// TODO: taoist:choose one attribute and a split point

		//System.out.println("level = " + this.level);
		//add----------------
		decisionGroup = new double[groupNum];
		for (int i = 0; i < groupNum; i++)
			decisionGroup[i] = 0;
		double allweight = 0;
		for (Instance ins : trainList) {
			allweight += ins.getWeight();
			decisionGroup[ins.getGroup()] += ins.getWeight();
		}
		for (int i = 0; i < groupNum; i++)
			decisionGroup[i] /= allweight;
		//---------------------

		double minEntropy = 0;
		int bestAttrId = -1;
		boolean isInitialized = false;
		for (int id = 0; id < attrUsed.length; id++) {
			if (attrUsed[id])
				continue;
			// TODO:finish continuousEntropy function
			DouValue<Double, Double> ret = continuousEntropy(trainList, id);
			double entropy = ret.first;
			// System.out.println("entropy:"+entropy);
			double divideValue = ret.second;
			if (!isInitialized) {
				minEntropy = entropy;
				bestAttrId = id;
				conDivideValue = divideValue;
				isInitialized = true;
				// System.out.println("id:"+id);
			}
			if (entropy < minEntropy) {
				minEntropy = entropy;
				bestAttrId = id;
				conDivideValue = divideValue;
				// System.out.println("id:"+id);
			}
		}

		attrID = bestAttrId;
		// System.out.println("attrID:"+attrID);

		boolean[] newAttrUsed = new boolean[attrUsed.length];
		// System.out.println("new attr length:"+newAttrUsed.length);
		for (int i = 0; i < attrUsed.length; i++)
			newAttrUsed[i] = attrUsed[i];
		newAttrUsed[attrID] = true;

		// TODO:taoist:split the set to L and R
		ArrayList<Instance> leftList = new ArrayList<Instance>();
		ArrayList<Instance> rightList = new ArrayList<Instance>();
		for (Instance ins : trainList) {
			if (ins.getAttr(attrID).getDvalue()[1] <= conDivideValue) {
				leftList.add(ins);
			} else if (ins.getAttr(attrID).getDvalue()[0] >= conDivideValue) {
				rightList.add(ins);
			} else {
				double leftWeight = (conDivideValue - ins.getAttr(attrID)
						.getDvalue()[0])
						/ (ins.getAttr(attrID).getDvalue()[1] - ins.getAttr(
								attrID).getDvalue()[0]);
				Instance leftChildInstance = new Instance(ins, leftWeight
						* ins.getWeight());
				Instance rightChildInstance = new Instance(ins, ins.getWeight()
						- leftWeight * ins.getWeight());
				leftList.add(leftChildInstance);
				rightList.add(rightChildInstance);
			}
		}
		// System.out.println("lsize:"+leftList.size()+"rsize:"+rightList.size());

		DtreeNode leftChild = new DtreeNode(this.groupNum, this.level + 1);
		leftChild.learn(leftList, newAttrUsed);
		this.addChild(leftChild);

		DtreeNode rightChild = new DtreeNode(this.groupNum, this.level + 1);
		rightChild.learn(rightList, newAttrUsed);
		this.addChild(rightChild);
	}

	// JIE-test
	public double[] test(Instance ins) {
		DtreeNode testNode = this;

		double[] result = new double[this.groupNum];
		if (testNode.leafNode) {
			for (int i = 0; i < this.groupNum; i++)
				result[i] = testNode.decisionGroup[i] * ins.getWeight();
			return result;
		}

		if (ins.getAttr(testNode.attrID).getDvalue()[0] > testNode.conDivideValue) {
			return testNode.getChild(1).test(ins);
		} else if (ins.getAttr(testNode.attrID).getDvalue()[1] <= testNode.conDivideValue) {
			return testNode.getChild(0).test(ins);
		} else {
			double orgWeight = ins.getWeight();
			double range = ins.getAttr(testNode.attrID).getDvalue()[1]
					- ins.getAttr(testNode.attrID).getDvalue()[0];
			double leftWeight = orgWeight
					* (testNode.conDivideValue - ins.getAttr(testNode.attrID)
							.getDvalue()[0]) / range;
			double rightWeight = orgWeight
					* (ins.getAttr(testNode.attrID).getDvalue()[1] - testNode.conDivideValue)
					/ range;
			// ins.setWeight(leftWeight);
			Instance ins1 = new Instance(ins, leftWeight);
			double[] result0 = testNode.getChild(0).test(ins1);
			// ins.setWeight(rightWeight);
			Instance ins2 = new Instance(ins, rightWeight);
			double[] result1 = testNode.getChild(1).test(ins2);
			for (int i = 0; i < this.groupNum; i++)
				result[i] = result0[i] + result1[i];
			return result;

		}
	}

	// TODO:taoist:
	public DouValue<Double, Double> continuousEntropy(
			ArrayList<Instance> trainList, final int id) {
		double[] endPoints = new double[trainList.size() * 2];
		for (int i = 0; i < trainList.size(); i++) {
			endPoints[2 * i] = trainList.get(i).getAttr(id).getDvalue()[0];
			endPoints[2 * i + 1] = trainList.get(i).getAttr(id).getDvalue()[1];
		}
		Arrays.sort(endPoints);
		ArrayList<Double> tempList = new ArrayList();
		tempList.add(endPoints[0]);
		// System.out.print(endPoints[0]);
		for (int i = 1; i < endPoints.length; i++) {
			if (endPoints[i] > tempList.get(tempList.size() - 1)) {
				tempList.add(endPoints[i]);
				// System.out.print(endPoints[i]+" ");
			}
		}
		// System.out.println("");
		if (tempList.size() == 2) {
			return new DouValue<Double, Double>(calcContinuousEntropy(
					trainList, id, (tempList.get(0) + tempList.get(tempList
							.size() - 1)) / 2), (tempList.get(0) + tempList
					.get(tempList.size() - 1)) / 2);
		}
		double minEntropy = 0;
		double bestDivideValue = 0;
		boolean isInitialized = false;
		// System.out.println("tempListsize:"+tempList.size());
		for (int i = 1; i < tempList.size() - 1; i = i + 5) {

			double calcEntropy = calcContinuousEntropy(trainList, id, tempList
					.get(i));
			// System.out.println("calcEntropy:"+calcEntropy);
			if (!isInitialized) {
				minEntropy = calcEntropy;
				bestDivideValue = tempList.get(i);
				isInitialized = true;
			}
			if (calcEntropy < minEntropy) {
				minEntropy = calcEntropy;
				bestDivideValue = tempList.get(i);
			}

		}
		return new DouValue<Double, Double>(minEntropy, bestDivideValue);

	}

	// TODO:taoist
	public double calcContinuousEntropy(ArrayList<Instance> knownList, int id,
			double divideValue) {
		double[][] weightMatrix = new double[2][groupNum];
		double[] totalWeight = new double[2];
		for (int i = 0; i < 2; i++) {
			totalWeight[i] = 0.0;
			for (int j = 0; j < groupNum; j++) {
				weightMatrix[i][j] = 0.0;
			}
		}
		for (Instance ins : knownList) {
			if (ins.getAttr(id).getDvalue()[1] <= divideValue) {
				weightMatrix[0][ins.getGroup()] += ins.getWeight();
				totalWeight[0] += ins.getWeight();
			} else if (ins.getAttr(id).getDvalue()[0] >= divideValue) {
				weightMatrix[1][ins.getGroup()] += ins.getWeight();
				totalWeight[1] += ins.getWeight();
			} else {
				double radio = (divideValue - ins.getAttr(id).getDvalue()[0])
						/ (ins.getAttr(id).getDvalue()[1] - ins.getAttr(id)
								.getDvalue()[0]);
				weightMatrix[0][ins.getGroup()] += ins.getWeight() * radio;
				totalWeight[0] += weightMatrix[0][ins.getGroup()];
				weightMatrix[1][ins.getGroup()] += ins.getWeight()
						* (1.0 - radio);
				totalWeight[1] += ins.getWeight() * (1.0 - radio);
			}
		}
		double info = 0.0;
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < groupNum; j++) {
				if (weightMatrix[i][j] > 0)
					info += -weightMatrix[i][j]
							* Math.log(weightMatrix[i][j] / totalWeight[i])/(totalWeight[0]+totalWeight[1]);
//				if (weightMatrix[i][j] / totalWeight[i] > 1) {
//					System.out.println("god: " + weightMatrix[i][j] + " "
//							+ totalWeight[i]);
//					for (int j2 = 0; j2 < totalWeight.length; j2++) {
//						System.out.print(weightMatrix[i][j] + " ");
//					}
//					System.out.println("");
//				}
			}
		}
		return info;
	}
	
	
}
