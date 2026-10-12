@file:OptIn(
    kotlinx.cinterop.BetaInteropApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.example.academyapp.session

import com.example.academyapp.network.AcademyJson
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import platform.CoreFoundation.CFDictionaryRef
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

class IosKeychainTokenStorage(
    private val service: String = "online.academy.app.session",
    private val account: String = "mobile-api-tokens-v1",
) : SessionTokenStorage {
    override suspend fun load(): SessionTokens? {
        val query = baseQuery().apply {
            setSecurityValue(kSecReturnData, platform.CoreFoundation.kCFBooleanTrue)
            setSecurityValue(kSecMatchLimit, kSecMatchLimitOne)
        }
        val data = memScoped {
            val result = alloc<CPointerVarOf<CPointer<out CPointed>>>()
            when (val status = SecItemCopyMatching(query.asDictionary(), result.ptr)) {
                errSecSuccess -> result.value?.let {
                    interpretObjCPointerOrNull<NSData>(it.rawValue)
                } ?: throw KeychainException(status, "Keychain returned an invalid token payload.")
                errSecItemNotFound -> null
                else -> throw KeychainException(status, "Unable to read the Keychain session.")
            }
        } ?: return null

        return AcademyJson.decodeFromString(data.toByteArray().decodeToString())
    }

    override suspend fun save(tokens: SessionTokens) {
        val data = AcademyJson.encodeToString(tokens).encodeToByteArray().toNSData()
        val attributes = NSMutableDictionary().apply {
            setSecurityValue(kSecValueData, data)
            setSecurityValue(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }
        val updateStatus = SecItemUpdate(baseQuery().asDictionary(), attributes.asDictionary())
        if (updateStatus == errSecSuccess) return
        if (updateStatus != errSecItemNotFound) {
            throw KeychainException(updateStatus, "Unable to update the Keychain session.")
        }

        val item = baseQuery().apply {
            setSecurityValue(kSecValueData, data)
            setSecurityValue(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }
        val addStatus = SecItemAdd(item.asDictionary(), null)
        if (addStatus != errSecSuccess) {
            throw KeychainException(addStatus, "Unable to save the Keychain session.")
        }
    }

    override suspend fun clear() {
        when (val status = SecItemDelete(baseQuery().asDictionary())) {
            errSecSuccess, errSecItemNotFound -> Unit
            else -> throw KeychainException(status, "Unable to clear the Keychain session.")
        }
    }

    private fun baseQuery(): NSMutableDictionary = NSMutableDictionary().apply {
        setSecurityValue(kSecClass, kSecClassGenericPassword)
        setSecurityValue(kSecAttrService, service)
        setSecurityValue(kSecAttrAccount, account)
    }

    private fun NSMutableDictionary.setSecurityValue(
        key: CPointer<*>?,
        value: Any?,
    ) {
        val keyObject = requireNotNull(key).toObjCObject<NSString>()
        val bridgedValue = if (value is CPointer<*>) value.toObjCObject<Any>() else value
        setObject(bridgedValue, forKey = keyObject)
    }

    private fun NSMutableDictionary.asDictionary(): CFDictionaryRef =
        requireNotNull(interpretCPointer(objcPtr()))

    private inline fun <reified T : Any> CPointer<*>.toObjCObject(): T =
        interpretObjCPointer(rawValue)

    private fun ByteArray.toNSData(): NSData = usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray =
        bytes?.reinterpret<ByteVar>()?.readBytes(length.toInt()) ?: ByteArray(0)
}

class KeychainException(
    val status: Int,
    message: String,
) : IllegalStateException("$message (status $status)")
