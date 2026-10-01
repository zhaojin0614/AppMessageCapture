package com.aifactory.appmessagecapture.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 功能开关守卫逻辑的纯函数测试：
 * Tab 级模块至少保留一个开启，子功能模块无此限制。
 */
class FeatureRepositoryTest {

    @Test
    fun `关闭一个主页Tab后其余Tab仍然开启时允许`() {
        val next = FeatureRepository.computeNextDisabled(
            current = emptySet(), module = FeatureModule.MESSAGES, enable = false
        )
        assertEquals(setOf(FeatureModule.MESSAGES), next)
    }

    @Test
    fun `关闭最后一个开启的主页Tab被拒绝`() {
        // 消息与记账已关闭，生日是最后一个 Tab → 拒绝
        val current = setOf(FeatureModule.MESSAGES, FeatureModule.BILLS)
        val next = FeatureRepository.computeNextDisabled(
            current = current, module = FeatureModule.BIRTHDAY, enable = false
        )
        assertNull(next)
    }

    @Test
    fun `重新开启已关闭的Tab不受守卫限制`() {
        val current = setOf(FeatureModule.MESSAGES, FeatureModule.BILLS)
        val next = FeatureRepository.computeNextDisabled(
            current = current, module = FeatureModule.MESSAGES, enable = true
        )
        assertEquals(setOf(FeatureModule.BILLS), next)
    }

    @Test
    fun `子功能模块可以全部关闭`() {
        FeatureModule.entries.filterNot { it.isTab }.forEach { module ->
            val next = FeatureRepository.computeNextDisabled(
                current = emptySet(), module = module, enable = false
            )
            assertTrue(module.id, next!!.contains(module))
        }
    }

    @Test
    fun `开启子功能模块即从关闭集合移除`() {
        val current = setOf(FeatureModule.REPORT, FeatureModule.BUDGET)
        val next = FeatureRepository.computeNextDisabled(
            current = current, module = FeatureModule.REPORT, enable = true
        )
        assertEquals(setOf(FeatureModule.BUDGET), next)
    }

    @Test
    fun `三个Tab全部开启时禁用任意两个仍保留一个`() {
        val first = FeatureRepository.computeNextDisabled(
            emptySet(), FeatureModule.MESSAGES, enable = false
        )!!
        val second = FeatureRepository.computeNextDisabled(
            first, FeatureModule.BILLS, enable = false
        )!!
        // 再关生日（最后一个）应被拒绝
        assertFalse(
            FeatureRepository.computeNextDisabled(
                second, FeatureModule.BIRTHDAY, enable = false
            ) != null
        )
    }
}
