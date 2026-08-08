package com.example

import com.example.data.model.UserEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleHierarchyTest {

    @Test
    fun testSuperAdminPermissions() {
        val user = UserEntity(id = "101", role = "Super Admin")
        assertTrue(user.isSuperAdmin())
        assertTrue(user.isAdmin())
        assertTrue(user.hasNoticePermission())
        assertTrue(user.hasComplaintPermission())
        assertTrue(user.hasMemberPermission())
    }

    @Test
    fun testAdminPermissions() {
        val user = UserEntity(id = "102", role = "Admin", canManageNotices = true, canManageComplaints = true)
        assertFalse(user.isSuperAdmin())
        assertTrue(user.isAdmin())
        assertTrue(user.hasNoticePermission())
        assertTrue(user.hasComplaintPermission())
    }

    @Test
    fun testEntrepreneurialMemberPermissions() {
        val user = UserEntity(id = "103", role = "Entrepreneurial Member", canManageNotices = true, canManageComplaints = true)
        assertFalse(user.isSuperAdmin())
        assertFalse(user.isAdmin())
        assertTrue(user.isEntrepreneurialMember())
        assertTrue(user.hasNoticePermission())
        assertTrue(user.hasComplaintPermission())
    }

    @Test
    fun testGeneralMemberPermissions() {
        val user = UserEntity(id = "104", role = "General Member")
        assertFalse(user.isSuperAdmin())
        assertFalse(user.isAdmin())
        assertFalse(user.isEntrepreneurialMember())
        assertTrue(user.isGeneralMember())
        assertFalse(user.isNewMember())
    }

    @Test
    fun testNewMemberPermissions() {
        val user = UserEntity(id = "105", role = "New Member")
        assertFalse(user.isSuperAdmin())
        assertFalse(user.isAdmin())
        assertFalse(user.isEntrepreneurialMember())
        assertFalse(user.isGeneralMember())
        assertTrue(user.isNewMember())
    }
}
