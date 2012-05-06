import java.util.ArrayList;
import java.util.List;


public class CountableArrayList<E> extends ArrayList<E> {
	
	/**
	 *  Provides an active list of peers dependent on the peer's active boolean.
	 */
	private static final long serialVersionUID = -1548561439412150277L;
	public  int activeSize  = 0;
	public CountableArrayList(int capacity)
	{
		super(capacity);
	}
	public CountableArrayList()
	{
		super();
	}
   public boolean add(E e) {
       activeSize++;
       return super.add(e);
    }
   public boolean remove(Object e) {
       activeSize--;
       return super.remove(e);
    }
   public E remove(int e) {
       activeSize--;
       return super.remove(e);
    }
	public E get(int index) {
	    int newIndex = index;
	    if(index > activeSize - 1)
	        newIndex = 0;
	    if(index < 0)
	        newIndex = activeSize-1;
       return super.get(newIndex);
	}
}
