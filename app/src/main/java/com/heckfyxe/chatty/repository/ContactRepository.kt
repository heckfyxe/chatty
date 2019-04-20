package com.heckfyxe.chatty.repository

import android.content.Context
import android.provider.ContactsContract
import com.heckfyxe.chatty.model.Contact
import io.michaelrocks.libphonenumber.android.NumberParseException
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import java.util.*

class ContactRepository : KoinComponent {

    private val context: Context by inject()

    fun getContacts(): List<Contact> {
        val phones = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null
        )

        val phoneMap = mutableMapOf<String, String>()

        while (phones!!.moveToNext()) {

            val name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            var phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

            val regionCode = Locale.getDefault().country
            val util = PhoneNumberUtil.createInstance(context)

            try {
                val number = util.parse(phoneNumber, regionCode)
                if (!util.isValidNumber(number))
                    continue
                phoneNumber = util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
            } catch (e: NumberParseException) {
                continue
            }

            phoneMap[phoneNumber] = name
        }

        phones.close()

        return phoneMap.map {
            Contact(it.key, it.value)
        }
    }
}