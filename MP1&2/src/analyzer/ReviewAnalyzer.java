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
 * @author Jinlong
 * BM25F implementation for the Text Mining course project  
 */
public class ReviewAnalyzer 
{
//	int m_N;
	double DocNum;//the total number of App description-review instances

	HashSet<String> m_stopwords;
	HashSet<String> m_CtrlVocabulary;
//	int m_train_num;
//	int m_test_num;
	ArrayList<Post> m_discriptions;// the discription part of the app
	ArrayList<Post> m_reviews; // the review part of the app
	ArrayList<Post> m_queries;// query
	
	HashMap<String, Token> m_count; // token df counts

	
//	HashMap<String, Integer> m_ID_Index_map; // hash map mapping review id and the index in m_reviews
//	HashMap<String, Integer> m_classtruth;// the review true pos and neg
	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;
	
	//this structure is for language modeling
//	LanguageModel m_langModel;
//	// MP3, the positive and negative language model
//	LanguageModel m_PoslangModel;
//	LanguageModel m_NeglangModel;
	
	
	
	public ReviewAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {

//		m_N = N;
		m_discriptions = new ArrayList<Post>();
		m_reviews = new ArrayList<Post>();
		m_queries = new ArrayList<Post>();
//		m_stats = new HashMap<String, Token>();
//		m_dfstats = new HashMap<String, Token>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<String>();
		m_CtrlVocabulary = new HashSet<String>();
	}
	
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
	
	String[] Tokenize(String text) {
		return m_tokenizer.tokenize(text);
	}
	
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
	
	public String SnowballStemming(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	public void analyzeDocument(JSONObject json) {		
		try {
			JSONArray jarray = json.getJSONArray("");
			int a = 0;
			int b = a;
//			HashSet<String> dfcheck;
//			dfcheck = new HashSet<String>();
//			for(int i=0; i<jarray.length(); i++) {
//				Post review = new Post(jarray.getJSONObject(i));
//				String[] tokens = Tokenize(review.getContent());
//				review.setTokens(tokens);
//
//				dfcheck.clear();
				
//				for (String tok : tokens)
//				{
//					if (m_stopwords.contains(tok))
//						continue;
//					tok = SnowballStemming(Normalization(tok));
//					if (tok.length() != 0)
//					{
//						dfcheck.add(tok);
//						if (m_stats.containsKey(tok))
//						{
//							Token temp = m_stats.get(tok);
//							temp.setValue(temp.getValue() + 1); // increase count by 1
//						}
//						else
//						{
//							Token newt = new Token(m_stats.size(), tok);
//							newt.setValue(1); 
//							newt.setPosNeg(review.getRating());
//							m_stats.put(tok, newt);
//						}
//					}
//					
//				}
//				
//				for (String tok : dfcheck)
//				{
//					if (m_dfstats.containsKey(tok))
//					{
//						Token temp = m_dfstats.get(tok);
//						temp.setValue(temp.getValue() + 1); // increase count by 1
//					}
//					else
//					{
//						Token newt = new Token(m_dfstats.size(), tok);
//						newt.setValue(1); 
//						m_dfstats.put(tok, newt);
//					}
//				}
				
//				m_reviews.add(review);
//			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder);
		int size = m_reviews.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix))
			{
				analyzeDocument(LoadJson(f.getAbsolutePath()));
//				System.out.println(m_stats.size());
			}
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
			
			break;//debug
		}
//		size = m_reviews.size() - size;
//		if (m_train_num < 0)
//		{
//			m_train_num = size;
//		}
//		else if (m_test_num < 0)
//		{
//			m_test_num = size;
//		}
		System.out.println("Loading " + size + " review documents from " + folder);
	}
	
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException 
	{	
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
		ReviewAnalyzer analyzer = new ReviewAnalyzer("./data/Model/en-token.bin", 1);
//		DocAnalyzer analyzer2 = new DocAnalyzer("./data/Model/en-token.bin", 2);

		//code for demonstrating tokenization and stemming
//		analyzer.TokenizerDemon("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this.");
		// analyzer.TokenizerDemon("this is just a test sentence for the function");
			
		//entry point to deal with a collection of documents
//		HashMap<String, Token> all_TFs, all_DFs;
//		all_TFs = new HashMap<String, Token>();
//		all_DFs = new HashMap<String, Token>();
//		analyzer.LoadStopwords("init_stop_words.txt");
//		analyzer2.LoadStopwords("init_stop_words.txt");
		analyzer.LoadDirectory("./data/AppReview/childDatas", ".json");
		analyzer.LoadDirectory("./data/AppReview/parentDatas", ".json"); // in text categorizaiton, loading all json files
//		analyzer2.LoadDirectory("./Data/yelp/train", ".json");
//		
//		all_DFs.putAll(analyzer.m_dfstats);
////		all_DFs.putAll(analyzer2.m_dfstats);
////		
////		
////		analyzer.LoadDirectory("./Data/yelp/test", ".json");
////		analyzer2.LoadDirectory("./Data/yelp/test", ".json");
////
//		all_TFs.putAll(analyzer.m_stats);
////		all_TFs.putAll(analyzer2.m_stats);
////		
//		List<Map.Entry<String, Token>> all_tf_sorted = DocAnalyzer.SortHashMap(all_TFs); // Sort the tokens by DF
//		DocAnalyzer.OutputWordCount(all_tf_sorted, "allTF.txt");
//		List<Map.Entry<String, Token>> all_df_sorted = DocAnalyzer.SortHashMap(all_DFs); // Sort the tokens by DF
//		DocAnalyzer.OutputWordCount(all_df_sorted, "allDF.txt"); 
//		
//		analyzer.BuildCVocabulary(all_df_sorted);//Build Controlled vocabulary
//		DocAnalyzer.OutputWordList(analyzer.m_CtrlVocabulary, "CtrlVocabulary.txt");
//		DocAnalyzer.OutputWordList(analyzer.m_stopwords, "Final_stop_words.txt");
//		
//		//***************************************************
//		// Above are the preprocessing
////		DocAnalyzer SimiAnalyzer = new DocAnalyzer("./data/Model/en-token.bin", 1);
////		SimiAnalyzer.LoadVocabulary("N_CtrlVocabulary.txt");
////		SimiAnalyzer.LoadDirectory("./Data/yelp/test", ".json");
////		SimiAnalyzer.LoadQurey("./Data/samples/query.json");
////
////		HashMap<String, Double> DFs = SimiAnalyzer.LoadDFs("N_allDF.txt");
//////		HashMap<String, Double> TFs = SimiAnalyzer.LoadTFs("alltf.txt");
////		SimiAnalyzer.analyzeDocumentVM(SimiAnalyzer.m_reviews);
////		SimiAnalyzer.analyzeDocumentVM(SimiAnalyzer.m_Qreviews);
////		int size = SimiAnalyzer.m_reviews.size();
////		for (int i = 0; i < size; i++)
////		{
////			SimiAnalyzer.TF_IDFCalc(SimiAnalyzer.m_reviews.get(i), DFs, 38688);
////		}
////		size = SimiAnalyzer.m_Qreviews.size();
////		for (int i = 0; i < size; i++)
////		{
////			SimiAnalyzer.TF_IDFCalc(SimiAnalyzer.m_Qreviews.get(i), DFs, 38688);
////		}
////		
////		SimiAnalyzer.CalcSimi();
//		
////		//*******************************************************
//		// MP2 starts here
////		analyzer.LoadLMDir("./data/yelp/train", ".json");
////		analyzer.createLanguageModel();
////		System.out.println("build unigram model complete...");
////		analyzer2.LoadLMDir("./data/yelp/train", ".json");
////		analyzer2.createLanguageModel();
////		System.out.println("build bigram model complete...");
////		
////		LanguageModel unigram = analyzer.m_langModel;
////		LanguageModel bigram = analyzer2.m_langModel;
////		bigram.setRefModel(unigram);
////		bigram.setUniKeySet(unigram.getModel().keySet());
////		
//////		HashMap<String, Token> unimodel = analyzer.m_langModel.getModel();
////		List<Map.Entry<String, Double>> q1 = bigram.ProbQuery("good", "lin");
////		List<Map.Entry<String, Double>> q2 = bigram.ProbQuery("good", "abs");
////		for (int i = 0; i < 10; i++)
////		{
////			System.out.println(q1.get(i).getKey() + " : " + q1.get(i).getValue());
////			System.out.println(q2.get(i).getKey() + " : " + q2.get(i).getValue());
////			System.out.println("-------------------");
////		}
////		
////
////		List<String> uniSen, biSenLin, biSenAbs;
////		uniSen = new ArrayList<String>();
////		biSenLin = new ArrayList<String>();
////		biSenAbs = new ArrayList<String>();
////		for (int i = 0; i < 10; i++)
////		{
////			String temp;
////			System.out.println("Generating the " + i + "th sentence...");
////			
////			temp = unigram.genSentance(15, 1, "lin");
////			System.out.print(temp);
////			uniSen.add(temp);
////			
////			temp = bigram.genSentance(15, 2, "lin");
////			System.out.print(temp);
////			biSenLin.add(temp);
////			
////			temp = bigram.genSentance(15, 2, "abs");
////			System.out.print(temp);
////			biSenAbs.add(temp);
////		}
////		
////		OutputWordList(uniSen, "uniSentence.txt");
////		OutputWordList(biSenLin, "biSentenceLin.txt");
////		OutputWordList(biSenAbs, "biSentenceAbs.txt");
////		
////		
////		analyzer.clearReview();
////		analyzer.LoadLMDir("./data/yelp/test", ".json");
////		System.out.println("reviews in test set loaded.");
////		double pp1, pp2, pp3;
////		pp1 = analyzer.PPcalc(unigram, "lin");
////		System.out.println("pp1 : " + pp1);
////		pp2 = analyzer.PPcalc(bigram, "lin");
////		System.out.println("pp2 : " + pp2);
////		pp3 = analyzer.PPcalc(bigram, "abs");
////		System.out.println("pp3 : " + pp3);
////		
////		System.out.println("complete.");

		
		System.out.println("Done!");
	}
	
}
