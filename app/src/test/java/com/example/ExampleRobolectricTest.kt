package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ApprovalStatus
import com.example.data.model.CommunityChannel
import com.example.data.model.SavRole
import com.example.ui.screens.allowedNoticeCategoriesFor
import com.example.ui.screens.canCreateEvents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches SAV FAMILY`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SAV FAMILY", appName)
    }

    @Test
    fun `all eight SAV roles are defined and accessible channels are scoped`() {
        assertEquals(8, SavRole.entries.size)
        val studentChannels = CommunityChannel.accessibleFor(SavRole.STUDENT)
        assertTrue(CommunityChannel.ALL_COMMUNITY in studentChannels)
        assertTrue(CommunityChannel.STUDENT_CAMPUS in studentChannels)
        assertFalse(CommunityChannel.FACULTY_STAFF in studentChannels)
        assertFalse(CommunityChannel.ALUMNI_CIRCLE in studentChannels)

        val principalChannels = CommunityChannel.accessibleFor(SavRole.PRINCIPAL)
        assertEquals(CommunityChannel.entries.size, principalChannels.size)
    }

    @Test
    fun `role permissions match notice and event rules`() {
        assertFalse(canCreateEvents(SavRole.STUDENT))
        assertFalse(canCreateEvents(SavRole.PARENT))
        assertTrue(canCreateEvents(SavRole.PRINCIPAL))
        assertTrue(canCreateEvents(SavRole.ALUMNI))

        val studentNoticeCats = allowedNoticeCategoriesFor(SavRole.STUDENT)
        assertEquals(1, studentNoticeCats.size)
        assertEquals(ApprovalStatus.PENDING, ApprovalStatus.fromRaw("UNKNOWN_VALUE"))
    }
}
