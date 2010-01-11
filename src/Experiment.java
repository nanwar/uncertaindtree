import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.io.*;

public class Experiment {
	/**
	 * �����ľ��������ڵ�
	 */
	private DtreeNode root = null;
	/**
	 * һ��ʵ�������ݶ������
	 */
	private InstanceInput inputer = new InstanceInput();
	/**
	 * һ��ʵ������Ԫ��ĸ���������ѵ�����Ͳ��Լ�
	 */
	public static int instanceNum = 0;
	/**
	 * һ��ʵ��������־�ĸ���
	 */
	public static int groupNum1 = 0;
	/**
	 * һ��ʵ����Ԫ�����Եĸ���
	 */
	public static int attriNum1 = 0;
	/**
	 *��־Ҷ�ӽڵ������ĳ��Ȩ�ر�������һ��ֵ�Ļ���ֱ�ӹ鲢Ϊ�ϸ��ڵ� 
	 */
	public static double canMerge = 0.9;
	/**
	 * �ж��Ƿ�������ĳ���ڵ�ķֲ�����ĳ�������֦��true���Լ�֦
	 */
	public static boolean pruneByMerge = false;
	/**
	 * �Ƿ���ж����߶����Ƶļ�֦
	 */
	public static boolean pruneByHight = true;
	/**
	 * �Ƿ�������ĳ���ڵ���Ŀ���ٶ���֦
	 */
	public static boolean pruneByNodeNum = false;
	/**
	 * ��־�ڵ���������ĳ�������;��м�֦�İٷֱ�
	 */
	public static double canNodeNum = 0.005;
	/**
	 * �Ƿ����cart�㷨�Դ����ʶ���֦
	 */
	public static boolean pruneByError = false;
	/**
	 * ��һ��ʵ��ִ�еĴ���
	 */
	public static int times = 10;
	/**
	 * �������ļ�
	 */
	public static String answerSource = "answer.txt";
	/**
	 * ѵ����
	 */
	private ArrayList<Instance> trainlist;
	/**
	 * ���Լ�
	 */
	private ArrayList<Instance> testlist;
	/**
	 * ��֦���Լ�
	 */
	private ArrayList<Instance> prunelist;
	
	/**
	 * ������Ԫ����зָ�õ�3���Ӽ�:ѵ���������Լ��Լ���֦���Լ�
	 * @param trainFilename
	 * �����ļ�·��
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
	 * �������Ľ����������߶ȼ�֦�����֦:����ýڵ��Ԫ����С��һ��������ֹͣ���ѣ�
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
		//���֦�����ݽڵ�ֲ����
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
//				System.out.println("������ѵ���ļ��ľ���·��");
//				String tempname = "E:\\iris_uncertain.txt";
//				String tempname = "E:\\glass_uncertain_noid.txt";
//				String tempname = "E:\\glass_uncertain.txt";
				String tempname = "E:\\satellite_uncertain.txt";
				experiment.prepareList(tempname);
				
				int treeSize = experiment.train();
				//���в���
				if (experiment.root == null) {
					System.out.println("ѵ��ʧ��!");
					input = in.readLine();
					continue;
				}
				double sucrates = experiment.test();
				totalValue += sucrates;
				long end = System.currentTimeMillis();
				fw.append(i + " : " + sucrates + "\n");
				System.out.println("׼ȷ��Ϊ: " + sucrates);
				System.out.println("finish " + i + " time; cost " + (end-start)/1000 + " seconds");
			}
			
//			input = in.readLine();
		}
		fw.append("ƽ��׼ȷ��" + totalValue/times);
		fw.close();	
		
		return;
	}
}
