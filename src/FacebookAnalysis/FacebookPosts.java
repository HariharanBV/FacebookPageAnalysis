/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FacebookAnalysis;

import com.csvreader.CsvWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

/**
 *
 * @author admin
 */
public class FacebookPosts {
    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read); 
            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }

    }
    public static void main(String args[]) throws Exception
    {
        CsvWriter csvposts = new CsvWriter(new FileWriter("posts1.csv", true), ',');
        CsvWriter csvcomments=new CsvWriter(new FileWriter("comments1.csv", true), ',');
        csvposts.write("postid");
        csvposts.write("postlikes");
        csvposts.write("postmessage");
        csvposts.write("postsentiment");
        csvposts.write("postcreatedtime");
        csvposts.write("postoverallsentiment");
        csvposts.write("noofcomments");
        csvposts.endRecord();
        csvcomments.write("commentid");
        csvcomments.write("postid");
        csvcomments.write("commentlikes");
        csvcomments.write("sentiment");
        csvcomments.write("message");
        csvcomments.write("createdtime");
        csvcomments.endRecord();
        RConnection connection=new RConnection();
        connection.eval("library(RSentiment)"); 
        String json=readUrl("https://graph.facebook.com/v2.10/874575546039021/posts?access_token=135225950534364|MjIpFfEtL-fI3LWkDdGltHESdC4");
        System.out.println(json);
        JSONParser parser=new JSONParser();       
        JSONObject jo=(JSONObject) parser.parse(json);
        JSONArray ja = (JSONArray) jo.get("data");          
        for(int i=0;i<ja.size();i++)
        {
            JSONObject jo1=(JSONObject)ja.get(i);
            String postid=(String)jo1.get("id");
            String postmessage=(String)jo1.get("message");
            String postsentiment=calculateSentiment(postmessage,connection);
            String postcreatedtime=(String)jo1.get("created_time");
             String json1=readUrl("https://graph.facebook.com/v2.10/"+postid+"/likes?access_token=135225950534364|MjIpFfEtL-fI3LWkDdGltHESdC4");
             JSONObject jo2=(JSONObject)parser.parse(json1);
             JSONArray ja1=(JSONArray)jo2.get("data");
             int likes=ja1.size();             
             System.out.println(postid+"\t"+postmessage+"\t"+postsentiment+"\t");
             System.out.print(postcreatedtime+"\t"+likes);
             System.out.println("----------");
             generatecomment(postid,postmessage,postsentiment,postcreatedtime,likes,connection,csvposts,csvcomments);
        }
        csvposts.close();
        csvcomments.close();
    } 
    private static void generatecomment(String postid, String postmessage, String postsentiment, String postcreatedtime, int likes, RConnection connection,CsvWriter csvposts,CsvWriter csvcomments) throws Exception {
             int positive=0,negative=0,neutral=0;
               String json=readUrl("https://graph.facebook.com/v2.10/"+postid+"/comments?access_token=135225950534364|MjIpFfEtL-fI3LWkDdGltHESdC4");
      // System.out.println("Comments JSON"+json);
       JSONParser parser=new JSONParser();
         JSONObject jo=(JSONObject) parser.parse(json);
        JSONArray ja = (JSONArray) jo.get("data");     
       int noofcomments=ja.size();
        for(int i=0;i<ja.size();i++)
        {
            JSONObject jo1=(JSONObject) ja.get(i);          
            String  message=(String) jo1.get("message");
            String created_time=(String)jo1.get("created_time");    
            String commentid=(String)jo1.get("id");
            JSONObject jo2=(JSONObject)jo1.get("from");
            int commentlikes=getcommentlikes(commentid);
            String sentiment=calculateSentiment(message,connection);
            if(sentiment=="Positive")
                    positive++;
            else if(sentiment=="Negative")
                    negative++;
            else if(sentiment=="Neutral")
                    neutral++;
            System.out.println(commentid+"\t"+postid+"\t"+commentlikes+"\t"+message+"\t"+sentiment+"\t"+created_time);
            String commentlikes1=Integer.toString(commentlikes);
            csvcomments.write(commentid);
            csvcomments.write(postid);
            csvcomments.write(commentlikes1);
            csvcomments.write(sentiment);
            csvcomments.write(message);
            csvcomments.write(created_time);
            csvcomments.endRecord();
        }
          String overallpostsentiment=generateoverallreview(positive,negative,neutral);
        System.out.println("Överall Sentiment is"+overallpostsentiment);
        System.out.println("******************");
        System.out.println(noofcomments);
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
        csvposts.write(postid);
        csvposts.write(Integer.toString(likes));
        csvposts.write(postmessage);
        csvposts.write(postsentiment);
        csvposts.write(postcreatedtime);
        csvposts.write(overallpostsentiment);
        csvposts.write(Integer.toString(noofcomments));
        csvposts.endRecord();   
    }
  
      private static int getcommentlikes(String commentid) throws Exception {
        
         String json=readUrl( "https://graph.facebook.com/v2.10/"+commentid+"/likes?access_token=135225950534364|MjIpFfEtL-fI3LWkDdGltHESdC4");
          // System.out.println("Comments JSON"+json);
       JSONParser parser=new JSONParser();
         JSONObject jo=(JSONObject) parser.parse(json);
        JSONArray ja = (JSONArray) jo.get("data");  
       
        return ja.size();
    }
    private static String generateoverallreview(int positive, int negative, int neutral) {
        if(positive>negative)
       {
           if(positive>neutral)
                return "Positive";
           else
               return "Neutral";
           
       }
       else
       {
           if(negative>neutral)
                return "Negative";
           else 
               return "Neutral";
        }
    }

    
    private static String calculateSentiment(String message,RConnection connection) throws RserveException, REXPMismatchException {
       
     if(message == null||message.equals(""))
         return null;
     else
     {
       
         connection.eval("result<-calculate_score('"+message +"')");
            connection.eval("print(result['1'])");       
            connection.eval("y<-toString(result['1'])");            
              connection.eval("print(y)");       
            String Result=connection.eval("y").asString();
            int score=Integer.parseInt(Result);
            if(score>0)
                return "Positive";
            else if(score<0)
                return "Negative";
            else if(score==0)
                return "Neutral";
            return "Undefined";
     }
    }

   
   
    
}
