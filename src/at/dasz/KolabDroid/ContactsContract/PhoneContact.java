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

package at.dasz.KolabDroid.ContactsContract;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.provider.ContactsContract.CommonDataKinds.Phone;
import at.dasz.KolabDroid.Utils;

public class PhoneContact extends ContactMethod
{

	public PhoneContact()
	{
		setType(Phone.TYPE_OTHER);
	}

	@Override
	public void toXml(Document xml, Element parent, String fullName)
	{
		Element phone = Utils.createXmlElement(xml, parent, "phone");
		switch (this.getType())
		{
		case Phone.TYPE_WORK:
			Utils.setXmlElementValue(xml, phone, "type", "business1");
			break;
		case Phone.TYPE_WORK_MOBILE:
			Utils.setXmlElementValue(xml, phone, "type", "business2");
			break;
		case Phone.TYPE_FAX_WORK:
			Utils.setXmlElementValue(xml, phone, "type", "businessfax");
			break;
		case Phone.TYPE_CALLBACK:
			Utils.setXmlElementValue(xml, phone, "type", "callback");
			break;
		case Phone.TYPE_CAR:
			Utils.setXmlElementValue(xml, phone, "type", "car");
			break;
		case Phone.TYPE_COMPANY_MAIN:
			Utils.setXmlElementValue(xml, phone, "type", "company");
			break;
		case Phone.TYPE_HOME:
			Utils.setXmlElementValue(xml, phone, "type", "home1");
			break;
		case Phone.TYPE_OTHER_FAX: // sic!
			Utils.setXmlElementValue(xml, phone, "type", "home2");
			break;
		case Phone.TYPE_FAX_HOME:
			Utils.setXmlElementValue(xml, phone, "type", "homefax");
			break;
		case Phone.TYPE_ISDN:
			Utils.setXmlElementValue(xml, phone, "type", "isdn");
			break;
		case Phone.TYPE_MOBILE:
			Utils.setXmlElementValue(xml, phone, "type", "mobile");
			break;
		case Phone.TYPE_PAGER:
			Utils.setXmlElementValue(xml, phone, "type", "pager");
			break;
		case Phone.TYPE_MAIN:
			Utils.setXmlElementValue(xml, phone, "type", "primary");
			break;
		case Phone.TYPE_RADIO:
			Utils.setXmlElementValue(xml, phone, "type", "radio");
			break;
		case Phone.TYPE_TELEX:
			Utils.setXmlElementValue(xml, phone, "type", "telex");
			break;
		case Phone.TYPE_TTY_TDD:
			Utils.setXmlElementValue(xml, phone, "type", "ttytdd");
			break;
		case Phone.TYPE_ASSISTANT:
			Utils.setXmlElementValue(xml, phone, "type", "assistant");
			break;
		default:
			Utils.setXmlElementValue(xml, phone, "type", "other");
			break;
		}
		Utils.setXmlElementValue(xml, phone, "number", getData());
	}
	
	@Override
	public void fromXml(Element parent)
	{
		this.setData(Utils.getXmlElementString(parent, "number"));
		setType(Phone.TYPE_OTHER);
		String type = Utils.getXmlElementString(parent, "type");
		if(type != null)
		{
			if(type.equals("business1")) setType(Phone.TYPE_WORK);
			if(type.equals("business2")) setType(Phone.TYPE_WORK_MOBILE);
			if(type.equals("businessfax")) setType(Phone.TYPE_FAX_WORK);
			if(type.equals("callback")) setType(Phone.TYPE_CALLBACK);
			if(type.equals("car")) setType(Phone.TYPE_CAR);
			if(type.equals("company")) setType(Phone.TYPE_COMPANY_MAIN);
			if(type.equals("home1")) setType(Phone.TYPE_HOME);
			if(type.equals("home2")) setType(Phone.TYPE_OTHER_FAX); // sic!
			if(type.equals("homefax")) setType(Phone.TYPE_FAX_HOME);
			if(type.equals("isdn")) setType(Phone.TYPE_ISDN);
			if(type.equals("mobile")) setType(Phone.TYPE_MOBILE);
			if(type.equals("pager")) setType(Phone.TYPE_PAGER);
			if(type.equals("primary")) setType(Phone.TYPE_MAIN);
			if(type.equals("radio")) setType(Phone.TYPE_RADIO);
			if(type.equals("telex")) setType(Phone.TYPE_TELEX);
			if(type.equals("ttytdd")) setType(Phone.TYPE_TTY_TDD);
			if(type.equals("assistant")) setType(Phone.TYPE_ASSISTANT);
			if(type.equals("other")) setType(Phone.TYPE_OTHER);
		}
	}
}
