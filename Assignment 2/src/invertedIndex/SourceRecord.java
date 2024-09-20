package invertedIndex;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author ehab
 */
public class SourceRecord {
    public int fid;
    public String URL;
    public String title;
    public String text;
    public Double norm;
    public int length;
    
    public String getURL(){
        return URL;
    }
    public SourceRecord(int f,String u, String tt,int ln, Double n, String tx){
        fid=f; URL=u; title=tt; text=tx;
        norm=n;
        length=ln;
    }
    public SourceRecord(int f,String u, String tt, String tx){
        fid=f; URL=u; title=tt; text=tx;
        norm=0.0;
        length=0;
    }
}
