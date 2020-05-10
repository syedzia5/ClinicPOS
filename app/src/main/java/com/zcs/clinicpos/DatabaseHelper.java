package com.zcs.clinicpos;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Constants for Database name, table name, and column names
    public static final String DB_NAME = "NamesDB";
    public static final String TABLE_NAME = "formdata";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "xmldata";
    public static final String COLUMN_STATUS = "status";

    //database version
    private static final int DB_VERSION = 1;
    private Context mcontext;
    //Constructor
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mcontext = context;
    }

    //creating the database
    @Override
    public void onCreate(SQLiteDatabase db) {

        Toast.makeText(LoginActivity.getAppContext(), "in DatabaseHelper.onCreate()", Toast.LENGTH_SHORT).show();
        String sql = "CREATE TABLE " + TABLE_NAME
                + "(" + COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_NAME +
                " VARCHAR);"; // , " + COLUMN_STATUS + " TINYINT);";
        db.execSQL(sql);

        sql = " CREATE TABLE IF NOT EXISTS prjmbl ( ";
        sql += " PrjID int NOT NULL DEFAULT 0, ";
        sql += " Name varchar(100) NOT NULL, ";
        sql += " UserName varchar(40) NOT NULL, ";
        sql += " address1 varchar(100) DEFAULT NULL, ";
        sql += " address2 varchar(100) DEFAULT NULL, ";
        sql += " address3 varchar(100) DEFAULT NULL, ";
        sql += " zipcode varchar(10) DEFAULT NULL, ";
        sql += " DistName varchar(40) NOT NULL, ";
        sql += " StateName varchar(40) NOT NULL ";
        sql += " ); ";
        db.execSQL(sql);

        sql = " CREATE TABLE IF NOT EXISTS userdata ( ";
        sql += " UDID INTEGER PRIMARY KEY AUTOINCREMENT, ";
        sql += " user_id VARCHAR(45) NOT NULL, ";
        sql += " roleid INT NOT NULL); ";
        db.execSQL(sql);
    }

    //upgrading the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS "+ TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

    /*
     * This method is taking two arguments
     * first one is the name that is to be saved
     * second one is the status
     * 0 means the name is synced with the server
     * 1 means the name is not synced with the server
     * */
    public boolean addData(String data) { //}, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COLUMN_NAME, data);
        //contentValues.put(COLUMN_STATUS, status);


        db.insert(TABLE_NAME, null, contentValues);
        db.close();
        return true;
    }

    public boolean addPrjData(String data) { //}, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COLUMN_NAME, data);
        //contentValues.put(COLUMN_STATUS, status);


        db.insert(TABLE_NAME, null, contentValues);
        db.close();
        return true;
    }

    /*
     * This method taking two arguments
     * first one is the id of the name for which
     * we have to update the sync status
     * and the second one is the status that will be changed
     * */
    /*
    public boolean updateNameStatus(int id, int status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        //contentValues.put(COLUMN_STATUS, status);
        db.update(TABLE_NAME, contentValues, COLUMN_ID + "=" + id, null);
        db.close();
        return true;
    }
    */
    /*
     * this method will give us all the name stored in sqlite
     * */
    public Cursor getData() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " ASC;";
        Cursor c = db.rawQuery(sql, null);

        return c;
    }

    public boolean deleteData(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ID, id);
        db.delete(TABLE_NAME,  COLUMN_ID + "=" + id, null);
        db.close();
        return true;
    }

    public void syncProjectData(String xmldata) {

        //Toast.makeText(LoginActivity.getAppContext(), "in dh.syncProjectData: "+xmldata, Toast.LENGTH_SHORT).show();

        SQLiteDatabase dbs = this.getWritableDatabase();

        try {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmldata)));
            Element element = doc.getDocumentElement();
            element.normalize();

            NodeList nList = doc.getElementsByTagName("PRJMBLDATAREC");
            int len = nList.getLength();
            //Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: Num PRJMBLDATAREC: "+Integer.toString(len), Toast.LENGTH_SHORT).show();

            if (len > 0) {
                dbs.delete("prjmbl", null,null);
            }
            else {
                //Something wrong???
            }
            for (int i = 0; i < len; i++) {
                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element2 = (Element)node;

                    ContentValues contentValues = new ContentValues();
                    contentValues.put("PrjID", Integer.parseInt(getValue("PRJID", element2)));
                    contentValues.put("Name",getValue("PRJNAME", element2));
                    contentValues.put("UserName",getValue("PRJDEVNAME", element2));
                    contentValues.put("address1",getValue("PRJADDR1", element2));
                    contentValues.put("address2",getValue("PRJADDR2", element2));
                    contentValues.put("address3",getValue("PRJADDR3", element2));
                    contentValues.put("zipcode",getValue("PRJZIP", element2));
                    contentValues.put("DistName",getValue("PRJCITY", element2));
                    contentValues.put("StateName",getValue("PRJSTATE", element2));

                    long ret = dbs.insert("prjmbl", null, contentValues);

                    //Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: Inserted at "+Integer.toString(i)+" PRJID = "+contentValues.getAsInteger("PrjId"), Toast.LENGTH_LONG).show();

                    if (ret < 0) {

                        Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: Error inserting prjdata"+Integer.toString((int)ret), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            dbs.close();
            return;
        }
        catch (ParserConfigurationException  e) {

            Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: ParserConfigurationException : Error inserting prjdata "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
        catch ( SAXException e) {

            Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: SAXException : Error inserting prjdata "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
        catch (IOException  e) {

            Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: IOException : Error inserting prjdata "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {

            Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData : Unknown : Error inserting prjdata "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private String getValue(String tag, Element element) {
        String s = "";
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        if(nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            s = node.getNodeValue();
        }
        return s;
    }

    public Cursor getPrjData() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = " SELECT * FROM prjmbl; "; //""SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " ASC;";
        Cursor c = db.rawQuery(sql, null);
        return c;
    }


    /*
     * this method is for getting all the unsynced name
     * so that we can sync it with database
     * */
    /*
    public Cursor getUnsyncedNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = 0;";
        Cursor c = db.rawQuery(sql, null);
        return c;
    }
     */

    public void uploadData (String cookie) {
        SQLiteDatabase dbs = this.getReadableDatabase();
        String sql = " SELECT * FROM "+TABLE_NAME+"; "; //""SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " ASC;";
        Cursor c = dbs.rawQuery(sql, null);
        int numprj = c.getCount();
        //Toast.makeText(mContext, "in getPrjData: Num of Projects"+Integer.toString(numprj), Toast.LENGTH_SHORT).show();

        String s = "";

        try {
            if (numprj > 0) {
                if (c.moveToFirst()) {

                    do {
                        String xmldata = c.getString(c.getColumnIndex(COLUMN_NAME));

                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document doc = db.parse(new InputSource(new StringReader(xmldata)));
                        Element element = doc.getDocumentElement();
                        element.normalize();

                        String prjid = getValue("PRJID", element);
                        String funcname = getValue("FUNCNAME", element);
                        new AlertDialog.Builder(mcontext).setTitle("Alert").setMessage("prjid="+prjid+", funcname="+funcname).setNeutralButton("Close", null).show();

                        HttpClient client = new HttpClient(funcname,cookie);
                        String url = client.connectForMultipart();
                        new AlertDialog.Builder(mcontext).setTitle("Alert").setMessage("url="+url).setNeutralButton("Close", null).show();
                        client.addFormPart("prjid", prjid);

                        NodeList nList = doc.getElementsByTagName("PARAM");
                        int len = nList.getLength();
                        //Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: Num PRJMBLDATAREC: "+Integer.toString(len), Toast.LENGTH_SHORT).show();

                        if (len == 0) {
                            continue;
                        }
                        for (int i = 0; i < len; i++) {
                            Node node = nList.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {

                                Element element2 = (Element) node;

                                String pnm = getValue("NAME", element2);
                                String ptyp = getValue("TYPE", element2);
                                String pvl = getValue("VALUE", element2);
                                if (Integer.parseInt(ptyp) == 0) {
                                    client.addFormPart(pnm, pvl);
                                }
                                else {
                                    client.addFilePart(pnm, pvl);
                                }
                                /*
                                String flid = getValue("PRJIMGFILEID", element2);
                                client.addFormPart("filelsVALUEt", flid);
                                String flnm = getValue("PRJIMGFILENAME", element2);
                                client.addFilePart(flid, flnm);

                                String dsid =   getValue("PRJIMGDESCID", element2);
                                String dsdc =   getValue("PRJIMGDESC", element2);
                                if (dsid != null && dsid.length() > 0) {
                                    client.addFormPart("dsid", dsdc);
                                }

                                ContentValues contentValues = new ContentValues();
                                contentValues.put("PrjID", Integer.parseInt(getValue("PRJIMGFILEID", element2)));
                                contentValues.put("Name", getValue("PRJNAME", element2));
                                contentValues.put("UserName", getValue("PRJDEVNAME", element2));
                                contentValues.put("address1", getValue("PRJADDR1", element2));
                                contentValues.put("address2", getValue("PRJADDR2", element2));
                                contentValues.put("address3", getValue("PRJADDR3", element2));
                                contentValues.put("zipcode", getValue("PRJZIP", element2));
                                contentValues.put("DistName", getValue("PRJCITY", element2));
                                contentValues.put("StateName", getValue("PRJSTATE", element2));

                                long ret = db.insert("prjmbl", null, contentValues);

                                //Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: Inserted at "+Integer.toString(i)+" PRJID = "+contentValues.getAsInteger("PrjId"), Toast.LENGTH_LONG).show();

                                if (ret < 0) {

                                    Toast.makeText(LoginActivity.getAppContext(), "in syncProjectData: Error inserting prjdata" + Integer.toString((int) ret), Toast.LENGTH_SHORT).show();
                                }

                                 */
                            }
                        }
                        client.finishMultipart();
                        String data = client.getResponse();
                    } while (c.moveToNext());
                }
            }
        }
        catch (ParserConfigurationException  e) {

            Toast.makeText(LoginActivity.getAppContext(), "in uploadData: ParserConfigurationException : Error uploading data "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
        catch ( SAXException e) {

            Toast.makeText(LoginActivity.getAppContext(), "in uploadData: SAXException : Error uploading data "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
        catch (IOException  e) {

            Toast.makeText(LoginActivity.getAppContext(), "in uploadData: IOException : Error uploading data "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
        /*catch (Exception e) {

            Toast.makeText(LoginActivity.getAppContext(), "in uploadData : Unknown : Error inserting prjdata "+e.getMessage(), Toast.LENGTH_LONG).show();
        }*/
   }

   public UserData getUserData() {

        UserData ud = null;
        try {

            SQLiteDatabase dbs = this.getReadableDatabase();
            String sql = " SELECT * FROM userdata; "; //""SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " ASC;";
            Cursor c = dbs.rawQuery(sql, null);
            int numprj = c.getCount();
            //Toast.makeText(mContext, "in getPrjData: Num of Projects"+Integer.toString(numprj), Toast.LENGTH_SHORT).show();

            String s = "";

            if (numprj > 0) {
                if (c.moveToFirst()) {

                    ud  = new UserData();
                    ud.UDID(c.getInt(c.getColumnIndex("UDID")));
                    ud.user_id(c.getString(c.getColumnIndex("user_id")));
                    ud.roleid(c.getInt(c.getColumnIndex("roleid")));
                }
            }
        }
        catch (Exception e ) {
            String msg = e.getMessage();
            e.printStackTrace();
        }
        return ud;
   }

   public void setUserData(String data) throws JSONException {

        UserData ud = getUserData();

        JSONObject dt = new JSONObject(data);

        String sql = "";
        if (ud == null) {

            //sql = " INSERT into userdata (user_id, roleid) values (\""+dt.getString("user_id")+"\", "+dt.getString("roleid")+");";
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();

            contentValues.put("user_id", dt.getString("user_id"));
            contentValues.put("roleid", dt.getInt("roleid"));

            db.insert("userdata", null, contentValues);
            db.close();

        }
        /*
        else {

        }
         */
   }
}
