import java.util.ArrayList;
import java.util.Arrays;

public class Cell
{
	public int row;
	public int col;
	public int value;
	public ArrayList<Integer> domain;
	public boolean set;
	
	Cell()
	{
		domain = new ArrayList<Integer>();
		row = 0;
		col = 0;
		value = 0;
	}
	
	Cell(int r, int c)
	{
		//originally each cell's domain can be any number 1-9
		domain = new ArrayList<Integer>(Arrays.asList(1,2,3,4,5,6,7,8,9));
		row = r;
		col = c;
		value =  0;
		set = false;
	}
	
	Cell(int r, int c, int v, ArrayList<Integer> d)
	{
		row = r;
		col = c;
		value = v;
		domain = d;
		set = false;
	}
}
