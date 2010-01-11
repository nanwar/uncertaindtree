import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.io.*;

public class Experiment {
	/**
	 * 建立的决策树根节点
	 */
	private DtreeNode root = null;
	/**
	 * 一次实验中数据读入对象
	 */
	private InstanceInput inputer = new InstanceInput();
	/**
	 * 一次实验中总元组的个数，包括训练集和测试集
	 */
	public static int instanceNum = 0;
	/**
	 * 一次实验中类别标志的个数
	 */
	public static int groupNum1 = 0;
	/**
	 * 一次实验中元组属性的个数
	 */
	public static int attriNum1 = 0;
	/**
	 *标志叶子节点中如果某个权重比例超过一定值的话就直接归并为上个节点 
	 */
	public static double canMerge = 0.9;
	/**
	 * 判断是否可以针对某个节点的分布集中某个类而剪枝：true可以剪枝
	 */
	public static boolean pruneByMerge = false;
	/**
	 * 是否进行对树高度限制的剪枝
	 */
	public static boolean pruneByHight = true;
	/**
	 * 是否进行针对某个节点数目过少而剪枝
	 */
	public static boolean pruneByNodeNum = false;
	/**
	 * 标志节点总是少于某个比例就就行剪枝的百分比
	 */
	public static double canNodeNum = 0.005;
	/**
	 * 是否针对cart算法对错误率而剪枝
	 */
	public static boolean pruneByError = false;
	/**
	 * 对一个实验执行的次数
	 */
	public static int times = 10;
	/**
	 * 结果输出文件
	 */
	public static String answerSource = "answer.txt";
	/**
	 * 训练集
	 */
	private ArrayList<Instance> trainlist;
	/**
	 * 测试集
	 */
	private ArrayList<Instance> testlist;
	/**
	 * 剪枝测试集
	 */
	private ArrayList<Instance> prunelist;
	
	/**
	 * 对所有元组进行分割得到3个子集:训练集，测试集以及剪枝测试集
	 * @param trainFilename
	 * 数据文件路径
	 */
	public void prepareList(String trainFilename) {
		int range = 80;
		int prunerange = 40;
		ArrayList<Instance> fullList = inputer.inputFile(trainFilename);
		instanceNum = fullList.size();
		groupNum1 = inputer.getGroupNum();
		attriNum1 = inputer.getAttriNum();
		trainlist = new ArrayList<Instance>();
		testlist = new ArrayList<Instance>();
		prunelist = new ArrayList<Instance>();
		root = new DtreeNode(inputer.getGroupNum(), 1);
		
		Random rand = new Random();
		rand.setSeed(new Date().getTime());

		/**
		 * listSize now is the total fullList
		 */
		int trainlistSize = fullList.size() * range / 100;
		int testlistSize = fullList.size() - trainlistSize;
		int prunelistSize = trainlistSize * prunerange / 100;
		trainlistSize = trainlistSize - prunelistSize;
		//int trainlistSize = fullList.size() * 8 / 100;
		//int testlistSize = fullList.size() * 2 / 100;

		while (trainlistSize > 0) {
			int picNumber = rand.nextInt(fullList.size());
			trainlist.add(fullList.get(picNumber));
			fullList.remove(picNumber);
			trainlistSize--;
		}
		
		while (prunelistSize > 0) {
			int picNumber = rand.nextInt(fullList.size());
			prunelist.add(fullList.get(picNumber));
			fullList.remove(picNumber);
			prunelistSize--;
		}
		
		while (testlistSize > 0) {
			int picNumber = rand.nextInt(fullList.size());
			testlist.add(fullList.get(picNumber));
			fullList.remove(picNumber);
			testlistSize--;
		}
		
	}
	
	/**
	 * 决策树的建立（包括高度剪枝，后剪枝:如果该节点的元组数小于一定数量就停止分裂）
	 * 
	 * 
	 * @return
	 * tree size
	 */
	public int train() {
		boolean[] attrUsed = new boolean[inputer.getAttriNum()];
		
		for (int i = 0; i < attrUsed.length; i++)
			attrUsed[i] = false;
		
		root.learn(trainlist,attrUsed);
		System.out.println("after builded(including hight pruning and num pruning), decision tree size :  " + root.getTreeSize());
		//后剪枝：根据节点分布情况
		if(pruneByMerge){
			sufPruning(root);
			System.out.println("after merged pruneing, decision tree size : " + root.getTreeSize());
		}
		
		double tempSucrate = this.test();
		System.out.println("Before cart Pruning, the sucrate = " + tempSucrate);
		if(pruneByError){
			root.cartPruning(root, this.prunelist);
			System.out.println("after cart pruning, decision tree size : " + root.getTreeSize());
		}
		return root.getTreeSize();
	}
	public void sufPruning(DtreeNode dNode){
		if(!dNode.isLeafNode()){
			if(canBeMerged(dNode.getDecisionGroup())){
				dNode.setLeafNode(true);
			}
			else{
				sufPruning(dNode.getChild(0));
				sufPruning(dNode.getChild(1));
			}
		}
		
	}
	public boolean canBeMerged(double[] weight){
		double total = 0.0;
		double max = 0.0;
		for (int i = 0; i < weight.length; i++) {
			total += weight[i];
			if(weight[i] > max)
				max = weight[i];
		}
		if(( max / total ) >= canMerge)
			return true;
		else
			return false;
	}
	
	public double test() {
		//denote the "+" error and the "-" error
		int sucCount = 0;
		
		for (int i = 0; i < testlist.size(); i++) {
			double[] decisions = new double[inputer.getGroupNum()];
			decisions = root.test(testlist.get(i));
			int treeDecision = 0;
			for (int k = 1; k < inputer.getGroupNum(); k++)
				if (decisions[k] > decisions[treeDecision])
					treeDecision = k;
			if (testlist.get(i).getGroup() == treeDecision)
				sucCount += 1;
		}
		
		/*
		for (int i = 0; i < trainlist.size(); i++) {
			double[] decisions = new double[inputer.getGroupNum()];
			decisions = root.test(trainlist.get(i));
			int treeDecision = 0;
			for (int k = 1; k < inputer.getGroupNum(); k++)
				if (decisions[k] > decisions[treeDecision])
					treeDecision = k;
			if (trainlist.get(i).getGroup() == treeDecision)
				sucCount += 1;
		}
		*/
		
		//return ((double)sucCount / (double)trainlist.size());
		return ((double)sucCount / (double)testlist.size());
	}
	
	public static void main(String[] argv) throws IOException {
		
		Experiment experiment = new Experiment();
		System.out.println("welcom:");
		
		String input;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
//		DataInputStream in = new DataInputStream(new BufferedInput(System.in));
//		input = in.readLine();
		FileWriter fw = new FileWriter(answerSource);
		double totalValue = 0.0;
		for (int i = 0; i < times; i++) {	
			if (true) {
				long start = System.currentTimeMillis();
				experiment.root = null;
//				System.out.println("请输入训练文件的绝对路径");
//				String tempname = "E:\\iris_uncertain.txt";
//				String tempname = "E:\\glass_uncertain_noid.txt";
//				String tempname = "E:\\glass_uncertain.txt";
				String tempname = "E:\\satellite_uncertain.txt";
				experiment.prepareList(tempname);
				
				int treeSize = experiment.train();
				//进行测试
				if (experiment.root == null) {
					System.out.println("训练失败!");
					input = in.readLine();
					continue;
				}
				double sucrates = experiment.test();
				totalValue += sucrates;
				long end = System.currentTimeMillis();
				fw.append(i + " : " + sucrates + "\n");
				System.out.println("准确率为: " + sucrates);
				System.out.println("finish " + i + " time; cost " + (end-start)/1000 + " seconds");
			}
			
//			input = in.readLine();
		}
		fw.append("平均准确率" + totalValue/times);
		fw.close();	
		
		return;
	}
}
