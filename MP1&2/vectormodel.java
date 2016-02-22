import src.analyzer.DocAnalyzer.analyzer;


public class vectormodel
{
	public static void main(String[] args) 
	{
		DocAnalyzer test;
		test = new DocAnalyzer();
		test.LoadStopwords("init_stop_words.txt");
	}
}