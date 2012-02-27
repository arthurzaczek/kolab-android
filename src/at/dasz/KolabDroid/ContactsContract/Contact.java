package at.dasz.KolabDroid.ContactsContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.text.TextUtils;

public class Contact
{
	// TODO: Android raw contact IDs are long!
	private int		id;
	private String	uid;
	private String	givenName, familyName, fullName;
	private String	birthday	= "";					// string as in android
														// for now
	private byte[]	photo;
	private String	notes;
	private String	webpage;
	private String  organization;

	public String getWebpage()
	{
		return webpage;
	}

	public void setWebpage(String webpage)
	{
		this.webpage = webpage;
	}

	public String getOrganization()
	{
		return organization;
	}

	public void setOrganization(String organization)
	{
		this.organization = organization;
	}
	
	public String getBirthday()
	{
		return birthday;
	}

	public void setBirthday(String birthday)
	{
		if(birthday != null && birthday.startsWith("0001-01-01"))
		{
			this.birthday = null;
		}
		else
		{
			this.birthday = birthday;
		}
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
		return fullName;
	}

	public void setFullName(String fullName)
	{
		this.fullName = fullName;
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

	public void removeContactMethod(ContactMethod cm)
	{
		if(null == cm)
			return;
		contactMethods.remove(cm);
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
		
		contents.add(TextUtils.isEmpty(birthday) ? "noBday" : birthday);
		contents.add(null == photo ? "noPhoto" : String.valueOf(Arrays.hashCode(photo)));
		contents.add(TextUtils.isEmpty(notes) ? "noNotes" : notes);
		contents.add(TextUtils.isEmpty(webpage) ? "noWebpage" : webpage);
		contents.add(TextUtils.isEmpty(organization) ? "noOrg" : organization);

		return TextUtils.join("|", contents.toArray());
	}

	public PhoneContact findPhone(String phone)
	{
		for (ContactMethod cm : contactMethods)
		{
			if (cm instanceof PhoneContact
					&& TextUtils.equals(cm.getData(), phone)) 
			{ 
				return (PhoneContact) cm; 
			}
		}

		return null;
	}
	
	public PhoneContact findPhone(int type)
	{
		for (ContactMethod cm : contactMethods)
		{
			if (cm instanceof PhoneContact
					&& cm.getType() == type) 
			{ 
				return (PhoneContact) cm; 
			}
		}

		return null;
	}

	public EmailContact findEmail(String mail)
	{
		for (ContactMethod cm : contactMethods)
		{
			if (cm instanceof EmailContact
					&& TextUtils.equals(cm.getData(), mail)) 
			{ 
				return (EmailContact) cm; 
			}
		}

		return null;
	}
	
	public AddressContact findAddress(int type)
	{
		for (ContactMethod cm : contactMethods)
		{
			if (cm instanceof AddressContact
					&& cm.getType() == type) 
			{ 
				return (AddressContact) cm; 
			}
		}

		return null;
	}
}
