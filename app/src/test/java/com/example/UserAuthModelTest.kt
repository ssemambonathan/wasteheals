package com.example

import com.example.domain.model.User
import com.example.domain.model.UserAccount
import com.example.domain.model.UserRole
import com.example.domain.model.UserRoleKey
import com.example.ui.auth.normalizeUgandaE164
import org.junit.Assert.assertEquals
import org.junit.Test

class UserAuthModelTest {

  @Test
  fun `canonical Firestore User roles support picker, collector, admin, and health_provider`() {
    assertEquals(
      listOf("picker", "collector", "admin", "health_provider"),
      UserRoleKey.entries.map { it.key }
    )
    assertEquals(UserRoleKey.PICKER, UserRoleKey.fromKey("picker"))
    assertEquals(UserRoleKey.COLLECTOR, UserRoleKey.fromKey("collector"))
    assertEquals(UserRoleKey.ADMIN, UserRoleKey.fromKey("admin"))
    assertEquals(UserRoleKey.HEALTH_PROVIDER, UserRoleKey.fromKey("health_provider"))

    assertEquals(UserRole.SUPER_ADMIN, UserRole.fromString("admin"))
    assertEquals(UserRole.COLLECTION_AGENT, UserRole.fromString("collector"))
    assertEquals(UserRole.HEALTHCARE_MANAGER, UserRole.fromString("health_provider"))
    assertEquals(UserRole.PICKER, UserRole.fromString("picker"))
  }

  @Test
  fun `User data model defaults to picker role safely`() {
    val user = User(
      userId = "uid_kampala_1",
      displayName = "Amina Nakato",
      authProvider = "phone"
    )
    val account: UserAccount = user
    assertEquals("picker", account.role)
    assertEquals(listOf("picker"), account.roles)
    assertEquals("phone", account.authProvider)
  }

  @Test
  fun `normalizeUgandaE164 formats local and international Uganda numbers`() {
    assertEquals("+256771234567", normalizeUgandaE164("0771 234-567"))
    assertEquals("+256701234567", normalizeUgandaE164("256701234567"))
    assertEquals("+256782112233", normalizeUgandaE164("+256782112233"))
  }
}
