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
	 * ��¼group������������Ҷ�ӽڵ��¼����ÿ��group��Ӧ�ĸ���
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
		// ֹͣ�������Ӽ�������Ԫ������ͬһ�ࡣleaf node
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
/*			
			//debug
			double max = decisionGroup[0];
			int maxindex = 0;
			for (int i = 1; i < groupNum; i++) {
				if (decisionGroup[i] > max){
					max = decisionGroup[i];
					maxindex = i;
				}
			}
			for (int i = 0; i < groupNum; i++)
				decisionGroup[i] = 0;
			decisionGroup[maxindex] = 1;
*/
			return;
		}		
		//ǰ��֦���������ĸ߶ȣ����͸��Ӷ�Experiment.pruneByHight && ((level > (Math.log(Experiment.instanceNum)/Math.log(2))) || (level > Experiment.attriNum1))
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
		//���֦:����ýڵ��Ԫ����С��һ��������ֹͣ����
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
/*		
		//debug
		double max = decisionGroup[0];
		int maxindex = 0;
		for (int i = 1; i < groupNum; i++) {
			if (decisionGroup[i] > max){
				max = decisionGroup[i];
				maxindex = i;
			}
		}
		for (int i = 0; i < groupNum; i++)
			decisionGroup[i] = 0;
		decisionGroup[maxindex] = 1;
*/
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
//				double leftWeight = (conDivideValue - ins.getAttr(attrID).getDvalue()[0])/ (ins.getAttr(attrID).getDvalue()[1] - ins.getAttr(attrID).getDvalue()[0]);
				//debug
				double z = conDivideValue;
				double mu = (ins.getAttr(attrID).getDvalue()[0] + ins.getAttr(attrID).getDvalue()[1])/2;
				double sigma = (ins.getAttr(attrID).getDvalue()[1] - ins.getAttr(attrID).getDvalue()[0])/6;
				double leftWeight = Phi(z, mu, sigma);
				
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
//			double leftWeight = (conDivideValue - ins.getAttr(attrID).getDvalue()[0])/ (ins.getAttr(attrID).getDvalue()[1] - ins.getAttr(attrID).getDvalue()[0]);
			//debug
			double z = conDivideValue;
			double mu = (ins.getAttr(attrID).getDvalue()[0] + ins.getAttr(attrID).getDvalue()[1])/2;
			double sigma = (ins.getAttr(attrID).getDvalue()[1] - ins.getAttr(attrID).getDvalue()[0])/6;
			double leftWeight = Phi(z, mu, sigma);
			
			
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
//				double radio = (divideValue - ins.getAttr(id).getDvalue()[0])
//						/ (ins.getAttr(id).getDvalue()[1] - ins.getAttr(id)
//								.getDvalue()[0]);
				//debug
				double z = divideValue;
				double mu = (ins.getAttr(id).getDvalue()[0] + ins.getAttr(id).getDvalue()[1])/2;
				double sigma = (ins.getAttr(id).getDvalue()[1] - ins.getAttr(id).getDvalue()[0])/6;
				double radio = Phi(z, mu, sigma);
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
	 * ���֦�����ü�֦������
	 * @param root1
	 * �������ĸ��ڵ�
	 * @param ins 
	 * ��֦���Լ�
	 */
	public void cartPruning(DtreeNode root1,ArrayList<Instance> ins){
		if(!leafNode){
			//���ӽڵ��֦
			this.getChild(0).cartPruning(root1,ins);
			this.getChild(1).cartPruning(root1,ins);
			
			//������Ӧ��e(s)&backuperror
			//expectederror:������ýڵ���ΪҶ�ӽڵ�õ�����ȷ��
			this.leafNode = true;
			expectedError = calcTest(root1,ins);
			//backuperror:�ýڵ㲻��֦����ȷ��
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
	
	/*
	 * Guassian for debug
	 */
	public double phi(double x) {
        return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
    }

    // return phi(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
    public double phi(double x, double mu, double sigma) {
        return phi((x - mu) / sigma) / sigma;
    }

	// return Phi(z) = standard Gaussian cdf using Taylor approximation
    public double Phi(double z) {
        if (z < -8.0) return 0.0;
        if (z >  8.0) return 1.0;
        double sum = 0.0, term = z;
        for (int i = 3; sum + term != sum; i += 2) {
            sum  = sum + term;
            term = term * z * z / i;
        }
        return 0.5 + sum * phi(z);
    }

    // return Phi(z, mu, sigma) = Gaussian cdf with mean mu and stddev sigma
    public double Phi(double z, double mu, double sigma) {
        return Phi((z - mu) / sigma);
    } 

    // Compute z such that Phi(z) = y via bisection search
    public double PhiInverse(double y) {
        return PhiInverse(y, .00000001, -8, 8);
    } 

    // bisection search
    private double PhiInverse(double y, double delta, double lo, double hi) {
        double mid = lo + (hi - lo) / 2;
        if (hi - lo < delta) return mid;
        if (Phi(mid) > y) return PhiInverse(y, delta, lo, mid);
        else              return PhiInverse(y, delta, mid, hi);
    }
/*    
  //Guassian debug [0, 6], mu = 3, sigma = 1
	double z     = 4;
    double mu    = 3;
    double sigma = 1;
    System.out.println(Phi(z, mu, sigma));
    double y = Phi(z);
    System.out.println(PhiInverse(y));
    //-------------------------------------------
     * 
     */
}


