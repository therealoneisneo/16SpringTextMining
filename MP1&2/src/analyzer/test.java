import java.util.*;

public class Test
{
	HashMap<String, int> m_stats;

	public static void main(String[] args)
	{
		Test temp = new Test();
		temp.m_stats.put("testinput1", 1);
		temp.m_stats.put("testinput2", 2);
		for (HashMap<String, int> pt : temp.m_stats)
		{
			System.out.println(pt.getvalue())
		}
	}
}