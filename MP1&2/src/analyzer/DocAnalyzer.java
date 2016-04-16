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
import java.util.Random;

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
	HashMap<List<Integer>, List<Post>> m_buckets; // the buckets for the random projection result in KNN
	ArrayList<HashMap<String, Double>> m_randvec;// the random vectors for the hash purpose in the bucket in KNN
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	HashMap<String, Token> m_stats;	
	HashMap<String, Token> m_dfstats; // the count for df

	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;
	
	//this structure is for language modeling
	LanguageModel m_langModel;
	// MP3, the positive and negative language model
	LanguageModel m_PoslangModel;
	LanguageModel m_NeglangModel;
	
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
	
	public static void OutputWordCount2(List<Map.Entry<String, Double>> entrylist, String filename) // out put word list of <string,double>
	{
		try
		{
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
			for (Entry<String, Double> entry : entrylist)
			{
				out.write(entry.getKey() + "," + entry.getValue() + "\r\n");
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
			System.out.println(filename + " saved!");
		  
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void OutputPRList(List<Map.Entry<Double, double[]>> PRs, String filename) // out put the multiple delta PR result
	{
		try
		{
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
			for (Map.Entry<Double, double[]> entry : PRs)
			{
				out.write(String.valueOf(entry.getKey()) + ',' + String.valueOf(entry.getValue()[0]) + ',' + String.valueOf(entry.getValue()[1]) + "\r\n");
//				
			}
			out.flush();
			out.close(); 
			System.out.println(filename + " saved!");
		  
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
								newt.setPosNeg(review.getRating());
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
	
	
	public void analyzeCtrlDocuments() // analyze the tf and df within all selected reviews, stored in m_stats and m_dfstats
	{		
		HashSet<String> dfcheck;
		dfcheck = new HashSet<String>();
		m_stats.clear();
		m_dfstats.clear();
		for(int i=0; i<m_reviews.size(); i++) 
		{
			Post review = m_reviews.get(i);
			String[] tokens = review.getTokens();


			dfcheck.clear();
			
			for (String tok : tokens)
			{
				tok = SnowballStemming(Normalization(tok));
				if (m_CtrlVocabulary.contains(tok))
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
						newt.setPosNeg(review.getRating());
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
	
	public void createPosNegLanguageModel(List<Post> reviews) // create 2 language models based on Pos and Neg reveiws
	{
		m_PoslangModel = new LanguageModel(m_N);
		m_NeglangModel = new LanguageModel(m_N);
		int count = 0;
		if (m_N == 1)// unigram model, calculate all terms
		{
			for(Post review : reviews) 
			{	
				System.out.println("Language model : " + count);
				count += 1;
				String[] tokens = review.getTokens();
				if (review.getRating() >= 4) // Pos
				{
					m_PoslangModel.IncreDocNum(1);
					for (String tok : tokens)
					{
						tok = SnowballStemming(Normalization(tok));
						if (tok.length() > 0)
						{
							m_PoslangModel.increCount(); 
							m_PoslangModel.addToken(tok); // add token to the m_model
						}	
					}
				}
				else // Neg
				{
					m_NeglangModel.IncreDocNum(1);
					for (String tok : tokens)
					{
						tok = SnowballStemming(Normalization(tok));
						if (tok.length() > 0)
						{
							m_NeglangModel.increCount(); 
							m_NeglangModel.addToken(tok); // add token to the m_model
						}	
					}
				}
				
				/**
				 * HINT: essentially you will perform very similar operations as what you have done in analyzeDocument() 
				 * Now you should properly update the counts in LanguageModel structure such that we can perform maximum likelihood estimation on it
				 */
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
	
	public static List<Map.Entry<String, Double>> SortHashMap_double(HashMap<String, Double> wordlist)
	{
		List<Map.Entry<String, Double>> entrylist = new ArrayList<Map.Entry<String, Double>>(wordlist.entrySet());

		Collections.sort(entrylist, new Comparator<Map.Entry<String, Double>>()
			{public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2)
				{
					if (o2.getValue() > o1.getValue())
						return 1;
					else if(o2.getValue() == o1.getValue())
						return 0;
					else
						return -1;
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
		m_CtrlVocabulary.clear();
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
	
	public void BuildCVocabulary2(List<Map.Entry<String, Token>> all_df_sorted) // the control vocabulary without remove the high frequency stop words for MP3
	{
		m_CtrlVocabulary.clear();
		System.out.println("Building CV...");
		for (Map.Entry<String, Token> entry : all_df_sorted)
		{
				double df = entry.getValue().getValue();
				if (df < 50 || m_stopwords.contains(entry.getKey()))
					break;
				m_CtrlVocabulary.add(entry.getKey());
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
	
//	public void TF_IDFCalc2(Post review, HashMap<String, Double> DFs, int DocNum) // calc TFIDF for the KNN vector
//	{
//		List<String> CtrlVoc = new ArrayList<String>(m_CtrlVocabulary);
//		int size = CtrlVoc.size();
//		for (int i = 0; i < size; i++)
//		{
//			double tf = review.getTvecValue(i);
//			if (tf > 0)
//			{
//				tf = 1 + Math.log10(tf);
//				String tk = CtrlVoc.get(i);
//				System.out.println(tk);
////				Token testtoken = DF.get(tk);
//				double idf = DFs.get(CtrlVoc.get(i));
//				idf = 1 + Math.log10((double)DocNum/idf);
//				review.setTvecValue(i, tf * idf);
//			}
//		}
//	}
	
	
	
	
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
	
	public void Calc_IG_Chi(HashMap<String, Double> IG, HashMap<String, Double> ChiSq)// calculate the information gain of each term and store in IG
	{
		double rating;
		double pos_num = 0;
		double neg_num = 0;
		HashMap<String, Double> termCount = new HashMap<String, Double>(); // the count of reviews that a term appears
		HashMap<String, Double> termPosCount = new HashMap<String, Double>(); // the count of Pos reviews that a term appears
		HashMap<String, Double> termNegCount = new HashMap<String, Double>(); // the count of Neg reviews that a term appears
		for (int i = 0; i < m_reviews.size(); i++)
		{
			rating = m_reviews.get(i).getRating();
			boolean pos = false;
			if (rating >= 4)
			{
				pos_num += 1;
				pos = true;
			}
			else
				neg_num += 1;
			
			HashSet<String> terms = new HashSet<String>(); //for each round, gather the existence of a term
			String[] tokens = m_reviews.get(i).getTokens();
			for (String tok : tokens)
			{
				tok = SnowballStemming(Normalization(tok));
				if (m_CtrlVocabulary.contains(tok))
					terms.add(tok);
			}
			
			String[] termsarray = terms.toArray(new String[0]);
			for (String thisterm : termsarray)
			{
				if (termCount.containsKey(thisterm))//get the term count on the bases of all reviews
				{
					Double temp = termCount.get(thisterm).doubleValue();
					temp += 1;
					termCount.put(thisterm, temp);
				}
				else
					termCount.put(thisterm, 1.0);
				
				if (pos)//get the term count on the bases of Pos reviews
				{
					if (termPosCount.containsKey(thisterm))
					{
						Double temp = termPosCount.get(thisterm).doubleValue();
						temp += 1;
						termPosCount.put(thisterm, temp);
					}
					else
						termPosCount.put(thisterm, 1.0);
				}
				else//get the term count on the bases of Neg reviews
				{
					if (termNegCount.containsKey(thisterm))
					{
						Double temp = termNegCount.get(thisterm).doubleValue();
						temp += 1;
						termNegCount.put(thisterm, temp);
					}
					else
						termNegCount.put(thisterm, 1.0);
				}
			}
		}
		
		double review_num = pos_num + neg_num;
		double p_1 = pos_num / review_num;//the p(y) value
		double p_0 = neg_num / review_num;
		
		double term1 = 0.0;// these 3 terms are the terms in the equation of IG in the homework page
		double term2, term3;
		double A,B,C,D;// these 4 terms are for the Chi sq computation.
		if (p_1 != 0.0)
			term1 -= p_1 * Math.log10(p_1);
		if (p_0 != 0.0)
			term1 -= p_0 * Math.log10(p_0);
		
		for (Map.Entry<String, Double> entry : termCount.entrySet())
		{
			term2 = 0.0;
			term3 = 0.0;
			A = 0.0;
			B = 0.0;
			C = 0.0;
			D = 0.0;
			double p_t = entry.getValue() / review_num;
			double poscount = 0;// init the count of this term entry within pos and neg reviews
			double negcount = 0;
			String word = entry.getKey();

			if (termPosCount.containsKey(word))// get the prob of pos and neg when observed the term
			{
				poscount = termPosCount.get(word).doubleValue();
				A = poscount;
			}
			B = pos_num - poscount;
			
				
			if (termNegCount.containsKey(word))
			{
				negcount = termNegCount.get(word).doubleValue();
				C = negcount;
			}
			D = neg_num - negcount;
			
			double p_y1_t = poscount / (poscount + negcount);
			// get the prob of pos and neg when not observe the term
			double p_y1_nott = (pos_num - poscount) / ((1 - p_t) * review_num);
			
			
			if (p_y1_t != 0.0 && p_y1_t != 1.0)
				term2 = p_t * ( Math.log10(p_y1_t) * p_y1_t + Math.log10(1 - p_y1_t) * (1 - p_y1_t));
			if (p_y1_nott != 0.0 && p_y1_nott != 1.0)
				term3 = (1 - p_t)  * ( Math.log10(p_y1_nott) * p_y1_nott + Math.log10(1 - p_y1_nott) * (1 - p_y1_nott));
			double current_result = term1 + term2 + term3;
			IG.put(word, current_result); // the information gain
			current_result = ((A + B + C + D) * (A * D - B * C) * (A * D - B * C)) / ((A + C) * (B + D) * (A + B) * (C + D));
			ChiSq.put(word, current_result);
			
		}

	}
	
	public List<Map.Entry<String, Double>> ChiFilter(List<Map.Entry<String, Double>> ChiSqsorted, double Value_threshold, int Count_threshold)
	{
		List<Map.Entry<String, Double>> temp = new ArrayList<Map.Entry<String, Double>>();
		int count = 0;
		for (Map.Entry<String, Double> entry : ChiSqsorted)
		{
			if (count <= Count_threshold && entry.getValue() >= Value_threshold)
			{
				temp.add(entry);
			}
			count += 1;
		}
		return temp;
	}
	
	public List<Map.Entry<String, Double>> IGFilter(List<Map.Entry<String, Double>> IGSqsorted, int Count_threshold)
	{
		List<Map.Entry<String, Double>> temp = new ArrayList<Map.Entry<String, Double>>();
		int count = 0;
		for (Map.Entry<String, Double> entry : IGSqsorted)
		{
			if (count <= Count_threshold )
			{
				temp.add(entry);
			}
			count += 1;
		}
		return temp;
	}
	
	public void BuildSparseVec()//build the sparse vecter representation for all reviews based on the ctrl vocabulary
	{
		ArrayList<Post> temp = new ArrayList<Post>();
		for (int i = 0; i < m_reviews.size(); i++)
		{
			m_reviews.get(i).initSparseVec();
			String[] tokens = m_reviews.get(i).getTokens();
			for (String tok : tokens)
			{
				tok = SnowballStemming(Normalization(tok));
				if (m_CtrlVocabulary.contains(tok))	
				{
					m_reviews.get(i).AddVct(tok);
				}
					
			}
			if (m_reviews.get(i).getVct().size() > 5)//eliminate the reviews with less than 5 non-empty entry in the vec representation.
				temp.add(m_reviews.get(i));
		}
		
		
		for (int i = 0; i < m_Qreviews.size(); i++)//process query reviews
		{
			m_Qreviews.get(i).initSparseVec();
			String[] tokens = m_Qreviews.get(i).getTokens();
			for (String tok : tokens)
			{
				tok = SnowballStemming(Normalization(tok));
				if (m_CtrlVocabulary.contains(tok))	
					m_Qreviews.get(i).AddVct(tok);
			}
		}
		m_reviews = temp;
	}
	
	public void BuildSparseVec2()//build the sparse vecter representation for KNN use, all the value are transformed to TF-IDF double value
	{
//		ArrayList<Post> temp = new ArrayList<Post>();
		for (int i = 0; i < m_reviews.size(); i++)
		{
			HashMap<String, Double> tempvec = new HashMap<String, Double>(m_reviews.get(i).getVct());
			for (Map.Entry<String, Double> entry : tempvec.entrySet())
			{
				String term = entry.getKey();
				double tf = entry.getValue();
				tf = 1 + Math.log10(tf);
				
//				String tk = CtrlVoc.get(i);
//				System.out.println(tk);
//				Token testtoken = DF.get(tk);
				double idf = m_dfstats.get(term).getValue();
				idf = 1 + Math.log10((double)DocNum/idf);
				m_reviews.get(i).SetVct(term, tf * idf);
//				review.setTvecValue(i, tf * idf);
			}
		}
		
		for (int i = 0; i < m_Qreviews.size(); i++)//process query reviews
		{
			HashMap<String, Double> tempvec = new HashMap<String, Double>(m_Qreviews.get(i).getVct());
			for (Map.Entry<String, Double> entry : tempvec.entrySet())
			{
				String term = entry.getKey();
				double tf = entry.getValue();
				tf = 1 + Math.log10(tf);
				
//				String tk = CtrlVoc.get(i);
//				System.out.println(tk);
//				Token testtoken = DF.get(tk);
				double idf = m_dfstats.get(term).getValue();
				idf = 1 + Math.log10((double)DocNum/idf);
				m_Qreviews.get(i).SetVct(term, tf * idf);
//				review.setTvecValue(i, tf * idf);
			}
		}
		
		
	}
	
	public double SparseVecDot(HashMap<String, Double> v1, HashMap<String, Double> v2)
	{
		double result = 0.0;
		for(Map.Entry<String, Double> entry : v1.entrySet())
		{
			if (v2.containsKey(entry.getKey()))
//				result += entry.getValue() * v2.get(entry.getKey()).;
				result += entry.getValue() * v2.get(entry.getKey()).doubleValue();
		}
		return result;
	}
	
	public List<Map.Entry<String, Double>> NBPosNegProb() // for MP3 task2.1 find the log probs.
	{
		HashMap<String, Double> prob = new HashMap<String, Double>();
		double posP, negP;
		m_PoslangModel.setDelta(0.1);
		m_NeglangModel.setDelta(0.1);
		int count = 0;
		for (int i = 0; i < m_reviews.size(); i++)
		{
			System.out.println(" PosNeg calc : " + count);
			count += 1;
			String[] tokens = m_reviews.get(i).getTokens();
			for (String tok : tokens)
			{
				tok = SnowballStemming(Normalization(tok));
				if(tok.length() > 0)
				{
					posP = m_PoslangModel.calcAddSmoothedProb(tok);
					negP = m_NeglangModel.calcAddSmoothedProb(tok);
					prob.put(tok, Math.log(posP / negP));
				}
				
			}
		}
//		List<Map.Entry<String, Double>> sorted = new ArrayList<Map.Entry<String, Double>>();
		return DocAnalyzer.SortHashMap_double(prob);
	}
	
	public double NaiveBayesScore(Post review)
	{
		double result = Math.log((m_PoslangModel.getDocNum()) / (m_NeglangModel.getDocNum()));
		String[] tokens = review.getTokens();
		for (String tok : tokens)
		{
			tok = SnowballStemming(Normalization(tok));
			if(tok.length() > 0)
				result += Math.log((m_PoslangModel.calcAddSmoothedProb(tok)) / (m_NeglangModel.calcAddSmoothedProb(tok)));
		}
		return result;
	}
//	HashMap<Integer, Double> score
	public List<Map.Entry<String, Double>> NaiveBayesClassifyer()
	{
		HashMap<String, Double> scores = new HashMap<String, Double>();
		for (int i = 0; i < m_reviews.size(); i++)
			scores.put(m_reviews.get(i).getID(), NaiveBayesScore(m_reviews.get(i)));
		return SortHashMap_double(scores);
	}
	
	public HashMap<String, Integer> getDocNegPos() // get the reviews Pos or Neg by there reviewID(String)
	{
		HashMap<String, Integer> result = new HashMap<String, Integer>();
		for (int i = 0; i < m_reviews.size(); i++)
		{
			if (m_reviews.get(i).getRating() >= 4)
				result.put(m_reviews.get(i).getID(), 1);
			else
				result.put(m_reviews.get(i).getID(), -1);
		}
		return result;
	}
	
	public double[] CalcPR(List<Map.Entry<String, Double>> predict, HashMap<String, Integer> truth)// precision and recall score
	{
		double[] pr = {0.0, 0.0};
		int TP,FP,TN,FN;
		String id;
		TP = 0;
		FP = 0;
		TN = 0;
		FN = 0;
		for(int i = 0; i < predict.size(); i++)
		{
			id = predict.get(i).getKey();
			if (predict.get(i).getValue() > 0) // predict as a positive
			{
				if (truth.get(id).intValue() > 0)
					TP += 1;
				else
					FP += 1;
			}
			else
			{
				if (truth.get(id).intValue() > 0)
					FN += 1;
				else
					TN += 1;
			}
		}
		pr[0] = (double)TP / (TP + FP);
		pr[1] = (double)TP / (TP + FN);
		return pr;
	}
	
	public List<Map.Entry<Double, double[]>> multipleDeltaPR() //test of the multiple delta values in the NB classification
	{
		HashMap<String, Integer> classtruth = getDocNegPos();
		List<Map.Entry<Double, double[]>> PRs = new ArrayList<Map.Entry<Double, double[]>>();
		for (double i = 1; i < 20; i = i + 2)
		{
			double delta = i / 10;
			m_NeglangModel.setDelta(delta);
			m_PoslangModel.setDelta(delta);
			List<Map.Entry<String, Double>> NBresult = NaiveBayesClassifyer();
			double[] pr = CalcPR(NBresult, classtruth); 
			PRs.add(new AbstractMap.SimpleEntry<Double, double[]>(delta, pr.clone()));
		}
		return PRs;
	}
	 
	
	public void BuildRandProjBucket(int l)// perform random projection and build buckets for all reviews
	{
		Random rnd = new Random(42);
		m_buckets = new HashMap<List<Integer>, List<Post>>();
		m_randvec = new ArrayList<HashMap<String, Double>>(); // random vector
		for (int i = 0; i < l; i ++)
		{
			HashMap<String, Double> tempvec = new HashMap<String, Double>();
			for(String term : m_CtrlVocabulary)
			{
				tempvec.put(term, (rnd.nextDouble() * 2) - 1);
			}
			m_randvec.add(tempvec);
		}
		for (int i = 0; i < m_reviews.size(); i++)
		{
			List<Integer> temphash = new ArrayList<Integer>();
			for (int j = 0; j < m_randvec.size(); j++)
			{
				if(SparseVecDot(m_reviews.get(i).getVct(), m_randvec.get(j)) > 0)
					temphash.add(1);
				else
					temphash.add(0);
			}
			
			if (m_buckets.containsKey(temphash))
				m_buckets.get(temphash).add(m_reviews.get(i));
			else
			{
				List<Post> tempPostlist = new ArrayList<Post>();
				tempPostlist.add(m_reviews.get(i));
				m_buckets.put(temphash, tempPostlist);
			}
		}
	}
	
	
	public void TopResults(int k, HashMap<String, Double> results, double sim, String ID)
	{
		if (results.size() < k)
			results.put(ID, sim);
		else
		{
			double min_sim = 10000000;
			String min_ID = ""; 
			for (Map.Entry<String, Double> entry : results.entrySet())
			{
				if (entry.getValue() < min_sim)
				{
					min_sim = entry.getValue();
					min_ID = entry.getKey();
				}
			}
			if (sim > min_sim)
			{
				results.remove(min_ID);
				results.put(ID, sim);
			}
		}
	}
	
//	
	public List<String> KNNbruteforce(int k, Post target)// using brute force to retrieve the k nearest reviews ID, input a target Post review, return a list of similar reveiw IDs
	{
//		HashMap<String, HashMap<String, Double>> results = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> results = new HashMap<String, Double>();
		for (int i = 0; i < m_reviews.size(); i++)
		{
			double similarity = SparseVecDot(m_reviews.get(i).getVct(), target.getVct());
			TopResults(k, results, similarity, target.getID());
		}
		List<String> top = new ArrayList<String>();
		for (Map.Entry<String, Double> entry : results.entrySet())
			top.add(entry.getKey());
		return top;
	}
	
	
	public List<String> KNNRandProj(int k, Post target)// using random projection to retrieve the k nearest reviews ID
	{
		HashMap<String, Double> results = new HashMap<String, Double>();
		List<Integer> temphash = new ArrayList<Integer>();
		for (int j = 0; j < m_randvec.size(); j++)
		{
			if(SparseVecDot(target.getVct(), m_randvec.get(j)) > 0)
				temphash.add(1);
			else
				temphash.add(0);
		}
		
		if (m_buckets.containsKey(temphash))
		{
			List<Post> reviews = m_buckets.get(temphash);
			for (int i = 0; i < reviews.size(); i++)
			{
				double similarity = SparseVecDot(reviews.get(i).getVct(), target.getVct());
				TopResults(k, results, similarity, target.getID());
			}
			List<String> top = new ArrayList<String>();
			for (Map.Entry<String, Double> entry : results.entrySet())
				top.add(entry.getKey());
			return top;
		}
		else
			return KNNbruteforce(k, target);
	}
	
//	public boolean ListEqual(List<Integer> l1, List<Integer> l2) // compare if two list are equal
//	{
//		if (l1.size() != l2.size())
//			return false;
//		for (int i = 0; i < l1.size(); i++)
//		{
//			if (l1.get(i) != l2.get(i))
//				return false;
//		}
//		return true;
//	}
//	
//	
	
	public double[] CrossValidation(int fold) // 10 fold CV for NB and KNN
	{
		double[] results = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; // first 3 are NBs P,R,F average, Last 3 are KNN's P,R,F average
		Collections.shuffle(m_reviews);
		int offset = m_reviews.size() / fold;
		for (int i = 0; i < fold; i++)
		{
			List<Post> train = new ArrayList<Post>();
			List<Post> test = new ArrayList<Post>();
			if (i == fold - 1) // the last fold
			{
				for (int j = 0; j < m_reviews.size(); j++)
				{
					if (j >= i * offset)
						test.add(m_reviews.get(j));
					else
						train.add(m_reviews.get(j));
				}
			}
			else
			{
				
				for (int j = 0; j < m_reviews.size(); j++)
				{
					if (j >= i * offset && j < (i + 1) * offset)
						test.add(m_reviews.get(j));
					else
						train.add(m_reviews.get(j));
				}
			}
			
			// perform NB and KNN here
			
			
		}
		
		return results;
	}
	
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException 
	{	
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
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
//		List<Map.Entry<String, Token>> all_tf_sorted = DocAnalyzer.SortHashMap(all_TFs); // Sort the tokens by DF
//		DocAnalyzer.OutputWordCount(all_tf_sorted, "allTF.txt");
//		List<Map.Entry<String, Token>> all_df_sorted = DocAnalyzer.SortHashMap(all_DFs); // Sort the tokens by DF
//		DocAnalyzer.OutputWordCount(all_df_sorted, "allDF.txt"); 
		
//		analyzer.BuildCVocabulary(all_df_sorted);//Build Controlled vocabulary
//		DocAnalyzer.OutputWordList(analyzer.m_CtrlVocabulary, "CtrlVocabulary.txt");
//		DocAnalyzer.OutputWordList(analyzer.m_stopwords, "Final_stop_words.txt");
		
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
		
//		analyzer.BuildCVocabulary2(all_df_sorted);//Build Controlled vocabulary
//		
//		// first, get the information gain
//		HashMap<String, Double> IG = new HashMap<String, Double>();
//		HashMap<String, Double> ChiSq = new HashMap<String, Double>();
//		analyzer.Calc_IG_Chi(IG, ChiSq);
//		List<Map.Entry<String, Double>> IG_sorted = DocAnalyzer.SortHashMap_double(IG); // Sort the tokens by DF
////		DocAnalyzer.OutputWordCount(IG_sorted, "SortedIG.txt");
//		List<Map.Entry<String, Double>> ChiSq_sorted = DocAnalyzer.SortHashMap_double(ChiSq); // Sort the tokens by DF
////		DocAnalyzer.OutputWordCount(all_df_sorted, "allDF.txt"); 
//		IG_sorted = analyzer.IGFilter(IG_sorted, 5000); // take first 5000
//		ChiSq_sorted = analyzer.ChiFilter(ChiSq_sorted, 3.841, 5000); //take first 5000 and threshold 3.841
//		HashSet<String> mergedCV = new HashSet<String>();
//		for (Map.Entry<String, Double> entry : IG_sorted)
//			mergedCV.add(entry.getKey());
//		for (Map.Entry<String, Double> entry : ChiSq_sorted)
//			mergedCV.add(entry.getKey());
//		analyzer.m_CtrlVocabulary = mergedCV;
//		DocAnalyzer.OutputWordList(analyzer.m_CtrlVocabulary, "CtrlVocabulary.txt");
//		DocAnalyzer.OutputWordCount2(IG_sorted, "IGsort.txt");
//		DocAnalyzer.OutputWordCount2(ChiSq_sorted, "ChiSqsort.txt");
		
		
		// feature selection complete
		analyzer.LoadVocabulary("CtrlVocabulary.txt");
		analyzer.LoadQurey("./Data/samples/query.json");
		System.out.println("Building Sparse Vectors...");
		analyzer.BuildSparseVec();
		
//		System.out.println("Creating language Models...");
//		analyzer.createPosNegLanguageModel(m_reviews);
//		
//		System.out.println("Calculating neg and pos probs...");
//		List<Map.Entry<String, Double>> logprobs = analyzer.NBPosNegProb();
//		DocAnalyzer.OutputWordCount2(logprobs, "probs.txt");
//		
//		
////		DocAnalyzer.OutputWordCount2(result, "NBclassResult.txt");
//		System.out.println("NB classfying...");
//		
//		List<Map.Entry<Double, double[]>> PRs = analyzer.multipleDeltaPR();
//		DocAnalyzer.OutputPRList(PRs, "PRresults.csv");
		
		//Task 3.Random projection for KNN
		// get all vector representation of a review. calc tfidf for all words
		
		System.out.println("building tf idf Sparse Vectors...");
		analyzer.BuildSparseVec2();
		System.out.println("building random projection buckets...");
		analyzer.BuildRandProjBucket(5);
		//do the Knn here
		
		
		
		//Task 4.Cross Validation
		
		double[] CVresults = analyzer.CrossValidation(10);
		
		
		
		
		System.out.println("Done!");
	}

}
