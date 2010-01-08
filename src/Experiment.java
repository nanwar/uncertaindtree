import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.io.*;

public class Experiment {
	private DtreeNode root = null;
	private InstanceInput inputer = new InstanceInput();
	
	public static int instanceNum = 0;
	public static int groupNum1 = 0;
	public static int attriNum1 = 0;
	/**
	 *标志叶子节点中如果某个权重比例超过一定值的话就直接归并为上个节点 
	 */
	public static double canMerge = 0.90;

	/**
	 * when train, used to store the train instances(part of the fullList)
	 * when test, used to store the test instances
	 */
	private ArrayList<Instance> trainlist;
	private ArrayList<Instance> testlist;
	private ArrayList<Instance> prunelist;
	
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
		 * listSize now is the totel fullList
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
	 * filename: input train file, src.train
	 * range: 60 means 60% of the train instances
	 * return: tree size
	 * @param filename
	 * @return
	 */
	public int train() {
		boolean[] attrUsed = new boolean[inputer.getAttriNum()];
		
		for (int i = 0; i < attrUsed.length; i++)
			attrUsed[i] = false;
		
		root.learn(trainlist,attrUsed);
		System.out.println("before size: " + root.getTreeSize());
		//后剪枝
		sufPruning(root);
		System.out.println("after size: " + root.getTreeSize());
		root.cartPruning(root, prunelist);
		//JIE
		//root.cartPruning(prunelist);
		System.out.println("after size1: " + root.getTreeSize());
		return root.getTreeSize();
//		return 0;
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
//		System.out.println("max/total:" + max/total);
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
		
		//DataInputStream in = new DataInputStream(new BufferedInput(System.in));
//		input = in.readLine();
		int times = 50;
		FileWriter fw = new FileWriter("answer.txt");
		double totalValue = 0.0;
		for (int i = 0; i < times; i++) {	
			if (true) {
				long start = System.currentTimeMillis();
				experiment.root = null;
//				System.out.println("请输入训练文件的绝对路径");
//				String tempname = "E:\\iris_uncertain.txt";
				String tempname = "E:\\glass_uncertain_noid.txt";
//				String tempname = "E:\\glass_uncertain.txt";
//				String tempname = "E:\\satellite_uncertain.txt";
				experiment.prepareList(tempname);
				
				int treeSize = experiment.train();
//				System.out.println("size = " + treeSize);
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
