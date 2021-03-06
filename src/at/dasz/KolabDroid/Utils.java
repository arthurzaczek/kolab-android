/*
 * Copyright 2010 Arthur Zaczek <arthur@dasz.at>, dasz.at OG; All rights reserved.
 * Copyright 2010 David Schmitt <david@dasz.at>, dasz.at OG; All rights reserved.
 *
 *  This file is part of Kolab Sync for Android.

 *  Kolab Sync for Android is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation, either version 3 of
 *  the License, or (at your option) any later version.

 *  Kolab Sync for Android is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 *  of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with Kolab Sync for Android.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package at.dasz.KolabDroid;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TimeFormatException;

public final class Utils
{
	public static final String	LOG_TAG_IMAPCLIENT							= "KolabDroid-IMAPClient";
	public static final String	LOG_TAG_TRUSTMANAGERFACTORY					= "KolabDroid-TrustManagerFactory";
	public static final String	LOG_TAG_SETTINGSVIEW						= "KolabDroid-SettingsView";
	public static final String	LOG_TAG_SPECIAL_KEYSTORE_SSL_SOCKETFACTORY	= "KolabDroid-SpecialKeystoreSSLSocketFactory";

	public static final String	SYNC_ACCOUNT_TYPE							= "at.dasz.kolabdroid";

	public static String getVersionNumber(Context ctx)
	{
		String version = "?";
		try
		{
			PackageInfo pi = ctx.getPackageManager().getPackageInfo(
					ctx.getPackageName(), 0);
			version = pi.versionName;
		}
		catch (PackageManager.NameNotFoundException e)
		{
			Log.e("", "Package name not found", e);
		};
		return version;
	}

	/**
	 * date format mask for Kolab's Datetime
	 */
	private static final SimpleDateFormat	UTC_DATE_FORMAT	= new SimpleDateFormat(
																	"yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
	private static final TimeZone			utc				= TimeZone
																	.getTimeZone("UTC");

	public static String toUtc(Date date)
	{
		UTC_DATE_FORMAT.setTimeZone(utc);
		final String milliformat = UTC_DATE_FORMAT.format(date);
		return milliformat;
	}

	public static final void setXmlElementValue(Document xml, Element parent,
			String name, String value)
	{
		if (value == null || "".equals(value))
		{
			deleteXmlElements(parent, name);
		}
		else
		{
			Element e = getOrCreateXmlElement(xml, parent, name);
			// Delete old text nodes
			NodeList nl = e.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++)
			{
				e.removeChild(nl.item(i));
			}
			// add new text node
			// Text t = xml.createTextNode(value);
			// Fixes issue #29 special characters in calendar subjects and
			// elsewhere
			Text t = xml.createTextNode(TextUtils.htmlEncode(value));
			e.appendChild(t);
		}
	}

	public static final void addXmlElementValue(Document xml, Element parent,
			String name, String value)
	{
		Element e = xml.createElement(name);
		parent.appendChild(e);
		// add new text node
		// Text t = xml.createTextNode(value);
		// Fixes issue #29 special characters in calendar subjects and elsewhere
		Text t = xml.createTextNode(TextUtils.htmlEncode(value));
		e.appendChild(t);
	}

	public static final void setXmlAttributeValue(Document xml, Element parent,
			String name, String value)
	{
		Attr a = xml.createAttribute(name);
		a.setValue(value);
		parent.getAttributes().setNamedItem(a);
	}

	public static final Element getOrCreateXmlElement(Document xml,
			Element parent, String name)
	{
		NodeList nl = parent.getElementsByTagName(name);
		if (nl.getLength() == 0)
		{
			Element e = xml.createElement(name);
			parent.appendChild(e);
			return e;
		}
		else
		{
			return (Element) nl.item(0);
		}
	}

	public static final Element createXmlElement(Document xml, Element parent,
			String name)
	{
		Element e = xml.createElement(name);
		parent.appendChild(e);
		return e;
	}

	public static final Element getXmlElement(Element parent, String name)
	{
		NodeList nl = parent.getElementsByTagName(name);
		if (nl.getLength() == 0)
		{
			return null;
		}
		else
		{
			return (Element) nl.item(0);
		}
	}

	public static final NodeList getXmlElements(Element parent, String name)
	{
		return parent.getElementsByTagName(name);
	}

	public static final String getXmlElementString(Element parent, String name)
	{
		Element e = getXmlElement(parent, name);
		return getXmlElementString(e);
	}

	public static final String getXmlElementString(Element e)
	{
		if (e == null) return null;
		NodeList nl = e.getChildNodes();
		// Fix from issue #27 special characters in text elements
		// if (nl.getLength() > 0) { return nl.item(0).getNodeValue(); }
		if (nl.getLength() > 0)
		{
			StringBuilder elementText = new StringBuilder();
			for (int i = 0; i < nl.getLength(); i++)
			{
				if (nl.item(i).getNodeType() == Node.TEXT_NODE) elementText
						.append(nl.item(i).getNodeValue());
			}
			return elementText.toString();
		}

		return null;
	}

	public static final String getXmlAttributeString(Element parent, String name)
	{
		return parent.getAttribute(name);
	}

	public static final Time getXmlElementTime(Element parent, String name)
	{
		String value = getXmlElementString(parent, name);
		if (value == null || "".equals(value)) return null;
		Time t = new Time();
		t.switchTimezone("UTC");
		try
		{
			t.parse3339(value);
		}
		catch (TimeFormatException tfe)
		{
			Log.e("sync", "Unable to parse DateTime " + value);
			tfe.printStackTrace();
			return null;
		}
		t.normalize(false);
		return t;
	}

	public static final int getXmlElementInt(Element parent, String name,
			int defaultValue)
	{
		String value = getXmlElementString(parent, name);
		if (value == null || "".equals(value)) return defaultValue;
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException nfe)
		{
			Log.e("sync", "Unable to parse Integer " + value);
			nfe.printStackTrace();
			return defaultValue;
		}
	}

	public static final void deleteXmlElements(Element parent, String name)
	{
		NodeList nl = parent.getElementsByTagName(name);
		for (int i = 0; i < nl.getLength(); i++)
		{
			parent.removeChild(nl.item(i));
		}
	}

	public final static Document getDocument(InputStream xmlinput)
			throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(xmlinput);
	}

	public final static Document newDocument(String rootName)
			throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document xml = db.newDocument();
		Node root = xml.createElement(rootName);
		Attr a = xml.createAttribute("version");
		a.setValue("1.0");
		root.getAttributes().setNamedItem(a);
		xml.appendChild(root);
		return xml;
	}

	public final static String getXml(Node node)
	{
		// http://groups.google.com/group/android-developers/browse_thread/thread/2cc84c1bc8a6b477/5edb01c0721081b0
		StringBuilder buffer = new StringBuilder();

		if (node == null) { return ""; }

		if (node instanceof Document)
		{
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			buffer.append(getXml(((Document) node).getDocumentElement()));
		}
		else if (node instanceof Element)
		{
			Element element = (Element) node;
			buffer.append("<");
			buffer.append(element.getNodeName());
			if (element.hasAttributes())
			{
				NamedNodeMap map = element.getAttributes();
				for (int i = 0; i < map.getLength(); i++)
				{
					Node attr = map.item(i);
					buffer.append(" ");
					buffer.append(attr.getNodeName());
					buffer.append("=\"");
					buffer.append(attr.getNodeValue());
					buffer.append("\"");
				}
			}
			buffer.append(">");
			NodeList children = element.getChildNodes();
			final int length = children.getLength();
			boolean isFirstElement = true;
			for (int i = 0; i < length; i++)
			{
				final Node cn = children.item(i);
				final boolean isElement = cn instanceof Element;
				if (isElement && isFirstElement)
				{
					buffer.append("\n");
					isFirstElement = false;
				}
				final String xml = getXml(cn);
				if (!TextUtils.isEmpty(xml))
				{
					buffer.append(xml);
				}
			}
			buffer.append("</");
			buffer.append(element.getNodeName());
			buffer.append(">\n");
		}
		else if (node != null && node instanceof org.w3c.dom.Text)
		{
			String text = node.getNodeValue();
			if(text != null)
			{
				text = text.trim();
				if(!TextUtils.isEmpty(text))
					buffer.append(text);
			}
		}

		return buffer.toString();
	}

	public final static Date getMailDate(Message m) throws MessagingException
	{
		Date dt = m.getSentDate();
		if (dt == null) dt = m.getReceivedDate();
		return dt;
	}

	public final static byte[] sha1Hash(String text)
	{
		MessageDigest hash = null;

		try
		{
			hash = MessageDigest.getInstance("SHA1");
			byte[] input = text.getBytes();
			byte[] hashValue = hash.digest(input);
			return hashValue;
		}
		catch (Exception ex)
		{
			Log.e("EE", "Exception in sha1hash: " + ex.toString());
		}

		return null;
	}

	public final static byte[] sha1Hash(byte[] input)
	{
		MessageDigest hash = null;

		try
		{
			hash = MessageDigest.getInstance("SHA1");
			byte[] hashValue = hash.digest(input);
			return hashValue;
		}
		catch (Exception ex)
		{
			Log.e("EE", "Exception in sha1hash: " + ex.toString());
		}

		return null;
	}

	public final static String getBytesAsHexString(byte[] raw)
	{
		final String HEXES = "0123456789ABCDEF";
		if (raw == null) return null;

		StringBuilder hex = new StringBuilder(2 * raw.length);

		for (final byte b : raw)
		{
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(
					HEXES.charAt((b & 0x0F)));
		}

		return hex.toString();
	}
}
