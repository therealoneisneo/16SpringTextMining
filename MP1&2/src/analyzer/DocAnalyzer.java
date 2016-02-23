/**
 * 
 */
package analyzer;

import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
//import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import structures.LanguageModel;
import structures.Post;
import structures.Token;

/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 * NOTE: the code here is only for demonstration purpose, 
 * please revise it accordingly to maximize your implementation's efficiency!
 */
public class DocAnalyzer {
	//N-gram to be created
	int m_N;
	int m_train_num; // the number of training examples
	int m_test_num; // the number of test examples
	//a list of stopwords
	HashSet<String> m_stopwords;
	
	//you can store the loaded reviews in this arraylist for further processing
//	ArrayList<Post> m_Train_reviews;
//	ArrayList<Post> m_Test_reviews;
	ArrayList<Post> m_reviews;
	
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	HashMap<String, Token> m_stats;	
	HashMap<String, Token> m_dfstats; // the count for df
	
	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;
	
	//this structure is for language modeling
	LanguageModel m_langModel;
	
	public DocAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {
		m_train_num = -1;
		m_test_num = -1;
		m_N = N;
		m_reviews = new ArrayList<Post>();
		m_stats = new HashMap<String, Token>();
		m_dfstats = new HashMap<String, Token>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<String>();
	}
	
	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				line = SnowballStemming(Normalization(line));
				if (!line.isEmpty())
					m_stopwords.add(line);
//					System.out.println(line);
			}
			
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}
	

	
	
	
	public void analyzeDocument(JSONObject json) {		
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			HashSet<String> dfcheck;
			dfcheck = new HashSet<String>();
			for(int i=0; i<jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				String[] tokens = Tokenize(review.getContent());
				review.setTokens(tokens);

				dfcheck.clear();
				
				if (m_N == 1)
				{
					for (String tok : tokens)
					{
						tok = Normalization(tok);
						if (tok.length() == 0){break;}
						if (!m_stopwords.contains(tok))
						{
							dfcheck.add(tok);
							if (m_stats.containsKey(tok))
							{
								Token temp = m_stats.get(tok);
								temp.setValue(temp.getValue() + 1); // increase count by 1
							}
							else
							{
								Token newt = new Token(m_stats.size(), tok);
								m_stats.put(tok, newt);
//								System.out.println(tok);
							}
						}
						
					}
					
					for (String tok : dfcheck)
					{
						if (m_dfstats.containsKey(tok))
						{
							Token temp = m_dfstats.get(tok);
							temp.setValue(temp.getValue() + 1); // increase count by 1
						}
						else
						{
							Token newt = new Token(m_dfstats.size(), tok);
							m_dfstats.put(tok, newt);
						}
					}
				}
				
				else if (m_N == 2)
				{
					for (int t = 0; t< tokens.length - 1; t++)
					{
						String tok1 = Normalization(tokens[t]);
						String tok2 = Normalization(tokens[t + 1]);

						if (!m_stopwords.contains(tok1) && !m_stopwords.contains(tok2) && tok1.length() != 0 && tok2.length() != 0 )
						{
							String tok = tok1 + "_" + tok2;
							dfcheck.add(tok);
							if (m_stats.containsKey(tok))
							{
								Token temp = m_stats.get(tok);
								temp.setValue(temp.getValue() + 1); // increase count by 1
							}
							else
							{
								m_stats.put(tok, new Token(tok));
							}
						}
						
					}
					
					for (String tok : dfcheck)
					{
						if (m_dfstats.containsKey(tok))
						{
							Token temp = m_stats.get(tok);
							temp.setValue(temp.getValue() + 1); // increase count by 1
						}
						else
						{
							m_dfstats.put(tok, new Token(tok));
						}
					}
				}
				
				
				m_reviews.add(review);
				System.out.println(m_stats.size());

			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void createLanguageModel() {
		m_langModel = new LanguageModel(m_N, m_stats.size());
		
		for(Post review:m_reviews) {
			String[] tokens = Tokenize(review.getContent());
			/**
			 * HINT: essentially you will perform very similar operations as what you have done in analyzeDocument() 
			 * Now you should properly update the counts in LanguageModel structure such that we can perform maximum likelihood estimation on it
			 */
		}
	}
	
	//sample code for loading a json file
	public JSONObject LoadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;
			
			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();
			
			return new JSONObject(buffer.toString());
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
		}
	}
	
	// sample code for demonstrating how to recursively load files in a directory 
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder);
		int size = m_reviews.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix))
				analyzeDocument(LoadJson(f.getAbsolutePath()));
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
		}
		size = m_reviews.size() - size;
		if (m_train_num < 0)
		{
			m_train_num = size;
		}
		else if (m_test_num < 0)
		{
			m_test_num = size;
		}
		System.out.println("Loading " + size + " review documents from " + folder);
	}

	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemming(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	//sample code for demonstrating how to use Porter stemmer
	public String PorterStemming(String token) {
		porterStemmer stemmer = new porterStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	//sample code for demonstrating how to perform text normalization
	//you should implement your own normalization procedure here
	public String Normalization(String token) {
		// remove all non-word characters
		// please change this to removing all English punctuation
//		
		
		token = token.replaceAll("\\p{Punct}+", "");
//		token = token.replaceAll("[^A-Za-z0-9]", "");
		
//		remove punctuation
	
		
		// convert to lower case
		token = token.toLowerCase(); 
		
		if (token.matches("[+-]*[0-9]+(\\.[0-9]+)*")) // substitude nums
		{
			token = "NUM";
		}
		
		// add a line to recognize integers and doubles via regular expression
		// and convert the recognized integers and doubles to a special symbol "NUM"
		token = token.replaceAll("\\W+", "");
		
		return token;
	}
	
	String[] Tokenize(String text) {
		return m_tokenizer.tokenize(text);
	}
//	public static void DFCalc(HashMap<String, Token> m_dfstats)
//	{
//		
//	}
	
	public static void ZipfsLaw(HashMap<String, Token> m_stats, String Filename)
	{

		List<Map.Entry<String, Token>> entrylist = new ArrayList<Map.Entry<String, Token>>(m_stats.entrySet());

		
		Collections.sort(entrylist, new Comparator<Map.Entry<String, Token>>()
			{public int compare(Map.Entry<String, Token> o1, Map.Entry<String, Token> o2)
				{
					return((int)o2.getValue().getValue() - (int)o1.getValue().getValue());
				}
			});
		FileWriter fw = null;
		try
		{
			fw = new FileWriter(Filename);
			String title = "Key,Value\r\n";
			fw.write(title);
			
			for (Entry<String, Token> entry : entrylist)
			{
				System.out.println(entry.getKey() + "\t" + entry.getValue().getValue());
				fw.write(entry.getKey() + "," + entry.getValue().getValue()+ "\r\n"); 
//			break;
			}
			 fw.close(); 
		} catch (IOException e){e.printStackTrace();}
	}
	
	
	
	public void TokenizerDemon(String text) {
		System.out.format("Token\tNormalization\tSnonball Stemmer\tPorter Stemmer\n");
		for(String token:m_tokenizer.tokenize(text)){
			System.out.format("%s\t%s\t%s\t%s\n", token, Normalization(token), SnowballStemming(token), PorterStemming(token));
		}		
	}
	
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException {		
		DocAnalyzer analyzer = new DocAnalyzer("./data/Model/en-token.bin", 1);
		DocAnalyzer analyzer2 = new DocAnalyzer("./data/Model/en-token.bin", 2);
		
		//code for demonstrating tokenization and stemming
//		analyzer.TokenizerDemon("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this.");
		// analyzer.TokenizerDemon("this is just a test sentence for the function");
			
		//entry point to deal with a collection of documents
		analyzer.LoadStopwords("init_stop_words.txt");
		analyzer.LoadDirectory("./Data/yelp/train", ".json");
//		analyzer.LoadDirectory("./Data/yelp/test", ".json");
		analyzer2.LoadStopwords("init_stop_words.txt");
		analyzer2.LoadDirectory("./Data/yelp/train", ".json");
//		analyzer2.LoadDirectory("./Data/yelp/test", ".json");
		HashMap<String, Token> all_TFs, all_DFs;
		all_TFs = new HashMap<String, Token>();
		all_DFs = new HashMap<String, Token>();
		all_TFs.putAll(analyzer.m_stats);
		all_TFs.putAll(analyzer2.m_stats);
		all_DFs.putAll(analyzer.m_dfstats);
		all_DFs.putAll(analyzer2.m_dfstats);
		DocAnalyzer.ZipfsLaw(all_TFs, "tf_result.csv");
		DocAnalyzer.ZipfsLaw(all_DFs, "df_result.csv");
		System.out.println(analyzer.m_train_num);
		
//		System.out.println(all_tokens.size());

		System.out.println("complete");
	}

}
