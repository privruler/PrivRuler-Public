package dfa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AndroidFunctionSummary implements MyConstants {

	public AndroidFunctionSummary() {
	}

	public static LinkedHashMap<String, LinkedHashMap<Integer, List<Integer>>> summary = new LinkedHashMap<String, LinkedHashMap<Integer, List<Integer>>>();

	public static void buildFunctionSummary() {

		String signature = null;
		Integer source = null;
		List<Integer> dests = null;
		LinkedHashMap<Integer, List<Integer>> stod = null;

		// <org.xmlpull.v1.XmlPullParser: void setInput(java.io.Reader)>
		{
			signature = "<org.xmlpull.v1.XmlPullParser: void setInput(java.io.Reader)>";
			stod = new LinkedHashMap<Integer, List<Integer>>();
			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);
			summary.put(signature, stod);
		}

		// <org.xmlpull.v1.XmlPullParser: void
		// setInput(java.io.InputStream,java.lang.String)>
		{
			signature = "<org.xmlpull.v1.XmlPullParser: void setInput(java.io.InputStream,java.lang.String)>";
			stod = new LinkedHashMap<Integer, List<Integer>>();
			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);
			summary.put(signature, stod);
		}

		// <java.util.Hashtable: java.lang.Object
		// put(java.lang.Object,java.lang.Object)>
		{
			signature = "<java.util.Hashtable: java.lang.Object put(java.lang.Object,java.lang.Object)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.lang.StringBuilder: void <init>(java.lang.String)>
		{
			signature = "<java.lang.StringBuilder: void <init>(java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.lang.String: void <init>(java.lang.String)>
		{
			signature = "<java.lang.String: void <init>(java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.util.UUID: void <init>(long,long)>
		{
			signature = "<java.util.UUID: void <init>(long,long)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <org.apache.http.client.entity.UrlEncodedFormEntity: void
		// <init>(java.util.List,java.lang.String)>
		{
			signature = "<org.apache.http.client.entity.UrlEncodedFormEntity: void <init>(java.util.List,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <org.apache.http.client.methods.HttpPost: void
		// <init>(java.lang.String)>
		{
			signature = "<org.apache.http.client.methods.HttpPost: void <init>(java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.security.MessageDigest: void update(byte[])>
		{
			signature = "<java.security.MessageDigest: void update(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.lang.System: void
		// arraycopy(java.lang.Object,int,java.lang.Object,int,int)>
		{
			signature = "<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(2);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.lang.String: void <init>(byte[],java.lang.String)>
		{
			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			signature = "<java.lang.String: void <init>(byte[],java.lang.String)>";
			summary.put(signature, stod);

			signature = "<java.lang.String: void <init>(byte[])>";
			summary.put(signature, stod);
		}

		// <android.widget.EditText: void setText(java.lang.CharSequence)>
		{
			signature = "<android.widget.EditText: void setText(java.lang.CharSequence)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <android.widget.TextView: void setText(java.lang.CharSequence)>
		{
			signature = "<android.widget.TextView: void setText(java.lang.CharSequence)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/xmlpull/v1/XmlSerializer;.setOutput
		{
			signature = "<org.xmlpull.v1.XmlSerializer: void setOutput(java.io.OutputStream,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/xmlpull/v1/XmlSerializer;.attribute
		{
			signature = "<org.xmlpull.v1.XmlSerializer: org.xmlpull.v1.XmlSerializer attribute(java.lang.String,java.lang.String,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 2;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			dests.add(returnValue);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/security/MessageDigest;.digest
		{
			signature = "<java.security.MessageDigest: int digest(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/Formatter;.format
		{
			signature = "<java.util.Formatter: java.util.Formatter format(java.lang.String,java.lang.Object[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			dests.add(returnValue);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/net/URL;.<init>
		{
			signature = "<java.net.URL: void <init>(java.lang.String)>";
			stod = new LinkedHashMap<Integer, List<Integer>>();
			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/HashMap;.put;
		{
			signature = "<java.util.HashMap: java.lang.Object put(java.lang.Object,java.lang.Object)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/LinkedHashMap;.put;
		{
			signature = "<java.util.LinkedHashMap: java.lang.Object put(java.lang.Object,java.lang.Object)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/Map;.put
		{
			signature = "<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/net/HttpURLConnection;.addRequestProperty
		{
			signature = "<java.net.HttpURLConnection: void addRequestProperty(java.lang.String,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/List;.addAll
		{
			signature = "<java.util.List: boolean addAll(java.util.Collection)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/List;.add
		{
			signature = "<java.util.List: boolean add(java.lang.Object)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/LinkedList;.add
		{
			signature = "<java.util.LinkedList: boolean add(java.lang.Object)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/Vector;.add
		{
			signature = "<java.util.Vector: boolean add(java.lang.Object)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/util/ArrayList;.add
		{
			signature = "<java.util.ArrayList: boolean add(java.lang.Object)>";
			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/apache/http/client/methods/HttpGet;.<init>
		{
			signature = "<org.apache.http.client.methods.HttpGet: void <init>(java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/apache/http/message/BasicNameValuePair;.<init>
		{
			signature = "<org.apache.http.message.BasicNameValuePair: void <init>(java.lang.String,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/apache/http/client/entity/UrlEncodedFormEntity;.<init>
		{
			signature = "<org.apache.http.client.entity.UrlEncodedFormEntity: void <init>(java.util.List)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <org.apache.http.client.methods.HttpEntityEnclosingRequestBase: void
		// setEntity(org.apache.http.HttpEntity)>
		{
			signature = "<org.apache.http.client.methods.HttpEntityEnclosingRequestBase: void setEntity(org.apache.http.HttpEntity)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/apache/http/client/methods/HttpPost;.setEntity
		{
			signature = "<org.apache.http.client.methods.HttpPost: void setEntity(org.apache.http.HttpEntity)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/apache/http/entity/FileEntity;.<init>
		{
			signature = "<org.apache.http.entity.FileEntity: void <init>(java.io.File,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/apache/http/entity/ByteArrayEntity;.<init>
		{
			signature = "<org.apache.http.entity.ByteArrayEntity: void <init>(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Landroid/content/ContentValues;.put
		{
			// Byte
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.Byte)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// Integer
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.Integer)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// Float
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.Float)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// Short
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.Short)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// byte[]
			signature = "<android.content.ContentValues: void put(java.lang.String,byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// String
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// Double
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.Double)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// Long
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.Long)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// Boolean
			signature = "<android.content.ContentValues: void put(java.lang.String,java.lang.Boolean)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/io/InputStream;.read
		{
			// byte[]
			signature = "<java.io.InputStream: int read(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);

			// byte[],int,int
			signature = "<java.io.InputStream: int read(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.BufferedInputStream: int read(byte[],int,int)>
		{
			// byte[]
			signature = "<java.io.BufferedInputStream: int read(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);

			// byte[],int,int
			signature = "<java.io.BufferedInputStream: int read(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/io/ByteArrayOutputStream;.write
		{
			// byte[]
			signature = "<java.io.ByteArrayOutputStream: void write(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

		}

		// Ljava/io/ByteArrayOutputStream;.write
		{
			// byte[]
			signature = "<java.io.ByteArrayOutputStream: void writeTo(java.io.OutputStream)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);
			summary.put(signature, stod);
		}

		// Ljava/net/HttpURLConnection;.setRequestProperty
		{
			signature = "<java.net.HttpURLConnection: void setRequestProperty(java.lang.String,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

		}

		// Ljava/net/HttpURLConnection;.setRequestMethod
		{
			signature = "<java.net.HttpURLConnection: void setRequestMethod(java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

		}

		// Ljava/io/ByteArrayInputStream;.read
		{
			signature = "<java.io.ByteArrayInputStream: int read(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);

		}

		// Ljava/io/OutputStream;.write
		{
			// byte[],int,int
			signature = "<java.io.OutputStream: void write(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// byte[]
			signature = "<java.io.OutputStream: void write(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

		}

		// Ljava/lang/StringBuffer;.append
		{
			// char[],int,int
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(char[],int,int)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// double
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(double)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// boolean
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(boolean)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// long
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(long)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// float
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(float)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// char[]
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(char[])>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// int
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(int)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.StringBuffer
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.StringBuffer)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.CharSequence
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.CharSequence)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// char
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(char)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.String
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// CharSequence,int,int
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.CharSequence,int,int)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.Object
			{
				signature = "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.Object)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

		}

		// Ljava/lang/StringBuilder;.append
		{
			// char[],int,int
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(char[],int,int)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// double
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(double)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// boolean
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(boolean)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// long
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(long)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// float
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(float)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// char[]
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(char[])>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// int
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(int)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.StringBuffer
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.StringBuffer)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.CharSequence
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.CharSequence)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// char
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(char)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.String
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);

				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);

				stod.put(source, dests);

				summary.put(signature, stod);

			}

			// CharSequence,int,int
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.CharSequence,int,int)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

			// java.lang.Object
			{
				signature = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>";

				stod = new LinkedHashMap<Integer, List<Integer>>();

				source = 0;
				dests = new ArrayList<Integer>();
				dests.add(thisObject);
				dests.add(returnValue);
				stod.put(source, dests);

				source = thisObject;
				dests = new ArrayList<Integer>();
				dests.add(returnValue);
				stod.put(source, dests);

				summary.put(signature, stod);
			}

		}

		// Ljava/io/DataOutputStream;.write
		{
			// byte[],int,int
			signature = "<java.io.DataOutputStream: void write(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			// byte[]
			signature = "<java.io.DataOutputStream: void write(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

		}

		// <java.io.DataOutputStream: void writeInt(int)>
		{
			signature = "<java.io.DataOutputStream: void writeInt(int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeShort(int)>
		{
			signature = "<java.io.DataOutputStream: void writeShort(int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeLong(long)>
		{
			signature = "<java.io.DataOutputStream: void writeLong(long)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeUTF(java.lang.String)>
		{
			signature = "<java.io.DataOutputStream: void writeUTF(java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeBoolean(boolean)>
		{
			signature = "<java.io.DataOutputStream: void writeBoolean(boolean)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeByte(int)>
		{
			signature = "<java.io.DataOutputStream: void writeByte(int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/io/DataOutputStream;.writeBytes
		{
			signature = "<java.io.DataOutputStream: void writeBytes(java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeChar(int)>
		{
			signature = "<java.io.DataOutputStream: void writeChar(int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeChars(String)>
		{
			signature = "<java.io.DataOutputStream: void writeChars(String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeDouble(double)>
		{
			signature = "<java.io.DataOutputStream: void writeDouble(double)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <java.io.DataOutputStream: void writeFloat(float)>
		{
			signature = "<java.io.DataOutputStream: void writeFloat(float)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/io/DataInputStream;.read
		{
			signature = "<java.io.DataInputStream: int read(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);

			signature = "<java.io.DataInputStream: int read(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = thisObject;
			dests = new ArrayList<Integer>();
			dests.add(0);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Landroid/content/SharedPreferences$Editor;.putString
		// android.content.SharedPreferences.Editor
		{
			signature = "<android.content.SharedPreferences.Editor: android.content.SharedPreferences.Editor putString(java.lang.String,java.lang.String)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Landroid/content/SharedPreferences$Editor;.putInt
		{
			signature = "<android.content.SharedPreferences.Editor: android.content.SharedPreferences.Editor putInt(java.lang.String,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Landroid/content/SharedPreferences$Editor;.putLong
		{
			signature = "<android.content.SharedPreferences.Editor: android.content.SharedPreferences.Editor putLong(java.lang.String,long)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Landroid/content/SharedPreferences$Editor;.putFloat
		{
			signature = "<android.content.SharedPreferences.Editor: android.content.SharedPreferences.Editor putFloat(java.lang.String,float)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// <android.content.SharedPreferences$Editor:
		// android.content.SharedPreferences$Editor
		// putFloat(java.lang.String,float)>
		{
			signature = "<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putFloat(java.lang.String,float)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Landroid/content/SharedPreferences$Editor;.putBoolean
		{
			signature = "<android.content.SharedPreferences.Editor: android.content.SharedPreferences.Editor putBoolean(java.lang.String,boolean)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Ljava/io/ByteArrayInputStream;.<init>
		{
			signature = "<java.io.ByteArrayInputStream: void <init>(byte[])>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);

			signature = "<java.io.ByteArrayInputStream: void <init>(byte[],int,int)>";

			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			stod.put(source, dests);

			summary.put(signature, stod);
		}

		// Lorg/json/JSONObject;.put
		{
			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 1;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			dests.add(returnValue);
			stod.put(source, dests);

			signature = "<org.json.JSONObject: org.json.JSONObject put(java.lang.String,int)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONObject: org.json.JSONObject put(java.lang.String,long)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONObject: org.json.JSONObject put(java.lang.String,java.lang.Object)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONObject: org.json.JSONObject put(java.lang.String,boolean)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONObject: org.json.JSONObject put(java.lang.String,double)>";
			summary.put(signature, stod);
		}

		// Lorg/json/JSONArray;.put
		{
			stod = new LinkedHashMap<Integer, List<Integer>>();

			source = 0;
			dests = new ArrayList<Integer>();
			dests.add(thisObject);
			dests.add(returnValue);
			stod.put(source, dests);

			signature = "<org.json.JSONArray: org.json.JSONArray put(boolean)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONArray: org.json.JSONArray put(int)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONArray: org.json.JSONArray put(long)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONArray: org.json.JSONArray put(double)>";
			summary.put(signature, stod);

			signature = "<org.json.JSONArray: org.json.JSONArray put(java.lang.Object)>";
			summary.put(signature, stod);
		}
	}

	public static LinkedHashMap<Integer, List<Integer>> lookupFunctionSummary(String signature) {
		if (summary.containsKey(signature)) {
			return summary.get(signature);
		} else {
			return null;
		}
	}

}
