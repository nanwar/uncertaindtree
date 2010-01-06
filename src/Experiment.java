import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.io.*;

public class Experiment {
	private DtreeNode root = null;
	private InstanceInput inputer = new InstanceInput();
	
	/**
	 * when train, used to store the train instances(part of the fullList)
	 * when test, used to store the test instances
	 */
	private ArrayList<Instance> trainlist;
	private ArrayList<Instance> testlist;
	
	public void prepareList(String trainFilename) {
		int range = 80;
		ArrayList<Instance> fullList = inputer.inputFile(trainFilename);
		trainlist = new ArrayList<Instance>();
		testlist = new ArrayList<Instance>();
		root = new DtreeNode(inputer.getGroupNum(), 1);
		
		Random rand = new Random();
		rand.setSeed(new Date().getTime());

		/**
		 * listSize now is the totel fullList
		 */
		int trainlistSize = fullList.size() * range / 100;
		int testlistSize = fullList.size() - trainlistSize;
		//int trainlistSize = fullList.size() * 8 / 100;
		//int testlistSize = fullList.size() * 2 / 100;

		while (trainlistSize > 0) {
			int picNumber = rand.nextInt(fullList.size());
			trainlist.add(fullList.get(picNumber));
			fullList.remove(picNumber);
			trainlistSize--;
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
		
		return root.getTreeSize();
//		return 0;
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
		input = in.readLine();
		
		while (!input.equals("exit")){
			if (true) {
				experiment.root = null;
				System.out.println("请输入训练文件的绝对路径");
				
				//String filename = in.readLine();
				//String tempname = "E:\\satellite_uncertain.txt";
				//String tempname = "E:\\test_uncertain.txt";
				String tempname = "E:\\glass_uncertain_noid.txt";
				//String tempname = "E:\\glass_uncertain.txt";
				//String tempname = "E:\\iris_uncertain.txt";
				experiment.prepareList(tempname);
				
				int treeSize = experiment.train();
				System.out.println("size = " + treeSize);
				
				//进行测试
				if (experiment.root == null) {
					System.out.println("训练失败!");
					input = in.readLine();
					continue;
				}
				double sucrates = experiment.test();
				System.out.println("准确率为: " + sucrates);
			}
			
			input = in.readLine();
		}
		
		return;
	}
}
