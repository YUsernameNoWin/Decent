import java.util.ArrayList;
import java.util.List;


public class CountableArrayList<T> extends ArrayList<T> {
	
	/**
	 *  Provides an active list of peers dependent on the peer's active boolean.
	 */
	private static final long serialVersionUID = -1548561439412150277L;
	public CountableArrayList(int capacity)
	{
		super(capacity);
	}
	public CountableArrayList()
	{
		super();
	}
	public int activeSize(){
		int size=0;
		for(int ix =0;ix<super.size();ix++){
			if(!((Peer)get(ix)).active)
			break;
			else
				size++;
		}
			
		return size;
		
	}
}
