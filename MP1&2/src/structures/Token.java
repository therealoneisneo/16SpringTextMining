/**
 * 
 */
package structures;

/**
 * @author hongning
 * Suggested structure for constructing N-gram language model and vector space representation
 */
public class Token {

	int m_id; // the numerical ID you assigned to this token/N-gram
	String m_token; // the actual text content of this token/N-gram
	boolean Pos = false;// indicator for if this term appeared in Positive or Negative reviews
	boolean Neg = false;

	
	public int getID() {
		return m_id;
	}

	public void setID(int id) {
		this.m_id = id;
	}

	
	public String getToken() {
		return m_token;
	}

	public void setToken(String token) {
		this.m_token = token;
	}
	
	
	
	double m_value; // frequency or count of this token/N-gram within a document
	double m_df_d; // document frequency of discription
	double m_df_r; // document frequency of reviews
	
	public double getValue() {
		return m_value;
	}

	public void setValue(double value) {
		this.m_value =value;
	}	
	
	public double getdfd() {
		return  m_df_d;
	}

	public void setdfd(double value) {
		this.m_df_d = value;
	}	
	
	public double getdfr() {
		return  m_df_r;
	}

	public void setdfr(double value) {
		this.m_df_r = value;
	}	
	
	//default constructor
	public Token(String token) {
		m_token = token;
		m_id = -1;
		m_value = 0;		
		m_df_d = 0;
		m_df_r = 0;
	}

	//default constructor
	public Token(int id, String token) {
		m_token = token;
		m_id = id;
		m_value = 0;
		m_df_d = 0;
		m_df_r = 0;
	}
	
	public void setPosNeg(double score)
	{
		if (score >= 4)
			Pos = true;
		else
			Neg = true;
	}
}
