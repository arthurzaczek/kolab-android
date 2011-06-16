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
		//TODO: we only support 1 home and 1 business number for now		
		case Phone.TYPE_HOME:
			Utils.setXmlElementValue(xml, phone, "type", "home1");
			break;
		case Phone.TYPE_WORK:
			Utils.setXmlElementValue(xml, phone, "type", "business1");
			break;
		case Phone.TYPE_MOBILE:
			Utils.setXmlElementValue(xml, phone, "type", "mobile");
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
			if(type.startsWith("home")) setType(Phone.TYPE_HOME);
			if(type.startsWith("business")) setType(Phone.TYPE_WORK);
			if(type.startsWith("mobile")) setType(Phone.TYPE_MOBILE);
		}
	}
}
