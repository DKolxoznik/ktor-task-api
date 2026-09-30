package com.example.taskapi.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Явная проверка: пароль хэшируется BCrypt и сверяется через verify,
 * а не сравнением строк (критерий КТ-2 «хранение паролей»).
 */
class PasswordHasherTest {

    @Test
    fun `hash differs from plaintext and from another hash of the same password`() {
        val hash1 = PasswordHasher.hash("secret1")
        val hash2 = PasswordHasher.hash("secret1")

        assertNotEquals("secret1", hash1)
        assertTrue(hash1.startsWith("\$2a\$") || hash1.startsWith("\$2b\$"))
        // BCrypt соль случайная — два хэша одного пароля не совпадают
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verify accepts correct password and rejects wrong one`() {
        val hash = PasswordHasher.hash("secret1")

        assertTrue(PasswordHasher.verify("secret1", hash))
        assertFalse(PasswordHasher.verify("wrong!!", hash))
        // Прямое сравнение строк с хэшем не сработало бы:
        assertFalse("secret1" == hash)
    }
}
