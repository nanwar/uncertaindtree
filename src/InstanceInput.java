import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Hashtable;

//import Attribute.Type;

public class InstanceInput {
	/**
	 *属性值维数 
	 */
	private int attriNum = 0;
	public int getAttriNum() {
		return attriNum;
	}
	/**
	 * 测试集的类别数
	 */
	private int groupNum = 0;
	public int getGroupNum() {
		return groupNum;
	}
	/**
	 * 重新定义的类别记录
	 * 从零开始标记类别号
	 */
	private ArrayList<Integer> groupList = new ArrayList<Integer>();
	public int getOriginalClassLabel(int id){
		return groupList.get(id);
	}
	private char prefix = '[';
	private char suffix = ']';
	private char splitSign = ',';
	
	public ArrayList<Instance> inputFile (String filename) {
		File file = new File(filename);
		ArrayList<Instance> instanceList = new ArrayList<Instance> ();
		
		//标志是否已经判断属性值维数
		boolean flag = false;
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			while (line != null && !line.trim().equals("")) {
				//识别属性值维度
				if(!flag){
					for (int i = 0; i < line.length(); i++) {
						if(line.charAt(i) == prefix){
							attriNum ++;
						}
					}
//System.out.println("attriNum:" + attriNum);
					flag = true;
				}
				
				//读入该行数据的类别
				String tempGroup = line.substring(0, 1);
				//直接读到的类别号
				int readGroup = Integer.parseInt(tempGroup);
				//重新定义的类别号
				int newGroup = -1;
				if(!groupList.contains(readGroup)){
					groupList.add(readGroup);
					newGroup = groupNum;
					groupNum ++;
				}
				else{
					newGroup = groupList.indexOf(readGroup);
				}
				//读入该行数据的属性值
				String concatString = line.substring(4, line.length());
				//建立该行数据的实例
				//attriString的string格式: a, b
				Attribute[] attrs = new Attribute[attriNum];
				String[] attriString = splitFix(concatString);
				for (int i = 0; i < attriString.length; i++) {
					int dotIndex = attriString[i].indexOf(splitSign);
					attrs[i] = new Attribute(attriString[i].substring(0, dotIndex), attriString[i].substring(dotIndex + 2));
				}
//System.out.println("group:" + readGroup + "length:"+attriString.length);
				//TODO:这里存的类别号仍然是直接读到的整数值，可以替换成重新编号的类别，这里是newGroup
				instanceList.add(new Instance(attrs, newGroup));
				line = br.readLine();
			}
			br.close();
		} catch(IOException e) {
			System.out.println("Could not input instance!");
		}
		return instanceList;
	}
	
	private String[] splitFix(String srcString){
		String[] ans = new String[attriNum];
		for (int i = 0; i < ans.length; i++) {
			ans[i] = srcString.substring(1,srcString.indexOf(suffix));
			if(i != (ans.length-1)){
				srcString = srcString.substring(srcString.indexOf(suffix)+2);
			}
		}
		return ans;
	}
	/*
	public static void main(String[] args){
		InstanceInput in = new InstanceInput();
		in.inputFile("4.txt");
		for (int i = 0; i < in.groupList.size(); i++) {
			System.out.println("i = " + i + "; " + in.getOriginalClassLabel(i));
		}
		System.out.println(in.groupNum);
	}
	*/
}
