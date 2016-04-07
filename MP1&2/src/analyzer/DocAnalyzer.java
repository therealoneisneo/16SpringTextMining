/**
 *  2016 spring Text Mining machine problem
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
	double DocNum;//the total number of documents in train
	//a list of stopwords
	HashSet<String> m_stopwords;
	HashSet<String> m_CtrlVocabulary;
	int m_train_num;
	int m_test_num;
	//you can store the loaded reviews in this arraylist for further processing
//	ArrayList<Post> m_Train_reviews;
//	ArrayList<Post> m_Test_reviews;
	ArrayList<Post> m_reviews;
	ArrayList<Post> m_Qreviews;
	
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	HashMap<String, Token> m_stats;	
	HashMap<String, Token> m_dfstats; // the count for df

	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;
	
	//this structure is for language modeling
	LanguageModel m_langModel;
	
	public DocAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {

		m_N = N;
		m_reviews = new ArrayList<Post>();
		m_Qreviews = new ArrayList<Post>();
		m_stats = new HashMap<String, Token>();
		m_dfstats = new HashMap<String, Token>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<String>();
		m_CtrlVocabulary = new HashSet<String>();
	}
	
	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	public void LoadStopwords(String filename) {
		try {
			m_stopwords.clear();
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
	
	
	public HashMap<String,Double> LoadDFs(String filename) {
		HashMap<String,Double> DFs = new HashMap<String,Double>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			String[] contents;
			
			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				if (!line.isEmpty())
				{
					contents = line.split(",");
					if (m_CtrlVocabulary.contains(contents[0]))
					{DFs.put(contents[0], Double.parseDouble(contents[1]));}
				}
					
//					System.out.println(line);
			}
			reader.close();
			
			System.out.format("Loading %d DF from %s\n", DFs.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
		return DFs;
	}
	
	
	public HashMap<String,Double> LoadTFs(String filename) {
		HashMap<String,Double> TFs = new HashMap<String,Double>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			String[] contents;
			
			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				if (!line.isEmpty())
				{
					contents = line.split(",");
//					System.out.println(contents[0]);
//					System.out.println(Double.parseDouble(contents[1]));
					if (m_CtrlVocabulary.contains(contents[0]))
					{TFs.put(contents[0], Double.parseDouble(contents[1]));}
					
				}
					
//					System.out.println(line);
			}
			reader.close();
			
//			System.out.format("Loading %d controlled vocabulary from %s\n", m_CtrlVocabulary.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
		return TFs;
	}
	
	
	
	
	public List<String> LoadVocabulary(String filename) {
		List<String> VList = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
//			List<String> testlist = new ArrayList<String>();
			
			
			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
//				String temp;
//				temp = SnowballStemming(Normalization(line));
				if (!line.isEmpty())
				{
					if (m_CtrlVocabulary.contains(line))
					{
						VList.add(line);
						System.out.println(line);
					}
					m_CtrlVocabulary.add(line);
//					testlist.add(line);
				}
					
//					System.out.println(line);
			}
			reader.close();
			System.out.format("Loading %d controlled vocabulary from %s\n", m_CtrlVocabulary.size(), filename);
			
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
		return VList;
	}
	
	
	public static void OutputWordCount(HashMap<String, Token> entrylist, String filename)
	{
		try
		{
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
			for (Map.Entry<String, Token> entry : entrylist.entrySet())
			{
				out.write(entry.getKey() + "," + entry.getValue().getValue() + "\r\n");
			}
			out.flush();
			out.close(); 
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void OutputWordCount(List<Map.Entry<String, Token>> entrylist, String filename)
	{
		try
		{
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
			for (Entry<String, Token> entry : entrylist)
			{
				out.write(entry.getKey() + "," + entry.getValue().getValue() + "\r\n");
			}
			out.flush();
			out.close(); 
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void OutputWordList(HashSet<String> entrylist, String filename)
	{
		try
		{
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
			for (String entry : entrylist)
			{
				out.write(entry + "\r\n");
			}
			out.flush();
			out.close(); 
		  
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void OutputWordList(List<String> entrylist, String filename)
	{
		try
		{
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
			for (String entry : entrylist)
			{
				out.write(entry + "\r\n");
			}
			out.flush();
			out.close(); 
		  
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void LoadLMDocument(JSONObject json) 
	{		
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			for(int i=0; i<jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				m_reviews.add(review);
			}
		} catch (JSONException e) {
			e.printStackTrace();
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
						tok = SnowballStemming(Normalization(tok));
						if (!m_stopwords.contains(tok) && tok.length() != 0)
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
								newt.setValue(1); 
								m_stats.put(tok, newt);
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
							newt.setValue(1); 
							m_dfstats.put(tok, newt);
						}
					}
				}
				
				else if (m_N == 2)
				{
					for (int t = 0; t< tokens.length - 1; t++)
					{
						String tok1 = SnowballStemming(Normalization(tokens[t]));
						String tok2 = SnowballStemming(Normalization(tokens[t + 1]));

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
								Token newt = new Token(m_stats.size(), tok);
								newt.setValue(1);
								m_stats.put(tok, newt);
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
							Token newt = new Token(m_stats.size(), tok);
							newt.setValue(1);
							m_dfstats.put(tok, newt);
						}
					}
				}
				
				
				m_reviews.add(review);
				

			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	//Load the query.json
//	public void analyzeQurey(JSONObject json) {		
	public void LoadQurey(String Filename) {		
		try {
			File f = new File(Filename);
			JSONObject json = LoadJson(f.getAbsolutePath());
			JSONArray jarray = json.getJSONArray("Reviews");
	
			for(int i=0; i<jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				String[] tokens = Tokenize(review.getContent());
				review.setTokens(tokens);
				m_Qreviews.add(review);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	
	// reload documents with controlled vocabulary to build vector space
	public void analyzeDocumentVM(ArrayList<Post> reviewentry) {		
		
		List<String> CtrlVoc = new ArrayList<String>(m_CtrlVocabulary);
		int CtrlSize = CtrlVoc.size();
		int count = 0;
		for(Post review : reviewentry)
		{
			System.out.println(count);
			count += 1;
			String[] tokens = Tokenize(review.getContent());
			review.setTokens(tokens);
			review.initTVec(CtrlSize);
			
			for (int t = 0; t< tokens.length - 1; t++)
			{
				String tok1 = SnowballStemming(Normalization(tokens[t]));
				String tok2 = SnowballStemming(Normalization(tokens[t + 1]));
				
				int index1 = CtrlVoc.indexOf(tok1);
				int index2 = CtrlVoc.indexOf(tok2);
				
				if (index1 >= 0)
				{
					double temp = review.getTvecValue(index1);
					temp += 1;
					review.setTvecValue(index1, temp);
				}
				
				if (index1 >= 0 && index2 >= 0)
				{
					int index3 = CtrlVoc.indexOf(tok1 + "_" + tok2);
					if (index3 >= 0)
					{
						double temp = review.getTvecValue(index3);
						temp += 1;
						review.setTvecValue(index3, temp);
					}
				}
			}
		}
	}
	

	public void LoadLMDir(String folder, String suffix) 
	{
		File dir = new File(folder);
		int size = m_reviews.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix))
			{
				LoadLMDocument(LoadJson(f.getAbsolutePath()));
//				System.out.println(m_stats.size());
			}
			else if (f.isDirectory())
				LoadLMDir(f.getAbsolutePath(), suffix);
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
	
	public double PPcalc(LanguageModel langModel, String SmType)//calculate the perplexity of 
	{
		double valueAll = 0;
		double countAll = 0;
		if (langModel.getGramNum() == 1)// unigram
		{
			for(Post review : m_reviews) 
			{
				countAll += 1;
				double value = 0;
				double count = 0;
				String[] tokens = Tokenize(review.getContent());
//				review.setTokens(tokens);
				for (String tok : tokens)
				{
					tok = SnowballStemming(Normalization(tok));
					if (tok.length() > 0)
					{
						count += 1;
						value += Math.log10(langModel.calcLinearSmoothedProb(tok));
					}
				}
				if (count != 0)
					valueAll += -1 * value / count;
			}
		}
		else//bigram
		{
			int testcount = 0;
			for(Post review : m_reviews) 
			{
				testcount += 1;
				countAll += 1;
				double value = 0;
				double count = 0;
				String[] tokens = Tokenize(review.getContent());
//				review.setTokens(tokens);
				for (int i = 0; i < tokens.length - 1; i++)
				{
					String tok1 = SnowballStemming(Normalization(tokens[i]));
					String tok2 = SnowballStemming(Normalization(tokens[i + 1]));
					if (tok1.length() > 0 && tok2.length() > 0)
					{
						String tok = tok1 + "_" + tok2;
						count += 1;
						if (SmType.equals("lin"))
							value += Math.log10(langModel.calcLinearSmoothedProb(tok));
						else
							value += Math.log10(langModel.calcAbsSmoothedProb(tok));
					}
				}
				if(count != 0)
					valueAll += -1 * value / count;
				
			}
		}
		return valueAll / countAll;
	}
	
	public void clearReview()// clear all the review in an analyzer
	{
		m_reviews.clear();
	}
	public void createLanguageModel() {
		m_langModel = new LanguageModel(m_N);
//		int count = 0;
		if (m_N == 1)// unigram model, calculate all terms
		{
			for(Post review : m_reviews) 
			{
				String[] tokens = Tokenize(review.getContent());
				review.setTokens(tokens);
				for (String tok : tokens)
				{
					tok = SnowballStemming(Normalization(tok));
					if (tok.length() > 0)
					{
						m_langModel.increCount(); 
						m_langModel.addToken(tok); // add token to the m_model
					}	
				}
				/**
				 * HINT: essentially you will perform very similar operations as what you have done in analyzeDocument() 
				 * Now you should properly update the counts in LanguageModel structure such that we can perform maximum likelihood estimation on it
				 */
			}
		}
		
		else//bigram model
		{
			if (m_N == 2)// the bigram model
			{
				for(Post review : m_reviews) 
				{
					String[] tokens = Tokenize(review.getContent());
					review.setTokens(tokens);
					for (int i = 0; i < tokens.length - 1; i++)
					{
						String tok1 = SnowballStemming(Normalization(tokens[i]));
						String tok2 = SnowballStemming(Normalization(tokens[i + 1]));
						if (tok1.length() > 0 && tok2.length() > 0)
						{
//							if (tok1.equals("good"))
//								count += 1;
							String tok = tok1 + "_" + tok2;
							m_langModel.increCount(); 
							m_langModel.addToken(tok); // add token to the m_model	
						}
					}
				}
//				System.out.println("bigram starts with good number: " + count);
				m_langModel.bigramTokenProcess();
//				m_langModel.testbigrams();
			}
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
			{
				analyzeDocument(LoadJson(f.getAbsolutePath()));
				System.out.println(m_stats.size());
			}
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

//		remove punctuation

		// convert to lower case
		token = token.toLowerCase(); 
		
		if (token.matches("[+-]*[0-9]+(\\.[0-9]+)*")) // substitude nums
		{
			token = "NUM";
		}
		
		// add a line to recognize integers and doubles via regular expression
		// and convert the recognized integers and doubles to a special symbol "NUM"
//		token = token.replaceAll("[^A-Za-z0-9]", "");
		token = token.replaceAll("\\W+", "");
		
		return token;
	}
	
	String[] Tokenize(String text) {
		return m_tokenizer.tokenize(text);
	}


	public static List<Map.Entry<String, Token>> SortHashMap(HashMap<String, Token> m_stats)
	{
		List<Map.Entry<String, Token>> entrylist = new ArrayList<Map.Entry<String, Token>>(m_stats.entrySet());

		Collections.sort(entrylist, new Comparator<Map.Entry<String, Token>>()
			{public int compare(Map.Entry<String, Token> o1, Map.Entry<String, Token> o2)
				{
					return((int)o2.getValue().getValue() - (int)o1.getValue().getValue());
				}
			});
		return entrylist;
	}
	
	public static void ZipfsLaw(HashMap<String, Token> m_stats, String Filename)
	{

		List<Map.Entry<String, Token>> entrylist = SortHashMap(m_stats);
//				new ArrayList<Map.Entry<String, Token>>(m_stats.entrySet());
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
//	}
	
	public void TokenizerDemon(String text) {
		System.out.format("Token\tNormalization\tSnonball Stemmer\tPorter Stemmer\n");
		for(String token:m_tokenizer.tokenize(text)){
			System.out.format("%s\t%s\t%s\t%s\n", token, Normalization(token), SnowballStemming(token), PorterStemming(token));
		}		
	}
	
	public void BuildCVocabulary(List<Map.Entry<String, Token>> all_df_sorted)
	{
		int count = 0;
		System.out.println("Building CV...");
		for (Map.Entry<String, Token> entry : all_df_sorted)
		{
			if (count < 100)
			{
				System.out.println("StopWord add  " + entry.getKey()+ "  " + entry.getValue().getValue());
				
				m_stopwords.add(entry.getKey());
				count += 1;
			}
			else
			{
				double df = entry.getValue().getValue();
				if (df < 50)
				{break;}
				m_CtrlVocabulary.add(entry.getKey());
			}
			
		}
	}
	
	
	
	public void TF_IDFCalc(Post review, HashMap<String, Double> DFs, int DocNum)
	{
		List<String> CtrlVoc = new ArrayList<String>(m_CtrlVocabulary);
		int size = CtrlVoc.size();
		for (int i = 0; i < size; i++)
		{
			double tf = review.getTvecValue(i);
			if (tf > 0)
			{
				tf = 1 + Math.log10(tf);
				String tk = CtrlVoc.get(i);
				System.out.println(tk);
//				Token testtoken = DF.get(tk);
				double idf = DFs.get(CtrlVoc.get(i));
				idf = 1 + Math.log10((double)DocNum/idf);
				review.setTvecValue(i, tf * idf);
			}
		}
	}
	
	
	
	
	public void CalcSimi()//Calculate the similarity between query and test
	{
		int count = 0;
		for(Post query : m_Qreviews)
		{
			count += 1;
			System.out.println(count);
			Post[] m = new Post[3];
			double[] sims = {0,0,0};

			for (Post test : m_reviews)
			{
				double sim = query.similiarity(test);
				if (sim > sims[0])
				{
					sims[2] = sims[1];
					sims[1] = sims[0];
					sims[0] = sim;
					m[2] = m[1];
					m[1] = m[0];
					m[0] = test;
				}
				else if(sim > sims[1])
				{
					sims[2] = sims[1];
					sims[1] = sim;
					m[2] = m[1];
					m[1] = test;
				}
				else if (sim > sims[2])
				{
					sims[2] = sim;
					m[2] = test;
				}
			}
			
			System.out.println("\n\n\n\n");
			System.out.println("query review : ");
			query.Output();
			System.out.println("\n\n");
			for(int i = 0; i < 3; i++)
			{
				System.out.println("similar review " + (i + 1) + " similarity : " + sims[i]);
				m[i].Output();
				System.out.println("\n");
			}
			
			System.out.println("\n\n\n\n");
		}
	}
	
	
	
	
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException 
	{	
		
		
		
		DocAnalyzer analyzer = new DocAnalyzer("./data/Model/en-token.bin", 1);
//		DocAnalyzer analyzer2 = new DocAnalyzer("./data/Model/en-token.bin", 2);

		//code for demonstrating tokenization and stemming
//		analyzer.TokenizerDemon("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this.");
		// analyzer.TokenizerDemon("this is just a test sentence for the function");
			
		//entry point to deal with a collection of documents
		HashMap<String, Token> all_TFs, all_DFs;
		all_TFs = new HashMap<String, Token>();
		all_DFs = new HashMap<String, Token>();
		analyzer.LoadStopwords("init_stop_words.txt");
//		analyzer2.LoadStopwords("init_stop_words.txt");
		analyzer.LoadDirectory("./Data/yelps/train", ".json");
		analyzer.LoadDirectory("./Data/yelps/test", ".json"); // in text categorizaiton, loading all json files
//		analyzer2.LoadDirectory("./Data/yelp/train", ".json");
//		
		all_DFs.putAll(analyzer.m_dfstats);
//		all_DFs.putAll(analyzer2.m_dfstats);
//		
//		
//		analyzer.LoadDirectory("./Data/yelp/test", ".json");
//		analyzer2.LoadDirectory("./Data/yelp/test", ".json");
//
		all_TFs.putAll(analyzer.m_stats);
//		all_TFs.putAll(analyzer2.m_stats);
//		
//		
		List<Map.Entry<String, Token>> all_tf_sorted = DocAnalyzer.SortHashMap(all_TFs); // Sort the tokens by DF
		DocAnalyzer.OutputWordCount(all_tf_sorted, "alltf.txt");
		List<Map.Entry<String, Token>> all_df_sorted = DocAnalyzer.SortHashMap(all_DFs); // Sort the tokens by DF
		DocAnalyzer.OutputWordCount(all_df_sorted, "N_allDF.txt"); 
		analyzer.BuildCVocabulary(all_df_sorted);//Build Controlled vocabulary
		DocAnalyzer.OutputWordList(analyzer.m_CtrlVocabulary, "N_CtrlVocabulary.txt");
		DocAnalyzer.OutputWordList(analyzer.m_stopwords, "Final_stop_words.txt");
		
		//***************************************************
		// Above are the preprocessing
//		DocAnalyzer SimiAnalyzer = new DocAnalyzer("./data/Model/en-token.bin", 1);
//		SimiAnalyzer.LoadVocabulary("N_CtrlVocabulary.txt");
//		SimiAnalyzer.LoadDirectory("./Data/yelp/test", ".json");
//		SimiAnalyzer.LoadQurey("./Data/samples/query.json");
//
//		HashMap<String, Double> DFs = SimiAnalyzer.LoadDFs("N_allDF.txt");
////		HashMap<String, Double> TFs = SimiAnalyzer.LoadTFs("alltf.txt");
//		SimiAnalyzer.analyzeDocumentVM(SimiAnalyzer.m_reviews);
//		SimiAnalyzer.analyzeDocumentVM(SimiAnalyzer.m_Qreviews);
//		int size = SimiAnalyzer.m_reviews.size();
//		for (int i = 0; i < size; i++)
//		{
//			SimiAnalyzer.TF_IDFCalc(SimiAnalyzer.m_reviews.get(i), DFs, 38688);
//		}
//		size = SimiAnalyzer.m_Qreviews.size();
//		for (int i = 0; i < size; i++)
//		{
//			SimiAnalyzer.TF_IDFCalc(SimiAnalyzer.m_Qreviews.get(i), DFs, 38688);
//		}
//		
//		SimiAnalyzer.CalcSimi();
		
//		//*******************************************************
		// MP2 starts here
//		analyzer.LoadLMDir("./data/yelp/train", ".json");
//		analyzer.createLanguageModel();
//		System.out.println("build unigram model complete...");
//		analyzer2.LoadLMDir("./data/yelp/train", ".json");
//		analyzer2.createLanguageModel();
//		System.out.println("build bigram model complete...");
//		
//		LanguageModel unigram = analyzer.m_langModel;
//		LanguageModel bigram = analyzer2.m_langModel;
//		bigram.setRefModel(unigram);
//		bigram.setUniKeySet(unigram.getModel().keySet());
//		
////		HashMap<String, Token> unimodel = analyzer.m_langModel.getModel();
//		List<Map.Entry<String, Double>> q1 = bigram.ProbQuery("good", "lin");
//		List<Map.Entry<String, Double>> q2 = bigram.ProbQuery("good", "abs");
//		for (int i = 0; i < 10; i++)
//		{
//			System.out.println(q1.get(i).getKey() + " : " + q1.get(i).getValue());
//			System.out.println(q2.get(i).getKey() + " : " + q2.get(i).getValue());
//			System.out.println("-------------------");
//		}
//		
//
//		List<String> uniSen, biSenLin, biSenAbs;
//		uniSen = new ArrayList<String>();
//		biSenLin = new ArrayList<String>();
//		biSenAbs = new ArrayList<String>();
//		for (int i = 0; i < 10; i++)
//		{
//			String temp;
//			System.out.println("Generating the " + i + "th sentence...");
//			
//			temp = unigram.genSentance(15, 1, "lin");
//			System.out.print(temp);
//			uniSen.add(temp);
//			
//			temp = bigram.genSentance(15, 2, "lin");
//			System.out.print(temp);
//			biSenLin.add(temp);
//			
//			temp = bigram.genSentance(15, 2, "abs");
//			System.out.print(temp);
//			biSenAbs.add(temp);
//		}
//		
//		OutputWordList(uniSen, "uniSentence.txt");
//		OutputWordList(biSenLin, "biSentenceLin.txt");
//		OutputWordList(biSenAbs, "biSentenceAbs.txt");
//		
//		
//		analyzer.clearReview();
//		analyzer.LoadLMDir("./data/yelp/test", ".json");
//		System.out.println("reviews in test set loaded.");
//		double pp1, pp2, pp3;
//		pp1 = analyzer.PPcalc(unigram, "lin");
//		System.out.println("pp1 : " + pp1);
//		pp2 = analyzer.PPcalc(bigram, "lin");
//		System.out.println("pp2 : " + pp2);
//		pp3 = analyzer.PPcalc(bigram, "abs");
//		System.out.println("pp3 : " + pp3);
//		
//		System.out.println("complete.");
		
		//*******************************************************
		// MP3 (text categorization) starts here
		
	}

}
