/*
 * Copyright 2010 Stefan Agner <stefan@agner.ch>, All rights reserved.
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

package at.dasz.KolabDroid.ContactsContract;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.provider.ContactsContract;
import at.dasz.KolabDroid.Utils;

// no support for address contacts yet
public class AddressContact// extends ContactMethod
{
//	private String street;
//	private String city;
//	private String postalcode;
//	private String region;
//	private String country;
//
//	@Override
//	public void toXml(Document xml, Element parent, String fullName)
//	{
//		Element address = Utils.createXmlElement(xml, parent, "address");
//		switch (this.getType())
//		{	
//			case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
//				Utils.setXmlElementValue(xml, address, "type", "home");
//				break;
//			case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
//				Utils.setXmlElementValue(xml, address, "type", "business");
//				break;
//			case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
//				Utils.setXmlElementValue(xml, address, "type", "mobile");
//				break;
//			default:
//				break;
//		}
//
//		Utils.setXmlElementValue(xml, address, "street", street);
//		Utils.setXmlElementValue(xml, address, "locality", city);
//		Utils.setXmlElementValue(xml, address, "region", region);
//		Utils.setXmlElementValue(xml, address, "postal-code", postalcode);
//		Utils.setXmlElementValue(xml, address, "country", country);
//
//	}
//
//	@Override
//	public void fromXml(Element parent)
//	{
//		street = Utils.getXmlElementString(parent, "street");
//		city = Utils.getXmlElementString(parent, "locality");
//		region = Utils.getXmlElementString(parent, "region");
//		postalcode = Utils.getXmlElementString(parent, "postal-code");
//		country = Utils.getXmlElementString(parent, "country");
//		
//		String data = "";
//		if(street != null)
//			data += street + ",";
//		if(street != null)
//			data += city + ",";
//		if(street != null)
//			data += postalcode + ",";
//		if(street != null)
//			data += region + ",";
//		if(street != null)
//			data += country;
//		
//		setData(data);
//		String type = Utils.getXmlElementString(parent, "type");
//		if(type != null)
//		{
//			if(type.startsWith("home")) setType(ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
//			if(type.startsWith("business")) setType(ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
//			if(type.startsWith("mobile")) setType(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
//		}
//
//	}
//
//	public String getCity()
//	{
//		return city;
//	}
//
//
//	public String getPostalCode()
//	{
//		return postalcode;
//	}
//
//
//	public String getStreet()
//	{
//		return street;
//	}
//
//	public String getRegion()
//	{
//		return region;
//	}
//
//	public String getCountry()
//	{
//		return country;
//	}

}
