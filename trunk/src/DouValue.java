/**
 * this class is the pair value
 * For example: <1,1>
 * @author carol
 *
 */
public class DouValue <T1, T2>{
	public T1 first = null;
	public T2 second = null;
	
	public DouValue(T1 firstValue, T2 secondValue) {
		first = firstValue;
		second = secondValue;
	}
}
