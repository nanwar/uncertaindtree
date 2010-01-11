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

	private double expectedError = 0.0;
	private double backedUpError = 0.0;
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
	
	public int getTreeSize(){
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

//		System.out.println("level = " + this.level);
		
		boolean samegroup = true;
		// TODO:list may be null
		for (Instance ins : trainList) {
			if (ins.getGroup() != trainList.get(0).getGroup()) {
				samegroup = false;
				break;
			}
		}
		// 停止条件：子集中所有元素属于同一类。leaf node
		if (samegroup) {
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
		//前剪枝：限制树的高度，降低复杂度Experiment.pruneByHight && ((level > (Math.log(Experiment.instanceNum)/Math.log(2))) || (level > Experiment.attriNum1))
		if ( Experiment.pruneByHight && (level >= Experiment.attriNum1 || level >(Math.log(Experiment.instanceNum)/Math.log(2)))){
//			System.out.println("level:"+level+";attriNum:"+Experiment.attriNum1+";log:"+(Math.log(Experiment.instanceNum)/Math.log(2)));
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
		//后剪枝:如果该节点的元组数小于一定数量就停止分裂
		if(Experiment.pruneByNodeNum && (trainList.size() <= ((int)(Experiment.canNodeNum*((double)Experiment.instanceNum))))){
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
		// means we have to use Entropy to decide choose which attribute and a split point
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
			DouValue<Double, Double> ret = continuousEntropy(trainList, id);
			double entropy = ret.first;
			double divideValue = ret.second;
			if (!isInitialized) {
				minEntropy = entropy;
				bestAttrId = id;
				conDivideValue = divideValue;
				isInitialized = true;
			}
			if (entropy < minEntropy) {
				minEntropy = entropy;
				bestAttrId = id;
				conDivideValue = divideValue;
			}
		}

		attrID = bestAttrId;
		boolean[] newAttrUsed = new boolean[attrUsed.length];
		for (int i = 0; i < attrUsed.length; i++)
			newAttrUsed[i] = attrUsed[i];
		newAttrUsed[attrID] = true;

		// split the set to L and R
		ArrayList<Instance> leftList = new ArrayList<Instance>();
		ArrayList<Instance> rightList = new ArrayList<Instance>();
//		System.out.println("conDivedeValue: " + conDivideValue);
		for (Instance ins : trainList) {
//			System.out.println(ins.getAttr(attrID).getDvalue()[0] + " " + ins.getAttr(attrID).getDvalue()[1]);
			if (ins.getAttr(attrID).getDvalue()[1] <= conDivideValue) {
				leftList.add(ins);
			} else if (ins.getAttr(attrID).getDvalue()[0] >= conDivideValue) {
				rightList.add(ins);
			} else {
				double leftWeight = (conDivideValue - ins.getAttr(attrID).getDvalue()[0])/ (ins.getAttr(attrID).getDvalue()[1] - ins.getAttr(attrID).getDvalue()[0]);
//				System.out.println("******* " + ins.getAttr(attrID).getDvalue()[0] + " " +conDivideValue + " " +ins.getAttr(attrID).getDvalue()[1]);
//				Instance leftChildInstance = new Instance(ins, attrID, ins.getAttr(attrID).getDvalue()[0], conDivideValue, leftWeight* ins.getWeight());
				Instance leftChildInstance = new Instance(ins, leftWeight* ins.getWeight());
//				System.out.println("left : " + leftChildInstance.getAttr(attrID).getDvalue()[0] + " " + leftChildInstance.getAttr(attrID).getDvalue()[1]);
//				System.out.println("now: " + ins.getAttr(attrID).getDvalue()[0]+ " " + ins.getAttr(attrID).getDvalue()[1]);
//				Instance rightChildInstance = new Instance(ins, attrID, conDivideValue, ins.getAttr(attrID).getDvalue()[1], ins.getWeight()- leftWeight * ins.getWeight());
				Instance rightChildInstance = new Instance(ins,ins.getWeight()- leftWeight * ins.getWeight());
//				System.out.println("right : " + rightChildInstance.getAttr(attrID).getDvalue()[0] + " " + rightChildInstance.getAttr(attrID).getDvalue()[1]);
				leftList.add(leftChildInstance);
				rightList.add(rightChildInstance);
			}
		}
//		System.out.println("lsize: " + leftList.size());
//		System.out.println("rsize: " + rightList.size());
		DtreeNode leftChild = new DtreeNode(this.groupNum, this.level + 1);
		leftChild.learn(leftList, newAttrUsed);
		this.addChild(leftChild);
//		System.out.println("left son ***********************");
//		for (int i = 0; i < leftList.size(); i++) {
//			System.out.println(leftList.get(i).getAttr(attrID).getDvalue()[0] + " " + leftList.get(i).getAttr(attrID).getDvalue()[1]);
//		}

		DtreeNode rightChild = new DtreeNode(this.groupNum, this.level + 1);
		rightChild.learn(rightList, newAttrUsed);
		this.addChild(rightChild);
//		System.out.println("right son ***********************");
//		for (int i = 0; i < rightList.size(); i++) {
//			System.out.println(rightList.get(i).getAttr(attrID).getDvalue()[0] + " " + rightList.get(i).getAttr(attrID).getDvalue()[1]);
//		}
	}


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
			double leftWeight = (conDivideValue - ins.getAttr(attrID).getDvalue()[0])/ (ins.getAttr(attrID).getDvalue()[1] - ins.getAttr(attrID).getDvalue()[0]);
//			Instance leftChildInstance = new Instance(ins, attrID, ins.getAttr(attrID).getDvalue()[0], testNode.conDivideValue, leftWeight* ins.getWeight());
			Instance leftChildInstance = new Instance(ins, leftWeight* ins.getWeight());
//			Instance rightChildInstance = new Instance(ins, attrID, conDivideValue, ins.getAttr(attrID).getDvalue()[1], ins.getWeight()- leftWeight * ins.getWeight());
			Instance rightChildInstance = new Instance(ins, ins.getWeight()- leftWeight * ins.getWeight());
			
			//Instance ins1 = new Instance(ins, leftWeight);
			double[] result0 = testNode.getChild(0).test(leftChildInstance);
			//Instance ins2 = new Instance(ins, rightWeight);
			double[] result1 = testNode.getChild(1).test(rightChildInstance);
			for (int i = 0; i < this.groupNum; i++)
				result[i] = result0[i] + result1[i];
			return result;

		}
	}

//	public double[] test(Instance ins) {
//		DtreeNode testNode = this;
//
//		double[] result = new double[this.groupNum];
//		if (testNode.leafNode) {
//			for (int i = 0; i < this.groupNum; i++)
//				result[i] = testNode.decisionGroup[i] * ins.getWeight();
//			return result;
//		}
//
//		if (ins.getAttr(testNode.attrID).getDvalue()[0] > testNode.conDivideValue) {
//			return testNode.getChild(1).test(ins);
//		} else if (ins.getAttr(testNode.attrID).getDvalue()[1] <= testNode.conDivideValue) {
//			return testNode.getChild(0).test(ins);
//		} else {
//			double orgWeight = ins.getWeight();
//			double range = ins.getAttr(testNode.attrID).getDvalue()[1]
//					- ins.getAttr(testNode.attrID).getDvalue()[0];
//			double leftWeight = orgWeight
//					* (testNode.conDivideValue - ins.getAttr(testNode.attrID)
//							.getDvalue()[0]) / range;
//			double rightWeight = orgWeight
//					* (ins.getAttr(testNode.attrID).getDvalue()[1] - testNode.conDivideValue)
//					/ range;
//			// ins.setWeight(leftWeight);
//			Instance ins1 = new Instance(ins, leftWeight);
//			double[] result0 = testNode.getChild(0).test(ins1);
//			// ins.setWeight(rightWeight);
//			Instance ins2 = new Instance(ins, rightWeight);
//			double[] result1 = testNode.getChild(1).test(ins2);
//			for (int i = 0; i < this.groupNum; i++)
//				result[i] = result0[i] + result1[i];
//			return result;
//
//		}
//	}

	
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
		for (int i = 1; i < endPoints.length; i++) {
			if (endPoints[i] > tempList.get(tempList.size() - 1)) {
				tempList.add(endPoints[i]);
			}
		}
		if (tempList.size() == 2) {
			return new DouValue<Double, Double>(calcContinuousEntropy(
					trainList, id, (tempList.get(0) + tempList.get(tempList
							.size() - 1)) / 2), (tempList.get(0) + tempList
					.get(tempList.size() - 1)) / 2);
		}
		double minEntropy = 0;
		double bestDivideValue = 0;
		boolean isInitialized = false;
		for (int i = 1; i < tempList.size() - 1; i = i + 1) {

			double calcEntropy = calcContinuousEntropy(trainList, id, tempList
					.get(i));
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
			}
		}
		return info;
	}

	/**
	 * 后剪枝，利用剪枝错误率
	 * @param root1
	 * 决策树的根节点
	 * @param ins 
	 * 剪枝测试集
	 */
	public void cartPruning(DtreeNode root1,ArrayList<Instance> ins){
		if(!leafNode){
			//对子节点剪枝
			this.getChild(0).cartPruning(root1,ins);
			this.getChild(1).cartPruning(root1,ins);
			
			//计算相应的e(s)&backuperror
			//expectederror:如果将该节点作为叶子节点得到的正确率
			this.leafNode = true;
			expectedError = calcTest(root1,ins);
			//backuperror:该节点不剪枝的正确率
			this.leafNode = false;
			backedUpError = calcTest(root1,ins);
//			System.out.println("ex: "+expectedError +" ;back: " + backedUpError);
			//decide whether to cut
			if(expectedError > backedUpError){
				this.leafNode = true;
//				System.out.println("yes");
			}
					
		}
	}
	//TODO:to modify
	public double calcTest(DtreeNode root1,ArrayList<Instance> ins){
		int sucCount = 0;
		for (int i = 0; i < ins.size(); i++) {
			double[] decisions = new double[this.groupNum];
			decisions = root1.test(ins.get(i));
			int treeDecision = 0;
			for (int k = 1; k < this.groupNum; k++)
				if (decisions[k] > decisions[treeDecision])
					treeDecision = k;
			if (ins.get(i).getGroup() == treeDecision)
				sucCount += 1;
		}
//		System.out.println("size:"+ins.size());
		return ((double)sucCount / (double)ins.size());
	}
}
