/**
 * 
 */
package structures;

import java.util.HashMap;
import java.util.Map;

import json.JSONException;
import json.JSONObject;

/**
 * @author hongning
 * @version 0.1
 * @category data structure
 * data structure for a Yelp review document
 * You can create some necessary data structure here to store the processed text content, e.g., bag-of-word representation
 */
public class Post {
	//unique review ID from Yelp
	String m_ID;		
	public void setID(String ID) {
		m_ID = ID;
	}
	
	double[] TokenVec;// the vector of Tokens count(the word vector), index follows the CtrlVocabulary 
	
	public double[] getTVec()
	{
		return TokenVec;
	}
	
	public void setTVec(double[] TV)
	{
		TokenVec = TV;
	}
	
	public void initTVec(int size)
	{
		TokenVec = new double[size];
	}
	
	public void setTvecValue(int index, double value)
	{
		TokenVec[index] = value;
	}
	
	public double getTvecValue(int index)
	{
		return TokenVec[index];
	}
	
	public String getID() {
		return m_ID;
	}

	//author's displayed name
	String m_author;	
	public String getAuthor() {
		return m_author;
	}

	public void setAuthor(String author) {
		this.m_author = author;
	}
	
	//author's location
	String m_location;
	public String getLocation() {
		return m_location;
	}

	public void setLocation(String location) {
		this.m_location = location;
	}

	//review text content
	String m_content;
	public String getContent() {
		return m_content;
	}

	public void setContent(String content) {
		if (!content.isEmpty())
			this.m_content = content;
	}
	
	public boolean isEmpty() {
		return m_content==null || m_content.isEmpty();
	}
	
	String m_title; // the apps title	
	public String getTitle() {
		return m_title;
	}

	public void setTitle(String title) {
		this.m_title = title;
	}

	//timestamp of the post
	String m_date;
	public String getDate() {
		return m_date;
	}

	public void setDate(String date) {
		this.m_date = date;
	}
	
	//overall rating to the business in this review
	double m_rating;
	public double getRating() {
		return m_rating;
	}

	public void setRating(double rating) {
		this.m_rating = rating;
	}

	public Post(String ID) {
		m_ID = ID;
		initSparseVec();
		m_results = new HashMap<String, Double>();
	}
	
	String[] m_tokens; // we will store the tokens 
	public String[] getTokens() {
		return m_tokens;
	}
	
	public void setTokens(String[] tokens) {
		m_tokens = tokens;
	}
	
	HashMap<String, Double> m_results; // for query use :the app name and its score
	public void Addresult(String appname, double score, int size) //maintain the top "size" number of results
	{
		if (m_results.size() < size)
			m_results.put(appname, score);
		else
		{
			double min_score = 10000000;
			String min_appname = ""; 
			for (Map.Entry<String, Double> entry : m_results.entrySet())
			{
				if (entry.getValue() < min_score)
				{
					min_score = entry.getValue();
					min_appname = entry.getKey();
				}
			}
			if (score > min_score)
			{
				m_results.remove(min_appname);
				m_results.put(appname, score);
			}
		}
	}
	
	public void AddresultAll(String appname, double score) // all results
	{
		m_results.put(appname, score);	
	}
	
	public HashMap<String, Double> getResult()
	{
		return m_results;
	}
	
	
	HashMap<String, Double> m_vector; // suggested sparse structure for storing the vector space representation with N-grams for this document
	
	public void initSparseVec()
	{
		m_vector = new HashMap<String, Double>();
	}
	
	public HashMap<String, Double> getVct() {
		return m_vector;
	}
	
	public void SetVct(String term, double value)
	{
		m_vector.put(term, value);
	}
	
	public void AddVct(String term) {
		if(m_vector.containsKey(term))
		{
			double temp = m_vector.get(term);
			temp += 1;
			m_vector.put(term, temp);
		}
		else
			m_vector.put(term, 1.0);
	}
	
HashMap<String, Double> m_tfidf_vector; // suggested sparse structure for storing the vector space representation with N-grams for this document
	
	public void init_tfidf_SparseVec()
	{
		m_tfidf_vector = new HashMap<String, Double>();
	}
	
	public HashMap<String, Double> get_tfidf_Vct() {
		return m_tfidf_vector;
	}
	
	public void Set_tfidf_Vct(String term, double value)
	{
		m_tfidf_vector.put(term, value);
	}
	
	public void Add_tfidf_Vct(String term) {
		if(m_tfidf_vector.containsKey(term))
		{
			double temp = m_tfidf_vector.get(term);
			temp += 1;
			m_tfidf_vector.put(term, temp);
		}
		else
			m_tfidf_vector.put(term, 1.0);
	}
	
	public double similiarity(Post p) {
		double result = 0;
		double m1 = 0;
		double m2 = 0;
		for (int i = 0; i < TokenVec.length; i ++)
		{
			result += TokenVec[i] * p.TokenVec[i];
			m1 += TokenVec[i] * TokenVec[i];
			m2 += p.TokenVec[i] * p.TokenVec[i];
		}
		
		m1 = Math.sqrt(m1);
		m2 = Math.sqrt(m2);
		result /= m1;
		result /= m2;
		
		return result;//compute the cosine similarity between this post and input p based on their vector space representation
	}
	
	public void Output()
	{
		System.out.println("m_ID : " + m_ID);
		System.out.println("m_author : " + m_author);
		System.out.println("m_date : " + m_date);
		System.out.println("m_content : " + m_content);
	}
	
	public Post(JSONObject json) {
		try {
			m_ID = json.getString("ReviewID");
			setAuthor(json.getString("Author"));
			
			setDate(json.getString("Date"));			
			setContent(json.getString("Content"));
			setRating(json.getDouble("Overall"));
			setLocation(json.getString("Author_Location"));			
//			MostSimi = null;
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject getJSON() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("ReviewID", m_ID);//must contain
		json.put("Author", m_author);//must contain
		json.put("Date", m_date);//must contain
		json.put("Content", m_content);//must contain
		json.put("Overall", m_rating);//must contain
		json.put("Author_Location", m_location);//must contain
		
		return json;
	}
	
//	
//	public Post[] MostSimi;
//	public Double[] SimiValue;
}
