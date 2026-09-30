package com.example.taskapi.security

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Хэширование и проверка паролей через BCrypt.
 *
 * Важно: при входе используется [verify], а не сравнение строк.
 * BCrypt сам закладывает соль в хэш, поэтому два вызова [hash] для одного
 * и того же пароля дают разные строки — прямое сравнение `==` здесь бесполезно.
 */
object PasswordHasher {

    private const val COST = 12

    fun hash(rawPassword: String): String =
        BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray())

    fun verify(rawPassword: String, passwordHash: String): Boolean =
        BCrypt.verifyer().verify(rawPassword.toCharArray(), passwordHash).verified
}
