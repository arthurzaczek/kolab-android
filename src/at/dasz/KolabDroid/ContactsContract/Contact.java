package at.dasz.KolabDroid.ContactsContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.text.TextUtils;
import at.dasz.KolabDroid.Utils;

public class Contact
{
	// TODO: Android raw contact IDs are long!
	private int		id;
	private String	uid;
	private String	givenName, familyName;
	private String	birthday	= "";		// string as in android for now
	private byte[]	photo;
	private String	notes;

	public String getBirthday()
	{
		return birthday;
	}

	public void setBirthday(String birthday)
	{
		this.birthday = birthday;
	}

	public String getGivenName()
	{
		return givenName;
	}

	public void setGivenName(String givenName)
	{
		this.givenName = givenName;
	}

	public String getFamilyName()
	{
		return familyName;
	}

	public void setFamilyName(String familyName)
	{
		this.familyName = familyName;
	}

	private List<ContactMethod>	contactMethods	= new ArrayList<ContactMethod>();

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getUid()
	{
		return uid;
	}

	public void setUid(String uid)
	{
		this.uid = uid;
	}

	public String getFullName()
	{
		return givenName + " " + familyName;
	}

	public byte[] getPhoto()
	{
		return photo;
	}

	public void setPhoto(byte[] photo)
	{
		this.photo = photo;
	}

	public String getNotes()
	{
		return notes;
	}

	public void setNote(String notes)
	{
		this.notes = notes;
	}

	public List<ContactMethod> getContactMethods()
	{
		return contactMethods;
	}

	public void clearContactMethods()
	{
		contactMethods.clear();
	}

	public void addContactMethod(ContactMethod cm)
	{
		contactMethods.add(cm);
	}

	@Override
	public String toString()
	{
		return getFullName();
	}

	public String getLocalHash()
	{
		ArrayList<String> contents = new ArrayList<String>(
				contactMethods.size() + 1);
		contents.add(getFullName() == null ? "no name" : getFullName());

		Collections.sort(contactMethods, new Comparator<ContactMethod>() {
			public int compare(ContactMethod cm1, ContactMethod cm2)
			{
				return cm1.toString().compareTo(cm2.toString());
			}
		});

		for (ContactMethod cm : contactMethods)
		{
			contents.add(cm.getData());
		}

		if (null != birthday && !"".equals(birthday))
		{
			contents.add(birthday);
		}
		else
		{
			contents.add("noBday");
		}

		if (null != photo && !"".equals(photo))
		{
			contents.add(String.valueOf(Arrays.hashCode(photo)));
		}
		else
		{
			contents.add("noPhoto");
		}

		if (null != notes && !"".equals(notes))
		{
			contents.add(notes);
		}
		else
		{
			contents.add("noNotes");
		}

		return Utils.join("|", contents.toArray());
	}

	public PhoneContact findPhone(String phone)
	{
		for (ContactMethod cm : contactMethods)
		{
			if(cm instanceof PhoneContact && TextUtils.equals(cm.getData(), phone))
			{
				return (PhoneContact)cm;
			}
		}
		
		return null;
	}
	
	public EmailContact findEmail(String mail)
	{
		for (ContactMethod cm : contactMethods)
		{
			if(cm instanceof EmailContact && TextUtils.equals(cm.getData(), mail))
			{
				return (EmailContact)cm;
			}
		}
		
		return null;
	}
}
